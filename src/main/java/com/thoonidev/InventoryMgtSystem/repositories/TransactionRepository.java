package com.thoonidev.InventoryMgtSystem.repositories;

import com.thoonidev.InventoryMgtSystem.models.Transaction;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction>{
    List<Transaction> findByOrganizationId(Long organizationId, Sort sort);

    List<Transaction> findByOrganizationIdAndCreatedAtBetween(
            Long organizationId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Sort sort
    );
}
