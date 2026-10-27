package com.thoonidev.InventoryMgtSystem.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterOrganizationRequest {

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 140, message = "El nombre de la empresa no debe superar 140 caracteres")
    private String organizationName;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "^(10|15|17|20)\\d{9}$", message = "El RUC debe tener 11 digitos y empezar con 10, 15, 17 o 20")
    private String ruc;

    @NotBlank(message = "El nombre del responsable es obligatorio")
    @Size(max = 120, message = "El nombre del responsable no debe superar 120 caracteres")
    private String adminName;

    @NotBlank(message = "El correo del responsable es obligatorio")
    @Email(message = "Ingrese un correo valido")
    private String adminEmail;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
    private String adminPassword;

    @NotBlank(message = "El celular del responsable es obligatorio")
    @Pattern(regexp = "^(\\+?51)?[\\s-]?9\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{3}$", message = "Ingrese un celular peruano valido")
    private String adminPhoneNumber;
}
