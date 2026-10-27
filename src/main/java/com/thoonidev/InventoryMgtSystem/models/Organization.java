package com.thoonidev.InventoryMgtSystem.models;

import com.thoonidev.InventoryMgtSystem.enums.OrganizationPlan;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "organizations")
@Data
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @Column(unique = true)
    @NotBlank(message = "RUC is required")
    private String ruc;

    @Enumerated(EnumType.STRING)
    private OrganizationPlan plan;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "created_at")
    private final LocalDateTime createdAt = LocalDateTime.now();

    @JsonIgnore
    @OneToMany(mappedBy = "organization")
    private List<User> users;

    @Override
    public String toString() {
        return "Organization{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", ruc='" + ruc + '\'' +
                ", plan=" + plan +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}
