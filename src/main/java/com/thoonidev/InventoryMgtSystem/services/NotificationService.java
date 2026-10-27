package com.thoonidev.InventoryMgtSystem.services;

import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.User;

public interface NotificationService {
    void create(Organization organization, User user, String type, String title, String message, String severity);

    Response getCurrentTenantNotifications();

    Response markAsRead(Long id);

    Response markAllAsRead();
}
