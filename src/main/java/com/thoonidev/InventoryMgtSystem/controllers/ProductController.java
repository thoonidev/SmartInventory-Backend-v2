package com.thoonidev.InventoryMgtSystem.controllers;

import com.thoonidev.InventoryMgtSystem.dtos.ProductDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> saveProduct(
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam("name") String name,
            @RequestParam("sku") String sku,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stockQuantity") Integer stockQuantity,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "reorderPoint", required = false) Integer reorderPoint,
            @RequestParam(value = "reorderQuantity", required = false) Integer reorderQuantity,
            @RequestParam("warehouseLocation") String warehouseLocation
    ) {
        validateProductInput(name, sku, price, stockQuantity, categoryId, reorderPoint, reorderQuantity, warehouseLocation, null, false);
        ProductDTO productDTO = new ProductDTO();
        productDTO.setName(name);
        productDTO.setSku(sku);
        productDTO.setPrice(price);
        productDTO.setStockQuantity(stockQuantity);
        productDTO.setCategoryId(categoryId);
        productDTO.setDescription(description);
        productDTO.setReorderPoint(reorderPoint);
        productDTO.setReorderQuantity(reorderQuantity);
        productDTO.setWarehouseLocation(warehouseLocation);

        return ResponseEntity.ok(productService.saveProduct(productDTO, imageFile));

    }

    @PutMapping({"/update", "/update/{pathProductId}"})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> updateProduct(
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "sku", required = false) String sku,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam(value = "stockQuantity", required = false) Integer stockQuantity,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "reorderPoint", required = false) Integer reorderPoint,
            @RequestParam(value = "reorderQuantity", required = false) Integer reorderQuantity,
            @RequestParam(value = "warehouseLocation", required = false) String warehouseLocation,
            @RequestParam(value = "abcClass", required = false) String abcClass,
            @RequestParam(value = "productId", required = false) Long productId,
            @PathVariable(value = "pathProductId", required = false) Long pathProductId
    ) {
        ProductDTO productDTO = new ProductDTO();
        Long resolvedProductId = productId != null ? productId : pathProductId;
        if (resolvedProductId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        validateProductInput(name, sku, price, stockQuantity, categoryId, reorderPoint, reorderQuantity, warehouseLocation, abcClass, true);
        productDTO.setName(name);
        productDTO.setSku(sku);
        productDTO.setPrice(price);
        productDTO.setProductId(resolvedProductId);
        productDTO.setStockQuantity(stockQuantity);
        productDTO.setCategoryId(categoryId);
        productDTO.setDescription(description);
        productDTO.setReorderPoint(reorderPoint);
        productDTO.setReorderQuantity(reorderQuantity);
        productDTO.setWarehouseLocation(warehouseLocation);
        productDTO.setAbcClass(abcClass);

        return ResponseEntity.ok(productService.updateProduct(productDTO, imageFile));

    }


    @GetMapping("/all")
    public ResponseEntity<Response> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }


    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> deleteProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deleteProduct(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Response> searchProduct(
            @RequestParam(required = false) String input,
            @RequestParam(required = false) String searchValue
    ) {
        String value = input != null ? input : searchValue;
        return ResponseEntity.ok(productService.searchProduct(value));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<Response> getLowStockProducts() {
        return ResponseEntity.ok(productService.getLowStockProducts());
    }

    @GetMapping("/abc-analysis")
    public ResponseEntity<Response> getAbcAnalysis() {
        return ResponseEntity.ok(productService.getAbcAnalysis());
    }

    private void validateProductInput(
            String name,
            String sku,
            BigDecimal price,
            Integer stockQuantity,
            Long categoryId,
            Integer reorderPoint,
            Integer reorderQuantity,
            String warehouseLocation,
            String abcClass,
            boolean partial
    ) {
        if (!partial) {
            if (isBlank(name)) throw new IllegalArgumentException("El nombre del producto es obligatorio.");
            if (isBlank(sku)) throw new IllegalArgumentException("El SKU es obligatorio.");
            if (price == null) throw new IllegalArgumentException("El precio es obligatorio.");
            if (stockQuantity == null) throw new IllegalArgumentException("La cantidad en stock es obligatoria.");
            if (categoryId == null) throw new IllegalArgumentException("La categoria es obligatoria.");
            if (isBlank(warehouseLocation)) throw new IllegalArgumentException("El almacen o sede es obligatorio.");
        }
        if (name != null && isBlank(name)) throw new IllegalArgumentException("El nombre del producto no puede estar vacio.");
        if (sku != null && !sku.matches("^[A-Za-z0-9._-]{2,30}$")) {
            throw new IllegalArgumentException("El SKU debe tener entre 2 y 30 caracteres y solo usar letras, numeros, punto, guion o guion bajo.");
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero.");
        }
        if (stockQuantity != null && stockQuantity < 0) {
            throw new IllegalArgumentException("La cantidad en stock no puede ser negativa.");
        }
        if (categoryId != null && categoryId <= 0) {
            throw new IllegalArgumentException("La categoria seleccionada no es valida.");
        }
        if (warehouseLocation != null && isBlank(warehouseLocation)) {
            throw new IllegalArgumentException("El almacen o sede no puede estar vacio.");
        }
        if (reorderPoint != null && reorderPoint < 0) {
            throw new IllegalArgumentException("El punto de reorden no puede ser negativo.");
        }
        if (reorderQuantity != null && reorderQuantity < 0) {
            throw new IllegalArgumentException("La cantidad sugerida no puede ser negativa.");
        }
        if (abcClass != null && !abcClass.isBlank() && !abcClass.matches("A|B|C")) {
            throw new IllegalArgumentException("La clase ABC solo puede ser A, B o C.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
