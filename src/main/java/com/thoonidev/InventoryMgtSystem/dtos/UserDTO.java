package com.thoonidev.InventoryMgtSystem.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoonidev.InventoryMgtSystem.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {

    private Long id;

    @Size(max = 120, message = "El nombre no debe superar 120 caracteres")
    private String name;

    @Email(message = "Ingrese un correo valido")
    private String email;

    @JsonIgnore
    private String password;

    @Pattern(regexp = "^(\\+?51)?[\\s-]?9\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{3}$|^$", message = "Ingrese un celular peruano valido")
    private String phoneNumber;

    private UserRole role;
    private Boolean active;
    private LocalDateTime lastLogin;

    private Long organizationId;
    private String organizationName;

    private List<TransactionDTO> transactions;

    private LocalDateTime createdAt;

}
