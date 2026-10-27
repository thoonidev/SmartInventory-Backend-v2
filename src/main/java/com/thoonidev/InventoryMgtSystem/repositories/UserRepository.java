package com.thoonidev.InventoryMgtSystem.repositories;

import com.thoonidev.InventoryMgtSystem.models.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);

    List<User> findByOrganizationId(Long organizationId, Sort sort);

    long countByOrganizationIdAndActiveTrue(Long organizationId);
}
