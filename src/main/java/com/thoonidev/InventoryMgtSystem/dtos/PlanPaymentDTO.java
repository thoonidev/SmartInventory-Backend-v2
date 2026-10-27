package com.thoonidev.InventoryMgtSystem.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoonidev.InventoryMgtSystem.enums.OrganizationPlan;
import com.thoonidev.InventoryMgtSystem.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanPaymentDTO {
    private Long id;
    private OrganizationPlan previousPlan;
    private OrganizationPlan targetPlan;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String referenceCode;
    private String paymentProvider;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime effectiveAt;
}
