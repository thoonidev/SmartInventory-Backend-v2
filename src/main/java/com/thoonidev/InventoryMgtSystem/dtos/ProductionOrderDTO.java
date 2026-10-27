package com.thoonidev.InventoryMgtSystem.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProductionOrderDTO {
    private Long id;
    @NotBlank(message = "El codigo de la orden es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9._-]{2,30}$", message = "El codigo debe tener entre 2 y 30 caracteres y solo usar letras, numeros, punto, guion o guion bajo")
    private String code;
    private Long productId;
    private String productSku;
    @NotBlank(message = "El nombre operativo del producto es obligatorio")
    @Size(max = 140, message = "El nombre operativo no debe superar 140 caracteres")
    private String productName;
    @Min(value = 1, message = "La cantidad planificada debe ser mayor a cero")
    private Integer plannedQuantity;
    @Min(value = 0, message = "La cantidad terminada no puede ser negativa")
    private Integer completedQuantity;
    private Integer stockAppliedQuantity;
    @Pattern(regexp = "PLANNED|IN_PROGRESS|DONE|PAUSED|CANCELLED", message = "El estado de produccion no es valido")
    private String status;
    private Boolean active;
    private LocalDate startDate;
    private LocalDate dueDate;
    @Size(max = 500, message = "Las notas no deben superar 500 caracteres")
    private String notes;
    private LocalDateTime createdAt;
    private String userName;
}
