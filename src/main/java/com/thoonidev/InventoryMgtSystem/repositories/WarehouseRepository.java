package com.thoonidev.InventoryMgtSystem.repositories;

import com.thoonidev.InventoryMgtSystem.models.Warehouse;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    @Query("select w from Warehouse w where (w.active is null or w.active = true) and w.organization.id = :organizationId")
    List<Warehouse> findByOrganizationId(@Param("organizationId") Long organizationId, Sort sort);
}
