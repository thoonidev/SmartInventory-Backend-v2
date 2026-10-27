package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.PlanLimitsDTO;
import com.thoonidev.InventoryMgtSystem.enums.OrganizationPlan;
import com.thoonidev.InventoryMgtSystem.exceptions.NameValueRequiredException;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.repositories.ProductRepository;
import com.thoonidev.InventoryMgtSystem.repositories.UserRepository;
import com.thoonidev.InventoryMgtSystem.services.PlanLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanLimitServiceImpl implements PlanLimitService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public PlanLimitsDTO getLimits(Organization organization) {
        OrganizationPlan plan = organization.getPlan() == null ? OrganizationPlan.FREE : organization.getPlan();
        long currentProducts = productRepository.findByOrganizationId(organization.getId(), Sort.by(Sort.Direction.ASC, "id")).size();
        long currentUsers = userRepository.countByOrganizationIdAndActiveTrue(organization.getId());

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

        limits.setCurrentProducts(currentProducts);
        limits.setCurrentUsers(currentUsers);
        return limits;
    }

    @Override
    public void validateCanCreateProduct(Organization organization) {
        PlanLimitsDTO limits = getLimits(organization);
        if (limits.getMaxProducts() != null && limits.getCurrentProducts() >= limits.getMaxProducts()) {
            throw new NameValueRequiredException("Limite de productos alcanzado para el plan " + limits.getPlan() + ".");
        }
    }

    @Override
    public void validateCanCreateUser(Organization organization) {
        PlanLimitsDTO limits = getLimits(organization);
        if (limits.getMaxUsers() != null && limits.getCurrentUsers() >= limits.getMaxUsers()) {
            throw new NameValueRequiredException("Limite de usuarios alcanzado para el plan " + limits.getPlan() + ".");
        }
    }

    @Override
    public void validateReportsEnabled(Organization organization) {
        if (!Boolean.TRUE.equals(getLimits(organization).getReportsEnabled())) {
            throw new NameValueRequiredException("Los reportes exportables estan disponibles desde el plan BASIC.");
        }
    }

    @Override
    public void validateLiveOperationsEnabled(Organization organization) {
        if (!Boolean.TRUE.equals(getLimits(organization).getLiveOperationsEnabled())) {
            throw new NameValueRequiredException("El centro operativo esta disponible en el plan PREMIUM.");
        }
    }

    @Override
    public void validateAuditEnabled(Organization organization) {
        if (!Boolean.TRUE.equals(getLimits(organization).getAuditEnabled())) {
            throw new NameValueRequiredException("La auditoria esta disponible desde el plan BASIC.");
        }
    }
}
