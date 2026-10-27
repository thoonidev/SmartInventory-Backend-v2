package com.thoonidev.InventoryMgtSystem.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class CustomerDTO {
    private Long id;
    private Long organizationId;
    private String organizationName;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 120, message = "El nombre del cliente no debe superar 120 caracteres")
    private String name;

    @Pattern(regexp = "^(\\d{8}|\\d{11})?$", message = "El documento debe tener 8 digitos (DNI) u 11 digitos (RUC)")
    private String documentNumber;

    @NotBlank(message = "El contacto del cliente es obligatorio")
    @Size(max = 120, message = "El contacto del cliente no debe superar 120 caracteres")
    private String contactInfo;

    @Size(max = 180, message = "La direccion no debe superar 180 caracteres")
    private String address;
    @Pattern(regexp = "ACTIVE|INACTIVE|OBSERVED|", message = "El estado del cliente no es valido")
    private String customerStatus;
}
