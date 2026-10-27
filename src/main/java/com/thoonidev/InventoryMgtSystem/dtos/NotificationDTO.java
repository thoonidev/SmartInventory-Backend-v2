package com.thoonidev.InventoryMgtSystem.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationDTO {
    private Long id;
    private String type;
    private String title;
    private String message;
    private String severity;
    private Boolean read;
    private Long organizationId;
    private String organizationName;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
}
