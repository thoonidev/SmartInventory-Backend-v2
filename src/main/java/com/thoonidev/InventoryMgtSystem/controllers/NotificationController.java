package com.thoonidev.InventoryMgtSystem.controllers;

import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Response> getNotifications() {
        return ResponseEntity.ok(notificationService.getCurrentTenantNotifications());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Response> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Response> markAllAsRead() {
        return ResponseEntity.ok(notificationService.markAllAsRead());
    }
}
