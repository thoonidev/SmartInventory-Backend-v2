package com.thoonidev.InventoryMgtSystem.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String action;
    private String entity;
    private Long entityId;
    private String detail;
    private String ipAddress;
    private Long organizationId;
    private String organizationName;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
}
