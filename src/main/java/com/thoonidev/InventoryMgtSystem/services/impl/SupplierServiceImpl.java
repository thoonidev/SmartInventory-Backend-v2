package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.dtos.SupplierDTO;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.Supplier;
import com.thoonidev.InventoryMgtSystem.repositories.SupplierRepository;
import com.thoonidev.InventoryMgtSystem.services.AuditService;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import com.thoonidev.InventoryMgtSystem.services.SupplierService;
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
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final ModelMapper modelMapper;
    private final MessageSource messageSource;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final AuditService auditService;

    @Override
    public Response addSupplier(SupplierDTO supplierDTO) {
        Organization organization = organizationService.getCurrentUserOrganization();
        Supplier supplierToSave = modelMapper.map(supplierDTO, Supplier.class);
        if (supplierToSave.getSupplierStatus() == null || supplierToSave.getSupplierStatus().isBlank()) {
            supplierToSave.setSupplierStatus("ACTIVE");
        }
        supplierToSave.setOrganization(organization);
        Supplier savedSupplier = supplierRepository.save(supplierToSave);
        auditService.log("CREATE", "Supplier", savedSupplier.getId(), savedSupplier.getName(), organization, userService.getCurrentLoggedInUser());

        return Response.builder()
                .status(200)
                .message(getMessage("supplier.saved"))
                .build();
    }

    @Override
    public Response updateSupplier(Long id, SupplierDTO supplierDTO) {
        Supplier existingSupplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(getMessage("supplier.not_found")));
        validateSameOrganization(existingSupplier);

        if (supplierDTO.getName() != null) existingSupplier.setName(supplierDTO.getName());
        if (supplierDTO.getContactInfo() != null) existingSupplier.setContactInfo(supplierDTO.getContactInfo());
        if (supplierDTO.getAddress() != null) existingSupplier.setAddress(supplierDTO.getAddress());
        if (supplierDTO.getLeadTimeDays() != null) existingSupplier.setLeadTimeDays(supplierDTO.getLeadTimeDays());
        if (supplierDTO.getSupplierStatus() != null) existingSupplier.setSupplierStatus(supplierDTO.getSupplierStatus());

        supplierRepository.save(existingSupplier);
        auditService.log("UPDATE", "Supplier", existingSupplier.getId(), existingSupplier.getName(), organizationService.getCurrentUserOrganization(), userService.getCurrentLoggedInUser());

        return Response.builder()
                .status(200)
                .message(getMessage("supplier.updated"))
                .build();
    }

    @Override
    public Response getAllSupplier() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Supplier> suppliers = supplierRepository.findByOrganizationIdOrOrganizationIsNull(
                organization.getId(),
                Sort.by(Sort.Direction.DESC, "id")
        );
        List<SupplierDTO> supplierDTOList = suppliers.stream()
                .map(supplier -> modelMapper.map(supplier, SupplierDTO.class))
                .toList();

        return Response.builder()
                .status(200)
                .message(getMessage("supplier.list"))
                .suppliers(supplierDTOList)
                .build();
    }

    @Override
    public Response getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(getMessage("supplier.not_found")));
        validateSameOrganization(supplier);

        SupplierDTO supplierDTO = modelMapper.map(supplier, SupplierDTO.class);

        return Response.builder()
                .status(200)
                .message(getMessage("supplier.found"))
                .supplier(supplierDTO)
                .build();
    }

    @Override
    public Response deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(getMessage("supplier.not_found")));
        validateSameOrganization(supplier);

        supplier.setSupplierStatus("INACTIVE");
        supplierRepository.save(supplier);
        auditService.log("ARCHIVE", "Supplier", supplier.getId(), supplier.getName(), organizationService.getCurrentUserOrganization(), userService.getCurrentLoggedInUser());

        return Response.builder()
                .status(200)
                .message(getMessage("supplier.archived"))
                .build();
    }

    // Helper method to get messages by location
    private String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    private void validateSameOrganization(Supplier supplier) {
        Organization organization = organizationService.getCurrentUserOrganization();
        if (supplier.getOrganization() == null) {
            supplier.setOrganization(organization);
            supplierRepository.save(supplier);
            return;
        }
        if (!organization.getId().equals(supplier.getOrganization().getId())) {
            throw new NotFoundException(getMessage("supplier.not_found"));
        }
    }
}
