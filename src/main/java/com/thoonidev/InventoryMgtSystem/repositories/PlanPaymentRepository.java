package com.thoonidev.InventoryMgtSystem.repositories;

import com.thoonidev.InventoryMgtSystem.models.PlanPayment;
import com.thoonidev.InventoryMgtSystem.enums.PaymentStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlanPaymentRepository extends JpaRepository<PlanPayment, Long> {
    List<PlanPayment> findByOrganizationId(Long organizationId, Sort sort);

    List<PlanPayment> findByStatusAndEffectiveAtLessThanEqual(PaymentStatus status, LocalDateTime effectiveAt);

    List<PlanPayment> findByOrganizationIdAndStatusIn(Long organizationId, Collection<PaymentStatus> statuses);

    Optional<PlanPayment> findFirstByOrganizationIdAndStatusInOrderByIdDesc(
            Long organizationId,
            Collection<PaymentStatus> statuses
    );
}
