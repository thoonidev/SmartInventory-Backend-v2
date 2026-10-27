package com.thoonidev.InventoryMgtSystem.repositories;

import com.thoonidev.InventoryMgtSystem.models.AuditLog;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByOrganizationId(Long organizationId, Sort sort);
}
