package com.thoonidev.InventoryMgtSystem.repositories;

import com.thoonidev.InventoryMgtSystem.models.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>{
    @Query("""
            select p from Product p
            where (p.active is null or p.active = true)
              and (lower(p.name) like lower(concat('%', :name, '%'))
                   or lower(p.description) like lower(concat('%', :description, '%')))
            """)
    List<Product> findByNameContainingOrDescriptionContaining(@Param("name") String name, @Param("description") String description);

    @Query("select p from Product p where (p.active is null or p.active = true) and (p.organization.id = :organizationId or p.organization is null)")
    List<Product> findByOrganizationIdOrOrganizationIsNull(@Param("organizationId") Long organizationId, Sort sort);

    @Query("select p from Product p where (p.active is null or p.active = true) and p.organization.id = :organizationId")
    List<Product> findByOrganizationId(@Param("organizationId") Long organizationId, Sort sort);

    @Query("select p from Product p where (p.active is null or p.active = true) and p.organization.id = :organizationId and p.reorderPoint is not null and p.stockQuantity <= p.reorderPoint")
    List<Product> findLowStockByOrganizationId(@Param("organizationId") Long organizationId, Sort sort);

    @Query("select count(p) > 0 from Product p where (p.active is null or p.active = true) and p.sku = :sku and p.organization.id = :organizationId")
    boolean existsBySkuAndOrganizationId(@Param("sku") String sku, @Param("organizationId") Long organizationId);

    @Query("select count(p) > 0 from Product p where (p.active is null or p.active = true) and p.sku = :sku and p.organization.id = :organizationId and p.id <> :id")
    boolean existsBySkuAndOrganizationIdAndIdNot(@Param("sku") String sku, @Param("organizationId") Long organizationId, @Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);
}
