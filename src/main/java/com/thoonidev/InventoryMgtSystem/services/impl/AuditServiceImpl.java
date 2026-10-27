package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.AuditLogDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.models.AuditLog;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.User;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.repositories.AuditLogRepository;
import com.thoonidev.InventoryMgtSystem.repositories.UserRepository;
import com.thoonidev.InventoryMgtSystem.services.AuditService;
import com.thoonidev.InventoryMgtSystem.services.PlanLimitService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final Map<String, String> ACTION_LABELS = Map.ofEntries(
            Map.entry("LOGIN", "Inicio de sesion"),
            Map.entry("CREATE", "Registro creado"),
            Map.entry("UPDATE", "Registro actualizado"),
            Map.entry("DELETE", "Registro eliminado"),
            Map.entry("ARCHIVE", "Registro archivado"),
            Map.entry("PURCHASE", "Compra registrada"),
            Map.entry("SALE", "Venta registrada"),
            Map.entry("RETURN", "Devolucion a proveedor"),
            Map.entry("CUSTOMER_RETURN", "Devolucion de cliente"),
            Map.entry("UPDATE_STATUS", "Cambio de estado"),
            Map.entry("CARGA_INICIAL", "Carga inicial"),
            Map.entry("INICIO_SESION", "Inicio de sesion"),
            Map.entry("CREACION_PRODUCTO", "Producto registrado"),
            Map.entry("COMPRA_REGISTRADA", "Compra registrada"),
            Map.entry("VENTA_REGISTRADA", "Venta registrada"),
            Map.entry("ALERTA_STOCK", "Alerta de stock"),
            Map.entry("DEVOLUCION_PROVEEDOR", "Devolucion a proveedor"),
            Map.entry("APROBACION_PENDIENTE", "Aprobacion pendiente"),
            Map.entry("REVISION_DEVOLUCION", "Revision de devolucion"),
            Map.entry("CONSULTA_REPORTE", "Consulta de reporte")
    );

    private static final Map<String, String> ENTITY_LABELS = Map.ofEntries(
            Map.entry("User", "Usuario"),
            Map.entry("Product", "Producto"),
            Map.entry("Category", "Categoria"),
            Map.entry("Supplier", "Proveedor"),
            Map.entry("Customer", "Cliente"),
            Map.entry("Transaction", "Movimiento"),
            Map.entry("Report", "Reporte"),
            Map.entry("Database", "Base de datos"),
            Map.entry("Warehouse", "Almacen o sede"),
            Map.entry("QualityRecord", "Calidad ISO 9001"),
            Map.entry("ProductionOrder", "Orden de produccion")
    );

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final MessageSource messageSource;
    private final PlanLimitService planLimitService;

    @Override
    @Transactional
    public void log(String action, String entity, Long entityId, String detail, Organization organization, User user) {
        auditLogRepository.save(AuditLog.builder()
                .action(labelFor(ACTION_LABELS, action))
                .entity(labelFor(ENTITY_LABELS, entity))
                .entityId(entityId)
                .detail(buildDetail(action, detail))
                .organization(organization)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Response getAuditLogs() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        Organization organization = currentUser.getOrganization();
        planLimitService.validateAuditEnabled(organization);
        List<AuditLog> auditLogs = auditLogRepository.findByOrganizationId(
                organization.getId(),
                Sort.by(Sort.Direction.DESC, "id")
        );
        List<AuditLogDTO> auditLogDTOS = auditLogs.stream()
                .map(auditLog -> modelMapper.map(auditLog, AuditLogDTO.class))
                .toList();

        return Response.builder()
                .status(200)
                .message(messageSource.getMessage("audit.list.success", null, LocaleContextHolder.getLocale()))
                .auditLogs(auditLogDTOS)
                .build();
    }

    private String labelFor(Map<String, String> labels, String value) {
        if (value == null || value.isBlank()) return value;
        return labels.getOrDefault(value, value);
    }

    private String buildDetail(String action, String detail) {
        if (detail == null || detail.isBlank()) return "Accion registrada.";
        if ("LOGIN".equals(action)) return "El usuario inicio sesion correctamente.";
        if ("UPDATE_STATUS".equals(action)) return "El documento cambio a estado: " + statusLabel(detail) + ".";
        return detail;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "PENDING" -> "Pendiente";
            case "PROCESSING" -> "En proceso";
            case "COMPLETED" -> "Completado";
            case "CANCELLED" -> "Cancelado";
            default -> status;
        };
    }
}
