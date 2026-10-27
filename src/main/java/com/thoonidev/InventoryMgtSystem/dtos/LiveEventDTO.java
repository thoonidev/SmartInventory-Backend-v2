package com.thoonidev.InventoryMgtSystem.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LiveEventDTO {
    private String type;
    private String title;
    private String message;
    private String severity;
    private String entity;
    private Long entityId;
    private Object data;
    private LocalDateTime createdAt;
}
