package com.thoonidev.InventoryMgtSystem.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoonidev.InventoryMgtSystem.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

    //Generic
    private int status;
    private String message;

    //for login
    private String token;
    private UserRole role;
    private String expirationTime;

    //for pagination
    private Integer totalPages;
    private Long totalElements;

    //data output optionals
    private UserDTO user;
    private List<UserDTO> users;

    private OrganizationDTO organization;
    private PlanLimitsDTO planLimits;
    private PlanPaymentDTO planPayment;
    private List<PlanPaymentDTO> planPayments;

    private SupplierDTO supplier;
    private List<SupplierDTO> suppliers;

    private CustomerDTO customer;
    private List<CustomerDTO> customers;

    private CategoryDTO category;
    private List<CategoryDTO> categories;

    private ProductDTO product;
    private List<ProductDTO> products;

    private TransactionDTO transaction;
    private List<TransactionDTO> transactions;

    private LogisticsKpiDTO logisticsKpi;
    private List<ReorderSuggestionDTO> reorderSuggestions;
    private List<AuditLogDTO> auditLogs;
    private List<NotificationDTO> notifications;
    private Long unreadNotifications;
    private WarehouseDTO warehouse;
    private List<WarehouseDTO> warehouses;
    private QualityRecordDTO qualityRecord;
    private List<QualityRecordDTO> qualityRecords;
    private ProductionOrderDTO productionOrder;
    private List<ProductionOrderDTO> productionOrders;

    private Object data;

    private final LocalDateTime timestamp = LocalDateTime.now();
}
