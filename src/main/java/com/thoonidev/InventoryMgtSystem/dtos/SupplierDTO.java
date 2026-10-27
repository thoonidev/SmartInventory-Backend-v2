package com.thoonidev.InventoryMgtSystem.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupplierDTO {

    private Long id;
    private Long organizationId;
    private String organizationName;

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 120, message = "El nombre del proveedor no debe superar 120 caracteres")
    private String name;

    @NotBlank(message = "El contacto del proveedor es obligatorio")
    @Size(max = 120, message = "El contacto del proveedor no debe superar 120 caracteres")
    private String contactInfo;

    @Size(max = 180, message = "La direccion no debe superar 180 caracteres")
    private String address;
    @Min(value = 0, message = "El tiempo de entrega no puede ser negativo")
    private Integer leadTimeDays;
    @Pattern(regexp = "ACTIVE|INACTIVE|OBSERVED|", message = "El estado del proveedor no es valido")
    private String supplierStatus;
}
