package com.thoonidev.InventoryMgtSystem.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoonidev.InventoryMgtSystem.enums.OrganizationPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanLimitsDTO {
    private OrganizationPlan plan;
    private Integer maxProducts;
    private Integer maxUsers;
    private Boolean reportsEnabled;
    private Boolean liveOperationsEnabled;
    private Boolean auditEnabled;
    private Long currentProducts;
    private Long currentUsers;
}
