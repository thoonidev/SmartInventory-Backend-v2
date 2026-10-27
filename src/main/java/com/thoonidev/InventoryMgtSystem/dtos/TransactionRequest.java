package com.thoonidev.InventoryMgtSystem.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest {

    private Long productId;

    private Integer quantity;

    private Long supplierId;

    private Long customerId;

    private String description;

    private String note;

    private String returnReason;

    private String returnCondition;

    private String warehouseLocation;

    @Valid
    private List<TransactionItemRequest> items;
}
