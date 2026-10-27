package com.thoonidev.InventoryMgtSystem.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogisticsKpiDTO {
    private Long totalProducts;
    private Long totalSuppliers;
    private Long lowStockProducts;
    private Long criticalStockProducts;
    private Integer totalUnitsInStock;
    private BigDecimal inventoryValue;
    private BigDecimal purchaseAmount;
    private BigDecimal salesAmount;
}
