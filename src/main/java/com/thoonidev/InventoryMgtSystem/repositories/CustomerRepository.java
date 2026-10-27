package com.thoonidev.InventoryMgtSystem.repositories;

import com.thoonidev.InventoryMgtSystem.models.Customer;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Query("select c from Customer c where (c.customerStatus is null or c.customerStatus <> 'INACTIVE') and (c.organization.id = :organizationId or c.organization is null)")
    List<Customer> findByOrganizationIdOrOrganizationIsNull(@Param("organizationId") Long organizationId, Sort sort);

    @Query("select c from Customer c where (c.customerStatus is null or c.customerStatus <> 'INACTIVE') and c.organization.id = :organizationId")
    List<Customer> findByOrganizationId(@Param("organizationId") Long organizationId, Sort sort);
}
