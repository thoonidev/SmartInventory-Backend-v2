package com.thoonidev.InventoryMgtSystem.services;

import com.thoonidev.InventoryMgtSystem.dtos.PlanLimitsDTO;
import com.thoonidev.InventoryMgtSystem.models.Organization;

public interface PlanLimitService {
    PlanLimitsDTO getLimits(Organization organization);

    void validateCanCreateProduct(Organization organization);

    void validateCanCreateUser(Organization organization);

    void validateReportsEnabled(Organization organization);

    void validateLiveOperationsEnabled(Organization organization);

    void validateAuditEnabled(Organization organization);
}
