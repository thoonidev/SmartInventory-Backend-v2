package com.thoonidev.InventoryMgtSystem.services;

import com.thoonidev.InventoryMgtSystem.dtos.OrganizationDTO;
import com.thoonidev.InventoryMgtSystem.dtos.RegisterOrganizationRequest;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.models.Organization;

public interface OrganizationService {
    Response registerOrganization(RegisterOrganizationRequest request);

    Response getCurrentOrganization();

    Response updateCurrentOrganization(OrganizationDTO organizationDTO);

    Response getCurrentOrganizationUsers();

    Organization getCurrentUserOrganization();

    Organization getOrCreateDefaultOrganization();
}
