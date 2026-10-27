package com.thoonidev.InventoryMgtSystem.config;

import com.thoonidev.InventoryMgtSystem.dtos.*;
import com.thoonidev.InventoryMgtSystem.models.*;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {
    @Bean
    public ModelMapper modelMapper(){
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STANDARD)
                .setPreferNestedProperties(false)
                .setCollectionsMergeEnabled(false);

        modelMapper.createTypeMap(Organization.class, OrganizationDTO.class)
                .setConverter(context -> toOrganizationDTO(context.getSource()));

        modelMapper.createTypeMap(User.class, UserDTO.class)
                .setConverter(context -> toUserDTO(context.getSource()));

        modelMapper.createTypeMap(Product.class, ProductDTO.class)
                .setConverter(context -> toProductDTO(context.getSource()));

        modelMapper.createTypeMap(Category.class, CategoryDTO.class)
                .setConverter(context -> toCategoryDTO(context.getSource()));

        modelMapper.createTypeMap(Supplier.class, SupplierDTO.class)
                .setConverter(context -> toSupplierDTO(context.getSource()));

        modelMapper.createTypeMap(Customer.class, CustomerDTO.class)
                .setConverter(context -> toCustomerDTO(context.getSource()));

        modelMapper.createTypeMap(Transaction.class, TransactionDTO.class)
                .setConverter(context -> toTransactionDTO(context.getSource()));

        modelMapper.createTypeMap(Notification.class, NotificationDTO.class)
                .setConverter(context -> toNotificationDTO(context.getSource()));

        modelMapper.createTypeMap(CategoryDTO.class, Category.class)
                .setConverter(context -> toCategory(context.getSource()));

        modelMapper.createTypeMap(SupplierDTO.class, Supplier.class)
                .setConverter(context -> toSupplier(context.getSource()));
        return modelMapper;
    }

    private static OrganizationDTO toOrganizationDTO(Organization organization) {
        if (organization == null) return null;
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setRuc(organization.getRuc());
        dto.setPlan(organization.getPlan());
        dto.setActive(organization.getActive());
        dto.setCreatedAt(organization.getCreatedAt());
        return dto;
    }

    private static NotificationDTO toNotificationDTO(Notification notification) {
        if (notification == null) return null;
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setSeverity(notification.getSeverity());
        dto.setRead(notification.getRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setOrganizationId(notification.getOrganization() != null ? notification.getOrganization().getId() : null);
        dto.setOrganizationName(notification.getOrganization() != null ? notification.getOrganization().getName() : null);
        dto.setUserId(notification.getUser() != null ? notification.getUser().getId() : null);
        dto.setUserName(notification.getUser() != null ? notification.getUser().getName() : null);
        return dto;
    }

    private static CustomerDTO toCustomerDTO(Customer customer) {
        if (customer == null) return null;
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setDocumentNumber(customer.getDocumentNumber());
        dto.setContactInfo(customer.getContactInfo());
        dto.setAddress(customer.getAddress());
        dto.setCustomerStatus(customer.getCustomerStatus());
        dto.setOrganizationId(customer.getOrganization() != null ? customer.getOrganization().getId() : null);
        dto.setOrganizationName(customer.getOrganization() != null ? customer.getOrganization().getName() : null);
        return dto;
    }

    private static UserDTO toUserDTO(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPassword(user.getPassword());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setActive(user.getActive());
        dto.setLastLogin(user.getLastLogin());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setOrganizationId(user.getOrganization() != null ? user.getOrganization().getId() : null);
        dto.setOrganizationName(user.getOrganization() != null ? user.getOrganization().getName() : null);
        dto.setTransactions(null);
        return dto;
    }

    private static ProductDTO toProductDTO(Product product) {
        if (product == null) return null;
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setProductId(product.getId());
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        dto.setOrganizationId(product.getOrganization() != null ? product.getOrganization().getId() : null);
        dto.setOrganizationName(product.getOrganization() != null ? product.getOrganization().getName() : null);
        dto.setName(product.getName());
        dto.setSku(product.getSku());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setDescription(product.getDescription());
        dto.setExpiryDate(product.getExpiryDate());
        dto.setImageUrl(product.getImageUrl());
        dto.setImagePublicId(product.getImagePublicId());
        dto.setImageContentType(product.getImageContentType());
        dto.setImageSize(product.getImageSize());
        dto.setAbcClass(product.getAbcClass());
        dto.setReorderPoint(product.getReorderPoint());
        dto.setReorderQuantity(product.getReorderQuantity());
        dto.setWarehouseLocation(product.getWarehouseLocation());
        dto.setInventoryValue(product.getInventoryValue());
        dto.setActive(product.getActive());
        dto.setCreatedAt(product.getCreatedAt());
        return dto;
    }

    private static CategoryDTO toCategoryDTO(Category category) {
        if (category == null) return null;
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setActive(category.getActive());
        dto.setOrganizationId(category.getOrganization() != null ? category.getOrganization().getId() : null);
        dto.setOrganizationName(category.getOrganization() != null ? category.getOrganization().getName() : null);
        dto.setProducts(null);
        return dto;
    }

    private static Category toCategory(CategoryDTO categoryDTO) {
        if (categoryDTO == null) return null;
        Category category = new Category();
        category.setId(categoryDTO.getId());
        category.setName(categoryDTO.getName());
        category.setActive(categoryDTO.getActive());
        category.setProducts(null);
        category.setOrganization(null);
        return category;
    }

    private static SupplierDTO toSupplierDTO(Supplier supplier) {
        if (supplier == null) return null;
        SupplierDTO dto = new SupplierDTO();
        dto.setId(supplier.getId());
        dto.setName(supplier.getName());
        dto.setContactInfo(supplier.getContactInfo());
        dto.setAddress(supplier.getAddress());
        dto.setLeadTimeDays(supplier.getLeadTimeDays());
        dto.setSupplierStatus(supplier.getSupplierStatus());
        dto.setOrganizationId(supplier.getOrganization() != null ? supplier.getOrganization().getId() : null);
        dto.setOrganizationName(supplier.getOrganization() != null ? supplier.getOrganization().getName() : null);
        return dto;
    }

    private static Supplier toSupplier(SupplierDTO supplierDTO) {
        if (supplierDTO == null) return null;
        Supplier supplier = new Supplier();
        supplier.setId(supplierDTO.getId());
        supplier.setName(supplierDTO.getName());
        supplier.setContactInfo(supplierDTO.getContactInfo());
        supplier.setAddress(supplierDTO.getAddress());
        supplier.setLeadTimeDays(supplierDTO.getLeadTimeDays());
        supplier.setSupplierStatus(supplierDTO.getSupplierStatus());
        supplier.setOrganization(null);
        return supplier;
    }

    private static TransactionDTO toTransactionDTO(Transaction transaction) {
        if (transaction == null) return null;
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setTotalProducts(transaction.getTotalProducts());
        dto.setTotalPrice(transaction.getTotalPrice());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setStatus(transaction.getStatus());
        dto.setDescription(transaction.getDescription());
        dto.setNote(transaction.getNote());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setUpdateAt(transaction.getUpdateAt());
        dto.setOrganizationId(transaction.getOrganization() != null ? transaction.getOrganization().getId() : null);
        dto.setOrganizationName(transaction.getOrganization() != null ? transaction.getOrganization().getName() : null);
        dto.setProduct(toProductDTO(transaction.getProduct()));
        dto.setUser(toUserDTO(transaction.getUser()));
        dto.setSupplier(toSupplierDTO(transaction.getSupplier()));
        dto.setCustomer(toCustomerDTO(transaction.getCustomer()));
        dto.setReturnReason(transaction.getReturnReason());
        dto.setReturnCondition(transaction.getReturnCondition());
        dto.setWarehouseLocation(transaction.getWarehouseLocation());
        dto.setStockApplied(transaction.getStockApplied());
        if (transaction.getItems() != null && !transaction.getItems().isEmpty()) {
            dto.setItems(transaction.getItems().stream()
                    .map(ModelMapperConfig::toTransactionItemDTO)
                    .toList());
        }
        return dto;
    }

    private static TransactionItemDTO toTransactionItemDTO(TransactionItem item) {
        if (item == null) return null;
        TransactionItemDTO dto = new TransactionItemDTO();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setSubtotal(item.getSubtotal());
        dto.setProduct(toProductDTO(item.getProduct()));
        return dto;
    }
}
