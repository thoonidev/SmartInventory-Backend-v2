package com.thoonidev.InventoryMgtSystem.controllers;

import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.dtos.WarehouseDTO;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.Warehouse;
import com.thoonidev.InventoryMgtSystem.repositories.WarehouseRepository;
import com.thoonidev.InventoryMgtSystem.services.AuditService;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import com.thoonidev.InventoryMgtSystem.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {
    private final WarehouseRepository warehouseRepository;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Response> getAll() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<WarehouseDTO> warehouses = warehouseRepository.findByOrganizationId(
                organization.getId(), Sort.by(Sort.Direction.DESC, "id")
        ).stream().map(this::toDto).toList();
        return ResponseEntity.ok(Response.builder().status(200).message("Almacenes obtenidos").warehouses(warehouses).build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> create(@RequestBody @Valid WarehouseDTO dto) {
        Organization organization = organizationService.getCurrentUserOrganization();
        Warehouse warehouse = Warehouse.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .type(dto.getType())
                .address(dto.getAddress())
                .managerName(dto.getManagerName())
                .active(dto.getActive() == null || dto.getActive())
                .organization(organization)
                .createdAt(LocalDateTime.now())
                .build();
        Warehouse saved = warehouseRepository.save(warehouse);
        auditService.log("CREATE", "Warehouse", saved.getId(), saved.getName(), organization, userService.getCurrentLoggedInUser());
        return ResponseEntity.ok(Response.builder().status(200).message("Almacen registrado").warehouse(toDto(saved)).build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> update(@PathVariable Long id, @RequestBody @Valid WarehouseDTO dto) {
        Organization organization = organizationService.getCurrentUserOrganization();
        Warehouse warehouse = findOwned(id, organization);
        if (dto.getName() != null) warehouse.setName(dto.getName());
        if (dto.getCode() != null) warehouse.setCode(dto.getCode());
        if (dto.getType() != null) warehouse.setType(dto.getType());
        if (dto.getAddress() != null) warehouse.setAddress(dto.getAddress());
        if (dto.getManagerName() != null) warehouse.setManagerName(dto.getManagerName());
        if (dto.getActive() != null) warehouse.setActive(dto.getActive());
        Warehouse saved = warehouseRepository.save(warehouse);
        auditService.log("UPDATE", "Warehouse", saved.getId(), saved.getName(), organization, userService.getCurrentLoggedInUser());
        return ResponseEntity.ok(Response.builder().status(200).message("Almacen actualizado").warehouse(toDto(saved)).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> delete(@PathVariable Long id) {
        Organization organization = organizationService.getCurrentUserOrganization();
        Warehouse warehouse = findOwned(id, organization);
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
        auditService.log("ARCHIVE", "Warehouse", id, warehouse.getName(), organization, userService.getCurrentLoggedInUser());
        return ResponseEntity.ok(Response.builder().status(200).message("Almacen archivado").build());
    }

    private Warehouse findOwned(Long id, Organization organization) {
        Warehouse warehouse = warehouseRepository.findById(id).orElseThrow(() -> new NotFoundException("Almacen no encontrado"));
        if (warehouse.getOrganization() == null || !organization.getId().equals(warehouse.getOrganization().getId())) {
            throw new NotFoundException("Almacen no encontrado");
        }
        return warehouse;
    }

    private WarehouseDTO toDto(Warehouse warehouse) {
        WarehouseDTO dto = new WarehouseDTO();
        dto.setId(warehouse.getId());
        dto.setName(warehouse.getName());
        dto.setCode(warehouse.getCode());
        dto.setType(warehouse.getType());
        dto.setAddress(warehouse.getAddress());
        dto.setManagerName(warehouse.getManagerName());
        dto.setActive(warehouse.getActive());
        dto.setCreatedAt(warehouse.getCreatedAt());
        return dto;
    }
}
