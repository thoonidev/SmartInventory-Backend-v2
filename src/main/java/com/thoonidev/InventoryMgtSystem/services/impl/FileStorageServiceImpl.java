package com.thoonidev.InventoryMgtSystem.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.thoonidev.InventoryMgtSystem.dtos.StoredFileDTO;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_IMAGE_SIZE = 1024L * 1024L * 5L;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/avif");

    @Value("${smartinventory.storage.provider:cloudinary}")
    private String provider;

    @Value("${smartinventory.storage.local-path:uploads}")
    private String localPath;

    @Value("${smartinventory.storage.public-url:http://localhost:5050/uploads}")
    private String publicUrl;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Override
    public StoredFileDTO uploadProductImage(MultipartFile file, Organization organization, String sku) {
        validateImage(file);
        if (useCloudinary()) {
            return uploadToCloudinary(file, organization, sku);
        }
        return uploadLocal(file, organization, sku);
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            if (publicId.startsWith("local:")) {
                Files.deleteIfExists(Path.of(localPath).resolve(publicId.substring("local:".length())).normalize());
                return;
            }
            if (useCloudinary()) {
                cloudinary().uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (Exception ignored) {
            // Deleting the old image is best-effort; product persistence should not fail because of it.
        }
    }

    private StoredFileDTO uploadToCloudinary(MultipartFile file, Organization organization, String sku) {
        try {
            String publicId = productPublicId(organization, sku);
            Map<?, ?> result = cloudinary().uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "overwrite", true,
                    "resource_type", "image",
                    "folder", "smartinventory",
                    "format", "webp"
            ));
            return StoredFileDTO.builder()
                    .url(String.valueOf(result.get("secure_url")))
                    .publicId(String.valueOf(result.get("public_id")))
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo subir la imagen a Cloudinary: " + e.getMessage());
        }
    }

    private StoredFileDTO uploadLocal(MultipartFile file, Organization organization, String sku) {
        try {
            String extension = extensionFor(file.getContentType());
            String relativePath = "organizations/" + organization.getId() + "/products/" + safeName(sku) + "-" + UUID.randomUUID() + extension;
            Path destination = Path.of(localPath).resolve(relativePath).normalize();
            Files.createDirectories(destination.getParent());
            file.transferTo(destination);
            return StoredFileDTO.builder()
                    .url(publicUrl.replaceAll("/$", "") + "/" + relativePath.replace("\\", "/"))
                    .publicId("local:" + relativePath.replace("\\", "/"))
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo guardar la imagen localmente: " + e.getMessage());
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Seleccione una imagen valida.");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType()) || file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Solo se permiten imagenes JPG, PNG, WEBP o AVIF de hasta 5MB.");
        }
    }

    private boolean useCloudinary() {
        return "cloudinary".equalsIgnoreCase(provider) &&
                !cloudName.isBlank() &&
                !apiKey.isBlank() &&
                !apiSecret.isBlank();
    }

    private Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    private String productPublicId(Organization organization, String sku) {
        return "organizations/" + organization.getId() + "/products/" + safeName(sku);
    }

    private String safeName(String value) {
        String normalized = Normalizer.normalize(value == null ? "product" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "product" : normalized;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/avif" -> ".avif";
            default -> ".jpg";
        };
    }
}
