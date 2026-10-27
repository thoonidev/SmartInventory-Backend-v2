package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.LiveEventDTO;
import com.thoonidev.InventoryMgtSystem.dtos.ProductDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.dtos.StoredFileDTO;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.Category;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.Product;
import com.thoonidev.InventoryMgtSystem.repositories.CategoryRepository;
import com.thoonidev.InventoryMgtSystem.repositories.ProductRepository;
import com.thoonidev.InventoryMgtSystem.services.AuditService;
import com.thoonidev.InventoryMgtSystem.services.FileStorageService;
import com.thoonidev.InventoryMgtSystem.services.LiveEventService;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import com.thoonidev.InventoryMgtSystem.services.PlanLimitService;
import com.thoonidev.InventoryMgtSystem.services.ProductService;
import com.thoonidev.InventoryMgtSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private final CategoryRepository categoryRepository;
    private final MessageSource messageSource;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final AuditService auditService;
    private final LiveEventService liveEventService;
    private final PlanLimitService planLimitService;
    private final FileStorageService fileStorageService;

    @Override
    public Response saveProduct(ProductDTO productDTO, MultipartFile imageFile) {
        Organization organization = organizationService.getCurrentUserOrganization();
        planLimitService.validateCanCreateProduct(organization);
        validateSkuAvailable(productDTO.getSku(), organization.getId(), null);

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new NotFoundException(getMessage("category.not_found")));
        validateCategoryAccess(category, organization);

        Product productToSave = Product.builder()
                .name(productDTO.getName())
                .sku(productDTO.getSku())
                .price(productDTO.getPrice())
                .stockQuantity(productDTO.getStockQuantity())
                .description(productDTO.getDescription())
                .category(category)
                .organization(organization)
                .abcClass(productDTO.getAbcClass())
                .reorderPoint(productDTO.getReorderPoint())
                .reorderQuantity(productDTO.getReorderQuantity())
                .warehouseLocation(productDTO.getWarehouseLocation())
                .active(true)
                .build();
        updateInventoryValue(productToSave);

        if (imageFile != null && !imageFile.isEmpty()) {
            StoredFileDTO image = fileStorageService.uploadProductImage(imageFile, organization, productDTO.getSku());
            applyImage(productToSave, image);
        }

        Product savedProduct = productRepository.save(productToSave);
        auditService.log("CREATE", "Product", savedProduct.getId(), savedProduct.getName(), organization, userService.getCurrentLoggedInUser());
        publishProductEvent("PRODUCT_CREATED", "Producto creado", savedProduct, organization.getId(), "INFO");

        return Response.builder()
                .status(200)
                .message(getMessage("product.saved"))
                .build();
    }

    @Override
    public Response updateProduct(ProductDTO productDTO, MultipartFile imageFile) {
        Product existingProduct = productRepository.findById(productDTO.getProductId())
                .orElseThrow(() -> new NotFoundException(getMessage("product.not_found")));
        validateProductAccess(existingProduct);
        Organization organization = organizationService.getCurrentUserOrganization();

        if (imageFile != null && !imageFile.isEmpty()) {
            String previousPublicId = existingProduct.getImagePublicId();
            String imageSku = productDTO.getSku() == null || productDTO.getSku().isBlank()
                    ? existingProduct.getSku()
                    : productDTO.getSku();
            StoredFileDTO image = fileStorageService.uploadProductImage(imageFile, organization, imageSku);
            applyImage(existingProduct, image);
            if (previousPublicId != null && !previousPublicId.equals(image.getPublicId())) {
                fileStorageService.delete(previousPublicId);
            }
        }

        if (productDTO.getCategoryId() != null && productDTO.getCategoryId() > 0) {
            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(getMessage("category.not_found")));
            validateCategoryAccess(category, organization);
            existingProduct.setCategory(category);
        }

        if (productDTO.getName() != null && !productDTO.getName().isBlank()) {
            existingProduct.setName(productDTO.getName());
        }
        if (productDTO.getSku() != null && !productDTO.getSku().isBlank()) {
            validateSkuAvailable(productDTO.getSku(), organization.getId(), existingProduct.getId());
            existingProduct.setSku(productDTO.getSku());
        }
        if (productDTO.getDescription() != null && !productDTO.getDescription().isBlank()) {
            existingProduct.setDescription(productDTO.getDescription());
        }
        if (productDTO.getPrice() != null && productDTO.getPrice().compareTo(BigDecimal.ZERO) >= 0) {
            existingProduct.setPrice(productDTO.getPrice());
        }
        if (productDTO.getReorderPoint() != null && productDTO.getReorderPoint() >= 0) {
            existingProduct.setReorderPoint(productDTO.getReorderPoint());
        }
        if (productDTO.getReorderQuantity() != null && productDTO.getReorderQuantity() >= 0) {
            existingProduct.setReorderQuantity(productDTO.getReorderQuantity());
        }
        if (productDTO.getWarehouseLocation() != null && !productDTO.getWarehouseLocation().isBlank()) {
            existingProduct.setWarehouseLocation(productDTO.getWarehouseLocation());
        }
        if (productDTO.getAbcClass() != null && !productDTO.getAbcClass().isBlank()) {
            existingProduct.setAbcClass(productDTO.getAbcClass().toUpperCase(Locale.ROOT));
        }
        updateInventoryValue(existingProduct);

        Product savedProduct = productRepository.save(existingProduct);
        auditService.log("UPDATE", "Product", existingProduct.getId(), existingProduct.getName(), organization, userService.getCurrentLoggedInUser());
        publishProductEvent("PRODUCT_UPDATED", "Producto actualizado", savedProduct, organization.getId(), isLowStock(savedProduct) ? "WARNING" : "INFO");

        return Response.builder()
                .status(200)
                .message(getMessage("product.updated"))
                .build();
    }

    @Override
    public Response getAllProducts() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Product> productList = productRepository.findByOrganizationIdOrOrganizationIsNull(
                organization.getId(),
                Sort.by(Sort.Direction.DESC, "id")
        );
        List<ProductDTO> productDTOList = productList.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();

        return Response.builder()
                .status(200)
                .message(getMessage("product.list"))
                .products(productDTOList)
                .build();
    }

    @Override
    public Response getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(getMessage("product.not_found")));
        validateProductAccess(product);

        return Response.builder()
                .status(200)
                .message(getMessage("product.found"))
                .product(modelMapper.map(product, ProductDTO.class))
                .build();
    }

    @Override
    public Response deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(getMessage("product.not_found")));
        validateProductAccess(product);

        Organization organization = organizationService.getCurrentUserOrganization();
        product.setActive(false);
        productRepository.save(product);
        auditService.log("ARCHIVE", "Product", product.getId(), product.getName(), organization, userService.getCurrentLoggedInUser());
        publishProductEvent("PRODUCT_ARCHIVED", "Producto archivado", product, organization.getId(), "WARNING");

        return Response.builder()
                .status(200)
                .message(getMessage("product.archived"))
                .build();
    }

    @Override
    public Response searchProduct(String input) {
        if (input == null || input.isBlank()) {
            return getAllProducts();
        }
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Product> products = productRepository.findByNameContainingOrDescriptionContaining(input, input);
        products = products.stream()
                .filter(product -> product.getOrganization() == null ||
                        organization.getId().equals(product.getOrganization().getId()))
                .toList();

        if (products.isEmpty()) {
            throw new NotFoundException(getMessage("product.search.empty"));
        }

        List<ProductDTO> productDTOList = products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();

        return Response.builder()
                .status(200)
                .message(getMessage("product.search.success"))
                .products(productDTOList)
                .build();
    }

    @Override
    public Response getLowStockProducts() {
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
                .message(getMessage("product.low_stock.list"))
                .products(productDTOList)
                .build();
    }

    @Override
    public Response getAbcAnalysis() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Product> products = productRepository.findByOrganizationIdOrOrganizationIsNull(
                organization.getId(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        products.forEach(this::updateInventoryValue);
        BigDecimal totalInventoryValue = products.stream()
                .map(product -> product.getInventoryValue() == null ? BigDecimal.ZERO : product.getInventoryValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalInventoryValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal accumulated = BigDecimal.ZERO;
            for (Product product : products.stream()
                    .sorted((first, second) -> nullSafeInventoryValue(second).compareTo(nullSafeInventoryValue(first)))
                    .toList()) {
                accumulated = accumulated.add(nullSafeInventoryValue(product));
                double percent = accumulated
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalInventoryValue, 2, java.math.RoundingMode.HALF_UP)
                        .doubleValue();

                if (percent <= 80) {
                    product.setAbcClass("A");
                } else if (percent <= 95) {
                    product.setAbcClass("B");
                } else {
                    product.setAbcClass("C");
                }
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

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    private void applyImage(Product product, StoredFileDTO image) {
        product.setImageUrl(image.getUrl());
        product.setImagePublicId(image.getPublicId());
        product.setImageContentType(image.getContentType());
        product.setImageSize(image.getSize());
    }

    private String getMessage(String key, Object[] args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    private void validateProductAccess(Product product) {
        Organization organization = organizationService.getCurrentUserOrganization();
        if (product.getOrganization() == null) {
            product.setOrganization(organization);
            productRepository.save(product);
            return;
        }
        if (!organization.getId().equals(product.getOrganization().getId())) {
            throw new NotFoundException(getMessage("product.not_found"));
        }
    }

    private void validateCategoryAccess(Category category, Organization organization) {
        if (category.getOrganization() == null) {
            category.setOrganization(organization);
            categoryRepository.save(category);
            return;
        }
        if (!organization.getId().equals(category.getOrganization().getId())) {
            throw new NotFoundException(getMessage("category.not_found"));
        }
    }

    private void validateSkuAvailable(String sku, Long organizationId, Long productId) {
        if (sku == null || sku.isBlank()) {
            return;
        }
        boolean skuExists = productId == null
                ? productRepository.existsBySkuAndOrganizationId(sku, organizationId)
                : productRepository.existsBySkuAndOrganizationIdAndIdNot(sku, organizationId, productId);
        if (skuExists) {
            throw new IllegalArgumentException(getMessage("product.sku.exists"));
        }
    }

    private void updateInventoryValue(Product product) {
        BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        int stock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        product.setInventoryValue(price.multiply(BigDecimal.valueOf(stock)));
    }

    private void publishProductEvent(String type, String title, Product product, Long organizationId, String severity) {
        liveEventService.publish(organizationId, LiveEventDTO.builder()
                .type(type)
                .title(title)
                .message(product.getName() + " quedo con stock " + product.getStockQuantity() + ".")
                .severity(severity)
                .entity("Product")
                .entityId(product.getId())
                .data(Map.of(
                        "productId", product.getId(),
                        "productName", product.getName(),
                        "sku", product.getSku(),
                        "stockQuantity", product.getStockQuantity() == null ? 0 : product.getStockQuantity(),
                        "reorderPoint", product.getReorderPoint() == null ? 0 : product.getReorderPoint()
                ))
                .build());

        if (isLowStock(product)) {
            liveEventService.publish(organizationId, LiveEventDTO.builder()
                    .type("STOCK_ALERT")
                    .title("Stock bajo detectado")
                    .message(product.getName() + " esta por debajo del punto de reposicion.")
                    .severity(product.getStockQuantity() != null && product.getStockQuantity() <= 0 ? "DANGER" : "WARNING")
                    .entity("Product")
                    .entityId(product.getId())
                    .data(Map.of(
                            "productId", product.getId(),
                            "productName", product.getName(),
                            "sku", product.getSku(),
                            "stockQuantity", product.getStockQuantity(),
                            "reorderPoint", product.getReorderPoint()
                    ))
                    .build());
        }
    }

    private boolean isLowStock(Product product) {
        return product.getReorderPoint() != null &&
                product.getStockQuantity() != null &&
                product.getStockQuantity() <= product.getReorderPoint();
    }

    private BigDecimal nullSafeInventoryValue(Product product) {
        return product.getInventoryValue() == null ? BigDecimal.ZERO : product.getInventoryValue();
    }
}
