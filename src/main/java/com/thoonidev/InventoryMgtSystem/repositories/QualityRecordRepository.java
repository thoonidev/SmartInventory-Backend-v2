package com.thoonidev.InventoryMgtSystem.repositories;

import com.thoonidev.InventoryMgtSystem.models.QualityRecord;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QualityRecordRepository extends JpaRepository<QualityRecord, Long> {
    @Query("select q from QualityRecord q where (q.active is null or q.active = true) and q.organization.id = :organizationId")
    List<QualityRecord> findByOrganizationId(@Param("organizationId") Long organizationId, Sort sort);
}
