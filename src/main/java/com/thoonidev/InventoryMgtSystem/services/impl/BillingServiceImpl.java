package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.PlanLimitsDTO;
import com.thoonidev.InventoryMgtSystem.dtos.PlanPaymentDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.enums.OrganizationPlan;
import com.thoonidev.InventoryMgtSystem.enums.PaymentStatus;
import com.thoonidev.InventoryMgtSystem.exceptions.NameValueRequiredException;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.PlanPayment;
import com.thoonidev.InventoryMgtSystem.models.User;
import com.thoonidev.InventoryMgtSystem.repositories.OrganizationRepository;
import com.thoonidev.InventoryMgtSystem.repositories.PlanPaymentRepository;
import com.thoonidev.InventoryMgtSystem.services.BillingService;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import com.thoonidev.InventoryMgtSystem.services.PlanLimitService;
import com.thoonidev.InventoryMgtSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final PlanPaymentRepository planPaymentRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;
    private final PlanLimitService planLimitService;
    private final UserService userService;

    @Override
    @Transactional
    public Response createCheckout(OrganizationPlan targetPlan) {
        Organization organization = organizationService.getCurrentUserOrganization();
        User user = userService.getCurrentLoggedInUser();
        OrganizationPlan currentPlan = organization.getPlan() == null ? OrganizationPlan.FREE : organization.getPlan();

        if (targetPlan == null) {
            throw new NameValueRequiredException("Seleccione un plan.");
        }
        if (targetPlan == currentPlan) {
            throw new NameValueRequiredException("La empresa ya esta en ese plan.");
        }
        planPaymentRepository.findFirstByOrganizationIdAndStatusInOrderByIdDesc(
                organization.getId(),
                List.of(PaymentStatus.PENDING, PaymentStatus.SCHEDULED)
        ).ifPresent(activePayment -> {
            throw new NameValueRequiredException(
                    "Ya existe una solicitud de plan pendiente. Cancelala o finalizala antes de crear otra."
            );
        });

        boolean downgrade = targetPlan.ordinal() < currentPlan.ordinal();
        if (downgrade) {
            if (targetPlan != OrganizationPlan.FREE) {
                throw new NameValueRequiredException("Para reducir la suscripcion usa la cancelacion al plan gratuito al finalizar el ciclo.");
            }
        }

        PlanPayment payment = PlanPayment.builder()
                .previousPlan(currentPlan)
                .targetPlan(targetPlan)
                .amount(downgrade ? BigDecimal.ZERO : priceFor(targetPlan))
                .currency("PEN")
                .status(downgrade ? PaymentStatus.SCHEDULED : PaymentStatus.PENDING)
                .referenceCode("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paymentProvider(downgrade ? "SUBSCRIPTION_CANCELLATION" : "SIMULATED_CHECKOUT")
                .organization(organization)
                .user(user)
                .createdAt(LocalDateTime.now())
                .paidAt(null)
                .effectiveAt(downgrade ? LocalDateTime.now().plusDays(30) : null)
                .build();
        PlanPayment saved = planPaymentRepository.save(payment);

        return Response.builder()
                .status(200)
                .message(downgrade ? "Suscripcion cancelada. Tu plan seguira activo hasta el final del ciclo." : "Checkout de pago generado.")
                .planPayment(toDto(saved))
                .planLimits(planLimitService.getLimits(organization))
                .build();
    }

    @Override
    @Transactional
    public Response confirmPayment(Long paymentId) {
        Organization organization = organizationService.getCurrentUserOrganization();
        PlanPayment payment = planPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Pago no encontrado"));
        if (payment.getOrganization() == null || !organization.getId().equals(payment.getOrganization().getId())) {
            throw new NotFoundException("Pago no encontrado");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            return Response.builder()
                    .status(200)
                    .message("El pago ya fue confirmado.")
                    .planPayment(toDto(payment))
                    .planLimits(planLimitService.getLimits(organization))
                    .build();
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new NameValueRequiredException("El pago no se puede confirmar en su estado actual.");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        planPaymentRepository.save(payment);
        activatePlan(organization, payment.getTargetPlan());

        return Response.builder()
                .status(200)
                .message("Pago aprobado. Plan activado correctamente.")
                .planPayment(toDto(payment))
                .planLimits(planLimitService.getLimits(organization))
                .build();
    }

    @Override
    @Transactional
    public Response cancelPlanChange(Long paymentId) {
        Organization organization = organizationService.getCurrentUserOrganization();
        PlanPayment payment = planPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Solicitud de plan no encontrada"));
        if (payment.getOrganization() == null || !organization.getId().equals(payment.getOrganization().getId())) {
            throw new NotFoundException("Solicitud de plan no encontrada");
        }
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.SCHEDULED) {
            throw new NameValueRequiredException("Esta solicitud de plan ya no se puede cancelar.");
        }

        List<PlanPayment> activePayments = planPaymentRepository.findByOrganizationIdAndStatusIn(
                organization.getId(),
                List.of(PaymentStatus.PENDING, PaymentStatus.SCHEDULED)
        );
        activePayments.forEach(activePayment -> activePayment.setStatus(PaymentStatus.CANCELLED));
        planPaymentRepository.saveAll(activePayments);

        return Response.builder()
                .status(200)
                .message("Solicitud de plan cancelada. Tu plan actual no cambio.")
                .planPayment(toDto(payment))
                .planLimits(planLimitService.getLimits(organization))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanPaymentDTO> getPaymentHistory() {
        Organization organization = organizationService.getCurrentUserOrganization();
        return planPaymentRepository.findByOrganizationId(organization.getId(), Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void applyScheduledPlanChanges() {
        List<PlanPayment> dueChanges = planPaymentRepository.findByStatusAndEffectiveAtLessThanEqual(
                PaymentStatus.SCHEDULED,
                LocalDateTime.now()
        );
        for (PlanPayment payment : dueChanges) {
            Organization organization = payment.getOrganization();
            if (organization == null || payment.getTargetPlan() == null) {
                payment.setStatus(PaymentStatus.CANCELLED);
                planPaymentRepository.save(payment);
                continue;
            }
            if (payment.getTargetPlan() != OrganizationPlan.FREE && !canApplyTargetPlan(payment.getTargetPlan(), organization)) {
                payment.setStatus(PaymentStatus.CANCELLED);
                planPaymentRepository.save(payment);
                continue;
            }
            activatePlan(organization, payment.getTargetPlan());
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            planPaymentRepository.save(payment);
        }
    }

    private boolean canApplyTargetPlan(OrganizationPlan targetPlan, Organization organization) {
        PlanLimitsDTO currentLimits = planLimitService.getLimits(organization);
        PlanLimitsDTO targetLimits = limitsFor(targetPlan, currentLimits.getCurrentProducts(), currentLimits.getCurrentUsers());
        if (targetLimits.getMaxProducts() != null && currentLimits.getCurrentProducts() > targetLimits.getMaxProducts()) {
            return false;
        }
        if (targetLimits.getMaxUsers() != null && currentLimits.getCurrentUsers() > targetLimits.getMaxUsers()) {
            return false;
        }
        return true;
    }

    private PlanLimitsDTO limitsFor(OrganizationPlan plan, Long products, Long users) {
        PlanLimitsDTO limits = switch (plan) {
            case BASIC -> PlanLimitsDTO.builder()
                    .plan(plan)
                    .maxProducts(200)
                    .maxUsers(5)
                    .reportsEnabled(true)
                    .liveOperationsEnabled(false)
                    .auditEnabled(true)
                    .build();
            case PREMIUM -> PlanLimitsDTO.builder()
                    .plan(plan)
                    .maxProducts(null)
                    .maxUsers(null)
                    .reportsEnabled(true)
                    .liveOperationsEnabled(true)
                    .auditEnabled(true)
                    .build();
            default -> PlanLimitsDTO.builder()
                    .plan(OrganizationPlan.FREE)
                    .maxProducts(20)
                    .maxUsers(2)
                    .reportsEnabled(false)
                    .liveOperationsEnabled(false)
                    .auditEnabled(false)
                    .build();
        };
        limits.setCurrentProducts(products);
        limits.setCurrentUsers(users);
        return limits;
    }

    private BigDecimal priceFor(OrganizationPlan plan) {
        return switch (plan) {
            case BASIC -> BigDecimal.valueOf(49);
            case PREMIUM -> BigDecimal.valueOf(99);
            default -> BigDecimal.ZERO;
        };
    }

    private void activatePlan(Organization organization, OrganizationPlan targetPlan) {
        organization.setPlan(targetPlan);
        organizationRepository.save(organization);
    }

    private PlanPaymentDTO toDto(PlanPayment payment) {
        PlanPaymentDTO dto = new PlanPaymentDTO();
        dto.setId(payment.getId());
        dto.setPreviousPlan(payment.getPreviousPlan());
        dto.setTargetPlan(payment.getTargetPlan());
        dto.setStatus(payment.getStatus());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setReferenceCode(payment.getReferenceCode());
        dto.setPaymentProvider(payment.getPaymentProvider());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setPaidAt(payment.getPaidAt());
        dto.setEffectiveAt(payment.getEffectiveAt());
        return dto;
    }
}
