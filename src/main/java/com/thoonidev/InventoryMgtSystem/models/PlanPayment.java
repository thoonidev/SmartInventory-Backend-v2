package com.thoonidev.InventoryMgtSystem.models;

import com.thoonidev.InventoryMgtSystem.enums.OrganizationPlan;
import com.thoonidev.InventoryMgtSystem.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_payments")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlanPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrganizationPlan previousPlan;

    @Enumerated(EnumType.STRING)
    private OrganizationPlan targetPlan;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private BigDecimal amount;
    private String currency;
    private String referenceCode;
    private String paymentProvider;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime effectiveAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
