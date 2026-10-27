package com.thoonidev.InventoryMgtSystem.controllers;

import com.thoonidev.InventoryMgtSystem.dtos.OrganizationDTO;
import com.thoonidev.InventoryMgtSystem.dtos.RegisterOrganizationRequest;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping("/register")
    public ResponseEntity<Response> registerOrganization(@RequestBody @Valid RegisterOrganizationRequest request) {
        return ResponseEntity.ok(organizationService.registerOrganization(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> getCurrentOrganization() {
        return ResponseEntity.ok(organizationService.getCurrentOrganization());
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> updateCurrentOrganization(@RequestBody OrganizationDTO organizationDTO) {
        return ResponseEntity.ok(organizationService.updateCurrentOrganization(organizationDTO));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> getCurrentOrganizationUsers() {
        return ResponseEntity.ok(organizationService.getCurrentOrganizationUsers());
    }
}
