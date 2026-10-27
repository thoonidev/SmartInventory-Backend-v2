package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.CategoryDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.Category;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.repositories.CategoryRepository;
import com.thoonidev.InventoryMgtSystem.services.AuditService;
import com.thoonidev.InventoryMgtSystem.services.CategoryService;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import com.thoonidev.InventoryMgtSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final MessageSource messageSource;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final AuditService auditService;

    @Override
    public Response createCategory(CategoryDTO categoryDTO) {
        Organization organization = organizationService.getCurrentUserOrganization();
        Category categoryToSave = modelMapper.map(categoryDTO, Category.class);
        categoryToSave.setOrganization(organization);
        categoryToSave.setActive(true);
        Category savedCategory = categoryRepository.save(categoryToSave);
        auditService.log("CREATE", "Category", savedCategory.getId(), savedCategory.getName(), organization, userService.getCurrentLoggedInUser());

        String message = messageSource.getMessage("category.saved", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .build();
    }

    @Override
    public Response getAllCategories() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Category> categories = categoryRepository.findByOrganizationIdOrOrganizationIsNull(
                organization.getId(),
                Sort.by(Sort.Direction.DESC, "id")
        );
        categories.forEach(category -> category.setProducts(null));

        List<CategoryDTO> categoryDTOList = categories.stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();

        String message = messageSource.getMessage("category.list", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .categories(categoryDTOList)
                .build();
    }

    @Override
    public Response getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messageSource.getMessage("category.not_found", null, LocaleContextHolder.getLocale())
                ));
        validateSameOrganization(category);

        CategoryDTO categoryDTO = modelMapper.map(category, CategoryDTO.class);

        String message = messageSource.getMessage("category.found", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .category(categoryDTO)
                .build();
    }

    @Override
    public Response updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messageSource.getMessage("category.not_found", null, LocaleContextHolder.getLocale())
                ));
        validateSameOrganization(existingCategory);

        existingCategory.setName(categoryDTO.getName());
        categoryRepository.save(existingCategory);
        auditService.log("UPDATE", "Category", existingCategory.getId(), existingCategory.getName(), organizationService.getCurrentUserOrganization(), userService.getCurrentLoggedInUser());

        String message = messageSource.getMessage("category.updated", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .build();
    }

    @Override
    public Response deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messageSource.getMessage("category.not_found", null, LocaleContextHolder.getLocale())
                ));
        validateSameOrganization(category);

        category.setActive(false);
        categoryRepository.save(category);
        auditService.log("ARCHIVE", "Category", category.getId(), category.getName(), organizationService.getCurrentUserOrganization(), userService.getCurrentLoggedInUser());

        String message = messageSource.getMessage("category.archived", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .build();
    }

    private void validateSameOrganization(Category category) {
        Organization organization = organizationService.getCurrentUserOrganization();
        if (category.getOrganization() == null) {
            category.setOrganization(organization);
            categoryRepository.save(category);
            return;
        }
        if (!organization.getId().equals(category.getOrganization().getId())) {
            throw new NotFoundException(
                    messageSource.getMessage("category.not_found", null, LocaleContextHolder.getLocale()));
        }
    }
}
