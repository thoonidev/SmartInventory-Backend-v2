package com.thoonidev.InventoryMgtSystem.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryDTO {

    private Long id;
    private Long organizationId;
    private String organizationName;

    @NotBlank(message = "El nombre de la categoria es obligatorio")
    @Size(max = 80, message = "El nombre de la categoria no debe superar 80 caracteres")
    private String name;
    private Boolean active;

    private List<ProductDTO> products;

}
