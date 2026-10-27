package com.thoonidev.InventoryMgtSystem.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarehouseDTO {
    private Long id;
    @NotBlank(message = "El nombre del almacen o sede es obligatorio")
    @Size(max = 120, message = "El nombre no debe superar 120 caracteres")
    private String name;
    @NotBlank(message = "El codigo del almacen o sede es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9._-]{2,20}$", message = "El codigo debe tener entre 2 y 20 caracteres y solo usar letras, numeros, punto, guion o guion bajo")
    private String code;
    @Pattern(regexp = "MAIN|WAREHOUSE|STORE|QUALITY", message = "El tipo de ubicacion no es valido")
    private String type;
    @Size(max = 180, message = "La direccion no debe superar 180 caracteres")
    private String address;
    @Size(max = 120, message = "El responsable no debe superar 120 caracteres")
    private String managerName;
    private Boolean active;
    private LocalDateTime createdAt;
}
