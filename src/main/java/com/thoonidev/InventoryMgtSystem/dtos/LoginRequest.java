package com.thoonidev.InventoryMgtSystem.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingrese un correo valido")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;
}
