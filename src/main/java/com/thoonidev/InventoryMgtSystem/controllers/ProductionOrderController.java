package com.thoonidev.InventoryMgtSystem.controllers;

import com.thoonidev.InventoryMgtSystem.enums.TransactionStatus;
import com.thoonidev.InventoryMgtSystem.enums.TransactionType;
import com.thoonidev.InventoryMgtSystem.dtos.ProductionOrderDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.Product;
import com.thoonidev.InventoryMgtSystem.models.ProductionOrder;
import com.thoonidev.InventoryMgtSystem.models.Transaction;
import com.thoonidev.InventoryMgtSystem.models.TransactionItem;
import com.thoonidev.InventoryMgtSystem.models.User;
import com.thoonidev.InventoryMgtSystem.repositories.ProductRepository;
import com.thoonidev.InventoryMgtSystem.repositories.ProductionOrderRepository;
import com.thoonidev.InventoryMgtSystem.repositories.TransactionRepository;
import com.thoonidev.InventoryMgtSystem.services.AuditService;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import com.thoonidev.InventoryMgtSystem.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/production-orders")
@RequiredArgsConstructor
public class ProductionOrderController {
    private final ProductionOrderRepository productionOrderRepository;
    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Response> getAll() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<ProductionOrderDTO> orders = productionOrderRepository.findByOrganizationId(
                organization.getId(), Sort.by(Sort.Direction.DESC, "id")
        ).stream().map(this::toDto).toList();
        return ResponseEntity.ok(Response.builder().status(200).message("Ordenes de produccion obtenidas").productionOrders(orders).build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Response> create(@RequestBody @Valid ProductionOrderDTO dto) {
        validateProductionDates(dto);
        Organization organization = organizationService.getCurrentUserOrganization();
        User user = userService.getCurrentLoggedInUser();
        Product product = dto.getProductId() == null ? null : findProduct(dto.getProductId(), organization);
        ProductionOrder order = ProductionOrder.builder()
                .code(dto.getCode())
                .product(product)
                .productName(product != null ? product.getName() : dto.getProductName())
                .plannedQuantity(dto.getPlannedQuantity())
                .completedQuantity(dto.getCompletedQuantity() == null ? 0 : dto.getCompletedQuantity())
                .stockAppliedQuantity(0)
                .status(dto.getStatus() == null ? "PLANNED" : dto.getStatus())
                .active(true)
                .startDate(dto.getStartDate())
                .dueDate(dto.getDueDate())
                .notes(dto.getNotes())
                .organization(organization)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        applyFinishedStock(order, organization);
        ProductionOrder saved = productionOrderRepository.save(order);
        auditService.log("CREATE", "ProductionOrder", saved.getId(), saved.getCode(), organization, user);
        return ResponseEntity.ok(Response.builder().status(200).message("Orden de produccion creada").productionOrder(toDto(saved)).build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Response> update(@PathVariable Long id, @RequestBody @Valid ProductionOrderDTO dto) {
        validateProductionDates(dto);
        Organization organization = organizationService.getCurrentUserOrganization();
        ProductionOrder order = findOwned(id, organization);
        if (dto.getCode() != null) order.setCode(dto.getCode());
        if (dto.getProductId() != null) {
            Product product = findProduct(dto.getProductId(), organization);
            order.setProduct(product);
            order.setProductName(product.getName());
        } else if (dto.getProductName() != null) {
            order.setProductName(dto.getProductName());
        }
        if (dto.getPlannedQuantity() != null) order.setPlannedQuantity(dto.getPlannedQuantity());
        if (dto.getCompletedQuantity() != null) order.setCompletedQuantity(dto.getCompletedQuantity());
        if (dto.getStatus() != null) order.setStatus(dto.getStatus());
        if (dto.getStartDate() != null) order.setStartDate(dto.getStartDate());
        if (dto.getDueDate() != null) order.setDueDate(dto.getDueDate());
        if (dto.getNotes() != null) order.setNotes(dto.getNotes());
        applyFinishedStock(order, organization);
        ProductionOrder saved = productionOrderRepository.save(order);
        auditService.log("UPDATE", "ProductionOrder", saved.getId(), saved.getCode(), organization, userService.getCurrentLoggedInUser());
        return ResponseEntity.ok(Response.builder().status(200).message("Orden de produccion actualizada").productionOrder(toDto(saved)).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> delete(@PathVariable Long id) {
        Organization organization = organizationService.getCurrentUserOrganization();
        ProductionOrder order = findOwned(id, organization);
        order.setActive(false);
        if (!"DONE".equals(order.getStatus())) {
            order.setStatus("CANCELLED");
        }
        productionOrderRepository.save(order);
        auditService.log("ARCHIVE", "ProductionOrder", id, order.getCode(), organization, userService.getCurrentLoggedInUser());
        return ResponseEntity.ok(Response.builder().status(200).message("Orden de produccion archivada").build());
    }

    private ProductionOrder findOwned(Long id, Organization organization) {
        ProductionOrder order = productionOrderRepository.findById(id).orElseThrow(() -> new NotFoundException("Orden de produccion no encontrada"));
        if (order.getOrganization() == null || !organization.getId().equals(order.getOrganization().getId())) {
            throw new NotFoundException("Orden de produccion no encontrada");
        }
        return order;
    }

    private void validateProductionDates(ProductionOrderDTO dto) {
        if (dto.getStartDate() != null && dto.getDueDate() != null && dto.getDueDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("La fecha estimada de entrega no puede ser anterior a la fecha de inicio.");
        }
        if (dto.getCompletedQuantity() != null && dto.getPlannedQuantity() != null &&
                dto.getCompletedQuantity() > dto.getPlannedQuantity() && !"DONE".equals(dto.getStatus())) {
            throw new IllegalArgumentException("La cantidad terminada no puede superar la planificada si la orden aun no esta terminada.");
        }
    }

    private Product findProduct(Long id, Organization organization) {
        Product product = productRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        if (product.getOrganization() == null || !organization.getId().equals(product.getOrganization().getId())) {
            throw new NotFoundException("Producto no encontrado");
        }
        if (Boolean.FALSE.equals(product.getActive())) {
            throw new NotFoundException("Producto no encontrado");
        }
        return product;
    }

    private void applyFinishedStock(ProductionOrder order, Organization organization) {
        if (!"DONE".equals(order.getStatus()) || order.getProduct() == null) return;
        int completed = order.getCompletedQuantity() == null ? 0 : order.getCompletedQuantity();
        int alreadyApplied = order.getStockAppliedQuantity() == null ? 0 : order.getStockAppliedQuantity();
        int delta = completed - alreadyApplied;
        if (delta <= 0) return;
        Product product = findProduct(order.getProduct().getId(), organization);
        product.setStockQuantity((product.getStockQuantity() == null ? 0 : product.getStockQuantity()) + delta);
        product.setInventoryValue((product.getPrice() == null ? java.math.BigDecimal.ZERO : product.getPrice())
                .multiply(java.math.BigDecimal.valueOf(product.getStockQuantity() == null ? 0 : product.getStockQuantity())));
        productRepository.save(product);
        order.setStockAppliedQuantity(completed);
        registerProductionMovement(order, product, delta, organization);
    }

    private void registerProductionMovement(ProductionOrder order, Product product, int quantity, Organization organization) {
        BigDecimal unitPrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.PRODUCTION_OUTPUT)
                .status(TransactionStatus.COMPLETED)
                .product(product)
                .user(userService.getCurrentLoggedInUser())
                .organization(organization)
                .totalProducts(quantity)
                .totalPrice(subtotal)
                .description("Entrada por produccion " + order.getCode())
                .note(order.getNotes())
                .warehouseLocation(product.getWarehouseLocation())
                .stockApplied(true)
                .build();
        TransactionItem item = TransactionItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .transaction(transaction)
                .build();
        transaction.getItems().add(item);
        transactionRepository.save(transaction);
    }

    private ProductionOrderDTO toDto(ProductionOrder order) {
        ProductionOrderDTO dto = new ProductionOrderDTO();
        dto.setId(order.getId());
        dto.setCode(order.getCode());
        dto.setProductId(order.getProduct() != null ? order.getProduct().getId() : null);
        dto.setProductSku(order.getProduct() != null ? order.getProduct().getSku() : null);
        dto.setProductName(order.getProductName());
        dto.setPlannedQuantity(order.getPlannedQuantity());
        dto.setCompletedQuantity(order.getCompletedQuantity());
        dto.setStockAppliedQuantity(order.getStockAppliedQuantity());
        dto.setStatus(order.getStatus());
        dto.setActive(order.getActive());
        dto.setStartDate(order.getStartDate());
        dto.setDueDate(order.getDueDate());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUserName(order.getUser() != null ? order.getUser().getName() : null);
        return dto;
    }
}
