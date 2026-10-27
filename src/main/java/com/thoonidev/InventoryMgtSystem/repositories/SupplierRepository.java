package com.thoonidev.InventoryMgtSystem.repositories;

import com.thoonidev.InventoryMgtSystem.models.Supplier;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    @Query("select s from Supplier s where (s.supplierStatus is null or s.supplierStatus <> 'INACTIVE') and (s.organization.id = :organizationId or s.organization is null)")
    List<Supplier> findByOrganizationIdOrOrganizationIsNull(@Param("organizationId") Long organizationId, Sort sort);

    @Query("select s from Supplier s where (s.supplierStatus is null or s.supplierStatus <> 'INACTIVE') and s.organization.id = :organizationId")
    List<Supplier> findByOrganizationId(@Param("organizationId") Long organizationId, Sort sort);
}
