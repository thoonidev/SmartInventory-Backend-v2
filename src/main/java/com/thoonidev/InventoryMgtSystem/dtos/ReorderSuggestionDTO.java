package com.thoonidev.InventoryMgtSystem.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReorderSuggestionDTO {
    private Long productId;
    private String productName;
    private String sku;
    private Integer stockQuantity;
    private Integer reorderPoint;
    private Integer suggestedQuantity;
    private String warehouseLocation;
}
