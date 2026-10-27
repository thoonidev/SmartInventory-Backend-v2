package com.thoonidev.InventoryMgtSystem.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoonidev.InventoryMgtSystem.enums.TransactionStatus;
import com.thoonidev.InventoryMgtSystem.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionDTO {

    private Long id;

    private Integer totalProducts;

    private BigDecimal totalPrice;

    private TransactionType transactionType; // purchase, sale, return

    private TransactionStatus status; //pending, completed, processing
    private Long organizationId;
    private String organizationName;

    private String description;
    private String note;
    private String returnReason;
    private String returnCondition;
    private String warehouseLocation;
    private Boolean stockApplied;

    private LocalDateTime createdAt;
    private LocalDateTime updateAt;

    private ProductDTO product;

    private UserDTO user;

    private SupplierDTO supplier;

    private CustomerDTO customer;

    private List<TransactionItemDTO> items;
}
