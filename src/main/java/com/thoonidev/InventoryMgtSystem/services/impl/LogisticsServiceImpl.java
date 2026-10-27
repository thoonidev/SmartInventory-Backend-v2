package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.LogisticsKpiDTO;
import com.thoonidev.InventoryMgtSystem.dtos.ProductDTO;
import com.thoonidev.InventoryMgtSystem.dtos.ReorderSuggestionDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.enums.TransactionType;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.Product;
import com.thoonidev.InventoryMgtSystem.models.Transaction;
import com.thoonidev.InventoryMgtSystem.repositories.ProductRepository;
import com.thoonidev.InventoryMgtSystem.repositories.SupplierRepository;
import com.thoonidev.InventoryMgtSystem.repositories.TransactionRepository;
import com.thoonidev.InventoryMgtSystem.services.LogisticsService;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogisticsServiceImpl implements LogisticsService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final TransactionRepository transactionRepository;
    private final OrganizationService organizationService;
    private final ModelMapper modelMapper;
    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    public Response getKpis() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Product> products = productRepository.findByOrganizationId(organization.getId(), Sort.by(Sort.Direction.ASC, "name"));
        List<Transaction> transactions = transactionRepository.findByOrganizationId(organization.getId(), Sort.by(Sort.Direction.DESC, "id"));

        BigDecimal inventoryValue = products.stream()
                .map(this::calculateInventoryValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalUnits = products.stream()
                .mapToInt(product -> product.getStockQuantity() == null ? 0 : product.getStockQuantity())
                .sum();
        long lowStock = products.stream().filter(this::isLowStock).count();
        long criticalStock = products.stream().filter(this::isCriticalStock).count();

        BigDecimal purchaseAmount = sumTransactions(transactions, TransactionType.PURCHASE);
        BigDecimal salesAmount = sumTransactions(transactions, TransactionType.SALE);

        LogisticsKpiDTO kpi = new LogisticsKpiDTO(
                (long) products.size(),
                (long) supplierRepository.findByOrganizationId(organization.getId(), Sort.by(Sort.Direction.ASC, "name")).size(),
                lowStock,
                criticalStock,
                totalUnits,
                inventoryValue,
                purchaseAmount,
                salesAmount
        );

        return Response.builder()
                .status(200)
                .message(getMessage("logistics.kpi.success"))
                .logisticsKpi(kpi)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response getStockAlerts() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Product> products = productRepository.findLowStockByOrganizationId(
                organization.getId(),
                Sort.by(Sort.Direction.ASC, "stockQuantity")
        );
        List<ProductDTO> productDTOList = products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();

        return Response.builder()
                .status(200)
                .message(getMessage("logistics.alerts.success"))
                .products(productDTOList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Response getReorderSuggestions() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Product> products = productRepository.findLowStockByOrganizationId(
                organization.getId(),
                Sort.by(Sort.Direction.ASC, "stockQuantity")
        );
        List<ReorderSuggestionDTO> suggestions = products.stream()
                .map(product -> new ReorderSuggestionDTO(
                        product.getId(),
                        product.getName(),
                        product.getSku(),
                        product.getStockQuantity(),
                        product.getReorderPoint(),
                        resolveSuggestedQuantity(product),
                        product.getWarehouseLocation()
                ))
                .toList();

        return Response.builder()
                .status(200)
                .message(getMessage("logistics.reorder.success"))
                .reorderSuggestions(suggestions)
                .build();
    }

    @Override
    @Transactional
    public Response getAbcAnalysis() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Product> products = productRepository.findByOrganizationId(organization.getId(), Sort.by(Sort.Direction.DESC, "id"));
        BigDecimal totalValue = products.stream()
                .map(this::calculateInventoryValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal accumulated = BigDecimal.ZERO;
            for (Product product : products.stream()
                    .sorted((first, second) -> calculateInventoryValue(second).compareTo(calculateInventoryValue(first)))
                    .toList()) {
                accumulated = accumulated.add(calculateInventoryValue(product));
                double percentage = accumulated
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalValue, 2, java.math.RoundingMode.HALF_UP)
                        .doubleValue();
                product.setAbcClass(percentage <= 80 ? "A" : percentage <= 95 ? "B" : "C");
                product.setInventoryValue(calculateInventoryValue(product));
            }
            productRepository.saveAll(products);
        }

        List<ProductDTO> productDTOList = products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();
        return Response.builder()
                .status(200)
                .message(getMessage("product.abc.list"))
                .products(productDTOList)
                .build();
    }

    private BigDecimal calculateInventoryValue(Product product) {
        BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        int stock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        return price.multiply(BigDecimal.valueOf(stock));
    }

    private BigDecimal sumTransactions(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == type)
                .map(transaction -> transaction.getTotalPrice() == null ? BigDecimal.ZERO : transaction.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isLowStock(Product product) {
        return product.getReorderPoint() != null &&
                product.getStockQuantity() != null &&
                product.getStockQuantity() <= product.getReorderPoint();
    }

    private boolean isCriticalStock(Product product) {
        return product.getStockQuantity() != null && product.getStockQuantity() <= 0;
    }

    private Integer resolveSuggestedQuantity(Product product) {
        if (product.getReorderQuantity() != null && product.getReorderQuantity() > 0) {
            return product.getReorderQuantity();
        }
        if (product.getReorderPoint() == null || product.getStockQuantity() == null) {
            return 0;
        }
        return Math.max(product.getReorderPoint() - product.getStockQuantity(), 0);
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
