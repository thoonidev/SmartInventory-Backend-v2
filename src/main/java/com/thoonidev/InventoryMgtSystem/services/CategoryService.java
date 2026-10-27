package com.thoonidev.InventoryMgtSystem.services;

import com.thoonidev.InventoryMgtSystem.dtos.CategoryDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;

public interface CategoryService {

    Response createCategory(CategoryDTO categoryDTO);

    Response getAllCategories();

    Response getCategoryById(Long id);

    Response updateCategory(Long id, CategoryDTO categoryDTO);

    Response deleteCategory(Long id);
}
