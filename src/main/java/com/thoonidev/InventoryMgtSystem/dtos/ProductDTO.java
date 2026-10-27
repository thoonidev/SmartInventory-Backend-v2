package com.thoonidev.InventoryMgtSystem.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDTO {

    private Long id;

    private Long categoryId;
    private Long productId;
    private Long supplierId;
    private Long organizationId;
    private String organizationName;

    @NotBlank(message = "El nombre del producto es obligatorio")
    private String name;

    @NotBlank(message = "El SKU es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9._-]{2,30}$", message = "El SKU debe tener entre 2 y 30 caracteres y solo usar letras, numeros, punto, guion o guion bajo")
    private String sku;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
    private BigDecimal price;

    @NotNull(message = "La cantidad en stock es obligatoria")
    @Min(value = 0, message = "La cantidad en stock no puede ser negativa")
    private Integer stockQuantity;

    private String description;
    private LocalDateTime expiryDate;
    private String imageUrl;
    private String imagePublicId;
    private String imageContentType;
    private Long imageSize;
    private String abcClass;
    @Min(value = 0, message = "El punto de reorden no puede ser negativo")
    private Integer reorderPoint;
    @Min(value = 0, message = "La cantidad sugerida no puede ser negativa")
    private Integer reorderQuantity;
    @NotBlank(message = "El almacen o sede es obligatorio")
    private String warehouseLocation;
    private BigDecimal inventoryValue;
    private Boolean active;

    private LocalDateTime createdAt;

}
