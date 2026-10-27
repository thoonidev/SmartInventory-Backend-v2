package com.thoonidev.InventoryMgtSystem.repositories;

import com.thoonidev.InventoryMgtSystem.models.Category;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("select c from Category c where (c.active is null or c.active = true) and (c.organization.id = :organizationId or c.organization is null)")
    List<Category> findByOrganizationIdOrOrganizationIsNull(@Param("organizationId") Long organizationId, Sort sort);
}
