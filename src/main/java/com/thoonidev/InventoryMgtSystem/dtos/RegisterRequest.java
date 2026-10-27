package com.thoonidev.InventoryMgtSystem.dtos;

import com.thoonidev.InventoryMgtSystem.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no debe superar 120 caracteres")
    private String name;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingrese un correo valido")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
    private String password;

    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = "^(\\+?51)?[\\s-]?9\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{3}$", message = "Ingrese un celular peruano valido")
    private String phoneNumber;

    private Long organizationId;
    private UserRole role;
}
