package com.thoonidev.InventoryMgtSystem.repositories;

import com.thoonidev.InventoryMgtSystem.models.ProductionOrder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {
    @Query("select p from ProductionOrder p where (p.active is null or p.active = true) and p.organization.id = :organizationId")
    List<ProductionOrder> findByOrganizationId(@Param("organizationId") Long organizationId, Sort sort);
}
