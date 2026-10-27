package com.thoonidev.InventoryMgtSystem.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoredFileDTO {
    private String url;
    private String publicId;
    private String contentType;
    private Long size;
}
