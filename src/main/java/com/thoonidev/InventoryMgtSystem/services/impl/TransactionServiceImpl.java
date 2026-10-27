package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.dtos.LiveEventDTO;
import com.thoonidev.InventoryMgtSystem.dtos.TransactionDTO;
import com.thoonidev.InventoryMgtSystem.dtos.TransactionItemRequest;
import com.thoonidev.InventoryMgtSystem.dtos.TransactionRequest;
import com.thoonidev.InventoryMgtSystem.enums.TransactionStatus;
import com.thoonidev.InventoryMgtSystem.enums.TransactionType;
import com.thoonidev.InventoryMgtSystem.exceptions.NameValueRequiredException;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.Product;
import com.thoonidev.InventoryMgtSystem.models.QualityRecord;
import com.thoonidev.InventoryMgtSystem.models.Supplier;
import com.thoonidev.InventoryMgtSystem.models.Customer;
import com.thoonidev.InventoryMgtSystem.models.Transaction;
import com.thoonidev.InventoryMgtSystem.models.TransactionItem;
import com.thoonidev.InventoryMgtSystem.models.User;
import com.thoonidev.InventoryMgtSystem.repositories.CustomerRepository;
import com.thoonidev.InventoryMgtSystem.repositories.ProductRepository;
import com.thoonidev.InventoryMgtSystem.repositories.QualityRecordRepository;
import com.thoonidev.InventoryMgtSystem.repositories.SupplierRepository;
import com.thoonidev.InventoryMgtSystem.repositories.TransactionRepository;
import com.thoonidev.InventoryMgtSystem.services.AuditService;
import com.thoonidev.InventoryMgtSystem.services.LiveEventService;
import com.thoonidev.InventoryMgtSystem.services.NotificationService;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import com.thoonidev.InventoryMgtSystem.services.TransactionService;
import com.thoonidev.InventoryMgtSystem.services.UserService;
import com.thoonidev.InventoryMgtSystem.specification.TransactionFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final QualityRecordRepository qualityRecordRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final AuditService auditService;
    private final LiveEventService liveEventService;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;
    private final MessageSource messageSource;

    @Override
    @Transactional
    public Response purchase(TransactionRequest transactionRequest) {
        Transaction savedTransaction = registerDocument(transactionRequest, TransactionType.PURCHASE, TransactionStatus.PENDING);
        User user = userService.getCurrentLoggedInUser();
        Organization organization = organizationService.getCurrentUserOrganization();
        auditService.log("PURCHASE", "Transaction", savedTransaction.getId(), buildAuditDetail(savedTransaction), organization, user);
        notifyApprovalRequired(organization, user, savedTransaction, "Compra pendiente de aprobacion");
        publishTransactionEvent(savedTransaction, organization.getId(), "Compra pendiente", "La compra fue registrada y espera aprobacion para ingresar stock.", "WARNING");

        return Response.builder()
                .status(200)
                .message(getMessage("transaction.purchase.success"))
                .build();
    }

    @Override
    @Transactional
    public Response sell(TransactionRequest transactionRequest) {
        Transaction savedTransaction = registerDocument(transactionRequest, TransactionType.SALE, TransactionStatus.COMPLETED);
        User user = userService.getCurrentLoggedInUser();
        Organization organization = organizationService.getCurrentUserOrganization();
        auditService.log("SALE", "Transaction", savedTransaction.getId(), buildAuditDetail(savedTransaction), organization, user);
        publishTransactionEvent(savedTransaction, organization.getId(), "Venta registrada", "Salida de inventario registrada en tiempo real.", "SUCCESS");

        return Response.builder()
                .status(200)
                .message(getMessage("transaction.sale.success"))
                .build();
    }

    @Override
    @Transactional
    public Response returnToSupplier(TransactionRequest transactionRequest) {
        Transaction savedTransaction = registerDocument(transactionRequest, TransactionType.RETURN_TO_SUPPLIER, TransactionStatus.PROCESSING);
        User user = userService.getCurrentLoggedInUser();
        Organization organization = organizationService.getCurrentUserOrganization();
        auditService.log("RETURN", "Transaction", savedTransaction.getId(), buildAuditDetail(savedTransaction), organization, user);
        publishTransactionEvent(savedTransaction, organization.getId(), "Devolucion registrada", "Salida hacia proveedor registrada en tiempo real.", "WARNING");

        return Response.builder()
                .status(200)
                .message(getMessage("transaction.return.success"))
                .build();
    }

    @Override
    @Transactional
    public Response returnFromCustomer(TransactionRequest transactionRequest) {
        Transaction savedTransaction = registerDocument(transactionRequest, TransactionType.RETURN_FROM_CUSTOMER, TransactionStatus.PENDING);
        User user = userService.getCurrentLoggedInUser();
        Organization organization = organizationService.getCurrentUserOrganization();
        auditService.log("CUSTOMER_RETURN", "Transaction", savedTransaction.getId(), buildAuditDetail(savedTransaction), organization, user);
        createQualityRecordForObservedReturn(savedTransaction, organization, user);
        notifyApprovalRequired(organization, user, savedTransaction, "Devolucion de cliente pendiente de revision");
        publishTransactionEvent(savedTransaction, organization.getId(), "Devolucion de cliente pendiente", "La devolucion espera aprobacion para decidir si vuelve a stock.", "WARNING");

        return Response.builder()
                .status(200)
                .message(getMessage("transaction.customer_return.success"))
                .build();
    }

    private Transaction registerDocument(TransactionRequest transactionRequest, TransactionType type, TransactionStatus status) {
        List<TransactionItemRequest> requestedItems = normalizeItems(transactionRequest);
        Long supplierId = transactionRequest.getSupplierId();
        Long customerId = transactionRequest.getCustomerId();

        if ((type == TransactionType.PURCHASE || type == TransactionType.RETURN_TO_SUPPLIER) && supplierId == null) {
            throw new NameValueRequiredException(getMessage("supplier.id.required"));
        }
        if ((type == TransactionType.SALE || type == TransactionType.RETURN_FROM_CUSTOMER) && customerId == null) {
            throw new NameValueRequiredException(getMessage("customer.id.required"));
        }

        User user = userService.getCurrentLoggedInUser();
        Organization organization = organizationService.getCurrentUserOrganization();
        Supplier supplier = null;
        Customer customer = null;

        if (supplierId != null) {
            supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new NotFoundException(getMessage("supplier.not_found")));
            validateSupplierAccess(supplier, organization);
        }
        if (customerId != null) {
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new NotFoundException(getMessage("customer.not_found")));
            validateCustomerAccess(customer, organization);
        }

        List<TransactionItem> transactionItems = new ArrayList<>();
        int totalProducts = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;
        Product firstProduct = null;

        for (TransactionItemRequest itemRequest : requestedItems) {
            Product product = productRepository.findByIdForUpdate(itemRequest.getProductId())
                    .orElseThrow(() -> new NotFoundException(getMessage("product.not_found")));
            validateProductAccess(product, organization);
            if (Boolean.FALSE.equals(product.getActive())) {
                throw new NotFoundException(getMessage("product.not_found"));
            }
            if (transactionRequest.getWarehouseLocation() != null && !transactionRequest.getWarehouseLocation().isBlank()) {
                product.setWarehouseLocation(transactionRequest.getWarehouseLocation());
            }

            Integer quantity = itemRequest.getQuantity();
            boolean applyStockNow = shouldApplyStockOnCreate(type);
            if (!applyStockNow) {
                // Stock is applied later by the approval workflow.
            } else if (type == TransactionType.SALE || type == TransactionType.RETURN_TO_SUPPLIER) {
                validateAvailableStock(product, quantity);
                product.setStockQuantity(product.getStockQuantity() - quantity);
            } else if (type == TransactionType.RETURN_FROM_CUSTOMER) {
                if (returnsToAvailableStock(transactionRequest.getReturnCondition())) {
                    product.setStockQuantity(product.getStockQuantity() + quantity);
                }
            } else {
                product.setStockQuantity(product.getStockQuantity() + quantity);
            }
            updateInventoryValue(product);

            if (applyStockNow) {
                productRepository.save(product);
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalProducts += quantity;
            totalPrice = totalPrice.add(subtotal);
            if (firstProduct == null) firstProduct = product;

            transactionItems.add(TransactionItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .build());
        }

        Transaction transaction = Transaction.builder()
                .transactionType(type)
                .status(status)
                .product(firstProduct)
                .user(user)
                .supplier(supplier)
                .customer(customer)
                .organization(organization)
                .totalProducts(totalProducts)
                .totalPrice(type == TransactionType.RETURN_TO_SUPPLIER ? BigDecimal.ZERO : totalPrice)
                .description(transactionRequest.getDescription())
                .note(transactionRequest.getNote())
                .returnReason(transactionRequest.getReturnReason())
                .returnCondition(transactionRequest.getReturnCondition())
                .warehouseLocation(transactionRequest.getWarehouseLocation())
                .stockApplied(shouldApplyStockOnCreate(type))
                .build();

        transactionItems.forEach(item -> {
            item.setTransaction(transaction);
            transaction.getItems().add(item);
        });

        return transactionRepository.save(transaction);
    }

    private void createQualityRecordForObservedReturn(Transaction transaction, Organization organization, User user) {
        if (transaction.getTransactionType() != TransactionType.RETURN_FROM_CUSTOMER ||
                returnsToAvailableStock(transaction.getReturnCondition())) {
            return;
        }

        String products = transaction.getItems().stream()
                .map(item -> item.getProduct().getName())
                .reduce((left, right) -> left + ", " + right)
                .orElse("Producto devuelto");

        QualityRecord record = QualityRecord.builder()
                .code("CAL-DEV-" + String.format("%05d", transaction.getId()))
                .processArea("Devoluciones de cliente")
                .referenceDocument(documentCode(transaction.getId()))
                .findingType("NON_CONFORMITY")
                .severity("HIGH")
                .status("OPEN")
                .description("Devolucion observada: " + products + ". Motivo: " +
                        (transaction.getReturnReason() == null ? "Por revisar" : transaction.getReturnReason()) + ".")
                .correctiveAction("Inspeccionar producto en zona de calidad antes de reincorporar o descartar.")
                .responsibleName(user.getName())
                .sourceType("RETURN_FROM_CUSTOMER")
                .sourceId(transaction.getId())
                .productName(products)
                .organization(organization)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        qualityRecordRepository.save(record);
        auditService.log("CREATE", "QualityRecord", record.getId(), record.getCode(), organization, user);
    }

    private boolean shouldApplyStockOnCreate(TransactionType type) {
        return type == TransactionType.SALE || type == TransactionType.RETURN_TO_SUPPLIER;
    }

    private void notifyApprovalRequired(Organization organization, User user, Transaction transaction, String title) {
        notificationService.create(
                organization,
                user,
                "APPROVAL_REQUIRED",
                title,
                "Documento " + documentCode(transaction.getId()) + " requiere revision.",
                "WARNING"
        );
    }

    private void publishTransactionEvent(Transaction transaction, Long organizationId, String title, String message, String severity) {
        liveEventService.publish(organizationId, LiveEventDTO.builder()
                .type("TRANSACTION_" + transaction.getTransactionType().name())
                .title(title)
                .message(message)
                .severity(severity)
                .entity("Transaction")
                .entityId(transaction.getId())
                .data(Map.of(
                        "transactionType", transaction.getTransactionType().name(),
                        "status", transaction.getStatus().name(),
                        "totalProducts", transaction.getTotalProducts(),
                        "totalPrice", transaction.getTotalPrice(),
                        "customerName", transaction.getCustomer() == null ? "" : transaction.getCustomer().getName(),
                        "supplierName", transaction.getSupplier() == null ? "" : transaction.getSupplier().getName(),
                        "items", transaction.getItems().stream()
                                .map(item -> Map.of(
                                        "productId", item.getProduct().getId(),
                                        "productName", item.getProduct().getName(),
                                        "sku", item.getProduct().getSku(),
                                        "quantity", item.getQuantity(),
                                        "stockQuantity", item.getProduct().getStockQuantity()
                                ))
                                .toList()
                ))
                .build());

        transaction.getItems().stream()
                .map(TransactionItem::getProduct)
                .filter(this::isLowStock)
                .forEach(product -> liveEventService.publish(organizationId, LiveEventDTO.builder()
                        .type("STOCK_ALERT")
                        .title("Stock bajo detectado")
                        .message(product.getName() + " llego al punto de reposicion.")
                        .severity(product.getStockQuantity() <= 0 ? "DANGER" : "WARNING")
                        .entity("Product")
                        .entityId(product.getId())
                        .data(Map.of(
                                "productId", product.getId(),
                                "productName", product.getName(),
                                "sku", product.getSku(),
                                "stockQuantity", product.getStockQuantity(),
                                "reorderPoint", product.getReorderPoint()
                        ))
                        .build()));
    }

    private List<TransactionItemRequest> normalizeItems(TransactionRequest transactionRequest) {
        List<TransactionItemRequest> rawItems = transactionRequest.getItems();
        if ((rawItems == null || rawItems.isEmpty()) && transactionRequest.getProductId() != null && transactionRequest.getQuantity() != null) {
            rawItems = List.of(new TransactionItemRequest(transactionRequest.getProductId(), transactionRequest.getQuantity()));
        }

        if (rawItems == null || rawItems.isEmpty()) {
            throw new NameValueRequiredException("Agregue al menos un producto al documento.");
        }

        Map<Long, Integer> quantitiesByProduct = new LinkedHashMap<>();
        rawItems.forEach(item -> {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new NameValueRequiredException("Cada linea debe tener producto y cantidad mayor a cero.");
            }
            quantitiesByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        });

        return quantitiesByProduct.entrySet().stream()
                .map(entry -> new TransactionItemRequest(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String buildAuditDetail(Transaction transaction) {
        String itemSummary = transaction.getItems() == null ? "" : transaction.getItems().stream()
                .map(item -> item.getProduct().getName() + " x " + item.getQuantity())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return "Items: " + transaction.getTotalProducts() + " | " + itemSummary +
                (transaction.getWarehouseLocation() == null || transaction.getWarehouseLocation().isBlank()
                        ? "" : " | Almacen: " + transaction.getWarehouseLocation());
    }

    @Override
    public Response getAllTransactions(int page, int size, String filter) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Organization organization = organizationService.getCurrentUserOrganization();
        Specification<Transaction> spec = TransactionFilter.byOrganization(organization.getId())
                .and(TransactionFilter.byFilter(filter));

        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);

        List<TransactionDTO> transactionDTOS = transactionPage.getContent().stream()
                .map(transaction -> modelMapper.map(transaction, TransactionDTO.class))
                .toList();

        transactionDTOS.forEach(transactionDTO -> {
            transactionDTO.setUser(null);
            transactionDTO.setProduct(null);
            transactionDTO.setSupplier(null);
        });

        return Response.builder()
                .status(200)
                .message(getMessage("transaction.list.success"))
                .transactions(transactionDTOS)
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .build();
    }

    @Override
    public Response getAllTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(getMessage("transaction.not_found")));
        validateTransactionAccess(transaction);

        TransactionDTO transactionDTO = modelMapper.map(transaction, TransactionDTO.class);
        transactionDTO.getUser().setTransactions(null);

        return Response.builder()
                .status(200)
                .message(getMessage("transaction.detail.success"))
                .transaction(transactionDTO)
                .build();
    }

    @Override
    public Response getAllTransactionByMonthAndYear(int month, int year) {
        Organization organization = organizationService.getCurrentUserOrganization();
        Specification<Transaction> spec = TransactionFilter.byOrganization(organization.getId())
                .and(TransactionFilter.byMonthAndYear(month, year));
        List<Transaction> transactions = transactionRepository.findAll(spec);

        List<TransactionDTO> transactionDTOS = transactions.stream()
                .map(transaction -> modelMapper.map(transaction, TransactionDTO.class))
                .toList();

        transactionDTOS.forEach(transactionDTO -> {
            transactionDTO.setUser(null);
            transactionDTO.setProduct(null);
            transactionDTO.setSupplier(null);
        });

        return Response.builder()
                .status(200)
                .message(getMessage("transaction.list.success"))
                .transactions(transactionDTOS)
                .build();
    }

    @Override
    @Transactional
    public Response updateTransactionStatus(Long transactionId, TransactionStatus status) {
        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException(getMessage("transaction.not_found")));
        validateTransactionAccess(existingTransaction);

        existingTransaction.setStatus(status);
        existingTransaction.setUpdateAt(LocalDateTime.now());
        if (status == TransactionStatus.COMPLETED && !Boolean.TRUE.equals(existingTransaction.getStockApplied())) {
            applyStockForApprovedTransaction(existingTransaction);
            existingTransaction.setStockApplied(true);
        }

        transactionRepository.save(existingTransaction);
        Organization organization = organizationService.getCurrentUserOrganization();
        User user = userService.getCurrentLoggedInUser();
        auditService.log("UPDATE_STATUS", "Transaction", existingTransaction.getId(), status.name(), organization, user);
        notificationService.create(
                organization,
                user,
                "APPROVAL_STATUS",
                "Documento " + status.name().toLowerCase(LocaleContextHolder.getLocale()),
                documentCode(existingTransaction.getId()) + " cambio a " + status.name() + ".",
                status == TransactionStatus.COMPLETED ? "SUCCESS" : status == TransactionStatus.CANCELLED ? "DANGER" : "INFO"
        );
        publishTransactionEvent(existingTransaction, organization.getId(), "Estado actualizado", "Documento actualizado a " + status.name() + ".", "INFO");

        return Response.builder()
                .status(200)
                .message(getMessage("transaction.status.updated"))
                .build();
    }

    private void applyStockForApprovedTransaction(Transaction transaction) {
        for (TransactionItem item : transaction.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new NotFoundException(getMessage("product.not_found")));
            validateProductAccess(product, transaction.getOrganization());
            if (transaction.getWarehouseLocation() != null && !transaction.getWarehouseLocation().isBlank()) {
                product.setWarehouseLocation(transaction.getWarehouseLocation());
            }

            if (transaction.getTransactionType() == TransactionType.PURCHASE) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            } else if (transaction.getTransactionType() == TransactionType.RETURN_FROM_CUSTOMER &&
                    returnsToAvailableStock(transaction.getReturnCondition())) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            }

            updateInventoryValue(product);
            productRepository.save(product);
        }
    }

    private void validateAvailableStock(Product product, Integer quantity) {
        if (product.getStockQuantity() < quantity) {
            throw new NameValueRequiredException(getMessage("product.stock.insufficient"));
        }
    }

    private String documentCode(Long transactionId) {
        return "TRX-" + String.format("%05d", transactionId);
    }

    private boolean isLowStock(Product product) {
        return product.getReorderPoint() != null &&
                product.getStockQuantity() != null &&
                product.getStockQuantity() <= product.getReorderPoint();
    }

    private void updateInventoryValue(Product product) {
        BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        int stock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        product.setInventoryValue(price.multiply(BigDecimal.valueOf(stock)));
    }

    private void validateProductAccess(Product product, Organization organization) {
        if (product.getOrganization() == null) {
            product.setOrganization(organization);
            productRepository.save(product);
            return;
        }
        if (!organization.getId().equals(product.getOrganization().getId())) {
            throw new NotFoundException(getMessage("product.not_found"));
        }
    }

    private void validateSupplierAccess(Supplier supplier, Organization organization) {
        if ("INACTIVE".equalsIgnoreCase(supplier.getSupplierStatus())) {
            throw new NotFoundException(getMessage("supplier.not_found"));
        }
        if (supplier.getOrganization() == null) {
            supplier.setOrganization(organization);
            supplierRepository.save(supplier);
            return;
        }
        if (!organization.getId().equals(supplier.getOrganization().getId())) {
            throw new NotFoundException(getMessage("supplier.not_found"));
        }
    }

    private void validateCustomerAccess(Customer customer, Organization organization) {
        if ("INACTIVE".equalsIgnoreCase(customer.getCustomerStatus())) {
            throw new NotFoundException(getMessage("customer.not_found"));
        }
        if (customer.getOrganization() == null) {
            customer.setOrganization(organization);
            customerRepository.save(customer);
            return;
        }
        if (!organization.getId().equals(customer.getOrganization().getId())) {
            throw new NotFoundException(getMessage("customer.not_found"));
        }
    }

    private boolean returnsToAvailableStock(String returnCondition) {
        return returnCondition == null ||
                returnCondition.isBlank() ||
                "SELLABLE".equalsIgnoreCase(returnCondition) ||
                "BUEN_ESTADO".equalsIgnoreCase(returnCondition);
    }

    private void validateTransactionAccess(Transaction transaction) {
        Organization organization = organizationService.getCurrentUserOrganization();
        if (transaction.getOrganization() == null) {
            transaction.setOrganization(organization);
            transactionRepository.save(transaction);
            return;
        }
        if (!organization.getId().equals(transaction.getOrganization().getId())) {
            throw new NotFoundException(getMessage("transaction.not_found"));
        }
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
