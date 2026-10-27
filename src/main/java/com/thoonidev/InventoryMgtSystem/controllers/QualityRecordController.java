package com.thoonidev.InventoryMgtSystem.controllers;

import com.thoonidev.InventoryMgtSystem.dtos.QualityRecordDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.QualityRecord;
import com.thoonidev.InventoryMgtSystem.models.User;
import com.thoonidev.InventoryMgtSystem.repositories.QualityRecordRepository;
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
@RequestMapping("/api/quality-records")
@RequiredArgsConstructor
public class QualityRecordController {
    private final QualityRecordRepository qualityRecordRepository;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Response> getAll() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<QualityRecordDTO> records = qualityRecordRepository.findByOrganizationId(
                organization.getId(), Sort.by(Sort.Direction.DESC, "id")
        ).stream().map(this::toDto).toList();
        return ResponseEntity.ok(Response.builder().status(200).message("Registros de calidad obtenidos").qualityRecords(records).build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Response> create(@RequestBody @Valid QualityRecordDTO dto) {
        Organization organization = organizationService.getCurrentUserOrganization();
        User user = userService.getCurrentLoggedInUser();
        QualityRecord record = QualityRecord.builder()
                .code(dto.getCode())
                .processArea(dto.getProcessArea())
                .referenceDocument(dto.getReferenceDocument())
                .findingType(dto.getFindingType())
                .severity(dto.getSeverity())
                .status(dto.getStatus() == null ? "OPEN" : dto.getStatus())
                .description(dto.getDescription())
                .correctiveAction(dto.getCorrectiveAction())
                .responsibleName(dto.getResponsibleName())
                .sourceType(dto.getSourceType())
                .sourceId(dto.getSourceId())
                .productName(dto.getProductName())
                .active(true)
                .dueDate(dto.getDueDate())
                .closedAt(dto.getClosedAt())
                .organization(organization)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        QualityRecord saved = qualityRecordRepository.save(record);
        auditService.log("CREATE", "QualityRecord", saved.getId(), saved.getCode(), organization, user);
        return ResponseEntity.ok(Response.builder().status(200).message("Registro de calidad creado").qualityRecord(toDto(saved)).build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Response> update(@PathVariable Long id, @RequestBody @Valid QualityRecordDTO dto) {
        Organization organization = organizationService.getCurrentUserOrganization();
        QualityRecord record = findOwned(id, organization);
        if (dto.getCode() != null) record.setCode(dto.getCode());
        if (dto.getProcessArea() != null) record.setProcessArea(dto.getProcessArea());
        if (dto.getReferenceDocument() != null) record.setReferenceDocument(dto.getReferenceDocument());
        if (dto.getFindingType() != null) record.setFindingType(dto.getFindingType());
        if (dto.getSeverity() != null) record.setSeverity(dto.getSeverity());
        if (dto.getStatus() != null) record.setStatus(dto.getStatus());
        if (dto.getDescription() != null) record.setDescription(dto.getDescription());
        if (dto.getCorrectiveAction() != null) record.setCorrectiveAction(dto.getCorrectiveAction());
        if (dto.getResponsibleName() != null) record.setResponsibleName(dto.getResponsibleName());
        if (dto.getSourceType() != null) record.setSourceType(dto.getSourceType());
        if (dto.getSourceId() != null) record.setSourceId(dto.getSourceId());
        if (dto.getProductName() != null) record.setProductName(dto.getProductName());
        if (dto.getDueDate() != null) record.setDueDate(dto.getDueDate());
        record.setClosedAt("CLOSED".equals(record.getStatus()) ? LocalDateTime.now() : null);
        QualityRecord saved = qualityRecordRepository.save(record);
        auditService.log("UPDATE", "QualityRecord", saved.getId(), saved.getCode(), organization, userService.getCurrentLoggedInUser());
        return ResponseEntity.ok(Response.builder().status(200).message("Registro de calidad actualizado").qualityRecord(toDto(saved)).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> delete(@PathVariable Long id) {
        Organization organization = organizationService.getCurrentUserOrganization();
        QualityRecord record = findOwned(id, organization);
        record.setActive(false);
        qualityRecordRepository.save(record);
        auditService.log("ARCHIVE", "QualityRecord", id, record.getCode(), organization, userService.getCurrentLoggedInUser());
        return ResponseEntity.ok(Response.builder().status(200).message("Registro de calidad archivado").build());
    }

    private QualityRecord findOwned(Long id, Organization organization) {
        QualityRecord record = qualityRecordRepository.findById(id).orElseThrow(() -> new NotFoundException("Registro de calidad no encontrado"));
        if (record.getOrganization() == null || !organization.getId().equals(record.getOrganization().getId())) {
            throw new NotFoundException("Registro de calidad no encontrado");
        }
        return record;
    }

    private QualityRecordDTO toDto(QualityRecord record) {
        QualityRecordDTO dto = new QualityRecordDTO();
        dto.setId(record.getId());
        dto.setCode(record.getCode());
        dto.setProcessArea(record.getProcessArea());
        dto.setReferenceDocument(record.getReferenceDocument());
        dto.setFindingType(record.getFindingType());
        dto.setSeverity(record.getSeverity());
        dto.setStatus(record.getStatus());
        dto.setDescription(record.getDescription());
        dto.setCorrectiveAction(record.getCorrectiveAction());
        dto.setResponsibleName(record.getResponsibleName());
        dto.setSourceType(record.getSourceType());
        dto.setSourceId(record.getSourceId());
        dto.setProductName(record.getProductName());
        dto.setActive(record.getActive());
        dto.setDueDate(record.getDueDate());
        dto.setClosedAt(record.getClosedAt());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUserName(record.getUser() != null ? record.getUser().getName() : null);
        return dto;
    }
}
