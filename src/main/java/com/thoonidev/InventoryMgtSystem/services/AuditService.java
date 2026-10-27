package com.thoonidev.InventoryMgtSystem.services;

import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.User;

public interface AuditService {
    void log(String action, String entity, Long entityId, String detail, Organization organization, User user);

    Response getAuditLogs();
}
