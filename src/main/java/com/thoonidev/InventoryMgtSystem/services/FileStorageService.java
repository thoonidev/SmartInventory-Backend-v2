package com.thoonidev.InventoryMgtSystem.services;

import com.thoonidev.InventoryMgtSystem.dtos.StoredFileDTO;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFileDTO uploadProductImage(MultipartFile file, Organization organization, String sku);

    void delete(String publicId);
}
