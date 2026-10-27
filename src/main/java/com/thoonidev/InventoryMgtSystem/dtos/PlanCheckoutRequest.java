package com.thoonidev.InventoryMgtSystem.dtos;

import com.thoonidev.InventoryMgtSystem.enums.OrganizationPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlanCheckoutRequest {
    @NotNull(message = "Seleccione un plan")
    private OrganizationPlan targetPlan;
}
