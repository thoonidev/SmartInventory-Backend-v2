package com.thoonidev.InventoryMgtSystem.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class QualityRecordDTO {
    private Long id;
    @NotBlank(message = "El codigo del registro de calidad es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9._-]{2,30}$", message = "El codigo debe tener entre 2 y 30 caracteres y solo usar letras, numeros, punto, guion o guion bajo")
    private String code;
    @NotBlank(message = "El area o proceso es obligatorio")
    @Size(max = 120, message = "El area o proceso no debe superar 120 caracteres")
    private String processArea;
    @Size(max = 120, message = "El documento de referencia no debe superar 120 caracteres")
    private String referenceDocument;
    @Pattern(regexp = "INSPECTION|NON_CONFORMITY|CORRECTIVE_ACTION|DOCUMENT_REVIEW", message = "El tipo de hallazgo no es valido")
    private String findingType;
    @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL", message = "La severidad no es valida")
    private String severity;
    @Pattern(regexp = "OPEN|IN_PROGRESS|CLOSED", message = "El estado de calidad no es valido")
    private String status;
    @Size(max = 700, message = "La descripcion no debe superar 700 caracteres")
    private String description;
    @Size(max = 700, message = "La accion correctiva no debe superar 700 caracteres")
    private String correctiveAction;
    @NotBlank(message = "El responsable del registro de calidad es obligatorio")
    @Size(max = 120, message = "El responsable no debe superar 120 caracteres")
    private String responsibleName;
    @Pattern(regexp = "RETURN_FROM_CUSTOMER|PRODUCTION|PURCHASE|SALE|MANUAL|", message = "El origen del registro no es valido")
    private String sourceType;
    @Min(value = 1, message = "El identificador de origen debe ser positivo")
    private Long sourceId;
    @Size(max = 140, message = "El nombre del producto no debe superar 140 caracteres")
    private String productName;
    private Boolean active;
    private LocalDate dueDate;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private String userName;
}
