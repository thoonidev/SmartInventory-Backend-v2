package com.thoonidev.InventoryMgtSystem.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thoonidev.InventoryMgtSystem.dtos.LiveEventDTO;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.repositories.OrganizationRepository;
import com.thoonidev.InventoryMgtSystem.security.JwtUtils;
import com.thoonidev.InventoryMgtSystem.services.LiveEventService;
import com.thoonidev.InventoryMgtSystem.services.PlanLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveEventServiceImpl extends TextWebSocketHandler implements LiveEventService {

    private static final String ORGANIZATION_ID_KEY = "organizationId";
    private static final String CHANNEL_KEY = "channel";
    private static final String NOTIFICATIONS_CHANNEL = "notifications";
    private final JwtUtils jwtUtils;
    private final OrganizationRepository organizationRepository;
    private final PlanLimitService planLimitService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Map<Long, Set<WebSocketSession>> sessionsByOrganization = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long organizationId = resolveOrganizationId(session.getUri());
        if (organizationId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Empresa no valida"));
            return;
        }
        Organization organization = organizationRepository.findById(organizationId).orElse(null);
        if (organization == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Empresa no encontrada"));
            return;
        }
        String channel = extractQueryValue(session.getUri(), "channel");
        channel = NOTIFICATIONS_CHANNEL.equals(channel) ? NOTIFICATIONS_CHANNEL : "live";

        if (!NOTIFICATIONS_CHANNEL.equals(channel)) {
            try {
                planLimitService.validateLiveOperationsEnabled(organization);
            } catch (Exception exception) {
                session.close(CloseStatus.POLICY_VIOLATION.withReason(exception.getMessage()));
                return;
            }
        }

        session.getAttributes().put(ORGANIZATION_ID_KEY, organizationId);
        session.getAttributes().put(CHANNEL_KEY, channel);
        sessionsByOrganization
                .computeIfAbsent(organizationId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);

        if (!NOTIFICATIONS_CHANNEL.equals(channel)) {
            publishToSession(session, LiveEventDTO.builder()
                    .type("CONNECTED")
                    .title("Conexion en vivo activa")
                    .message("Recibiras cambios de inventario de tu empresa en tiempo real.")
                    .severity("INFO")
                    .entity("WebSocket")
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    private void removeSession(WebSocketSession session) {
        Object organizationId = session.getAttributes().get(ORGANIZATION_ID_KEY);
        if (organizationId instanceof Long id && sessionsByOrganization.containsKey(id)) {
            sessionsByOrganization.get(id).remove(session);
        }
    }

    @Override
    public void publish(Long organizationId, LiveEventDTO event) {
        if (organizationId == null || event == null) return;
        event.setCreatedAt(event.getCreatedAt() == null ? LocalDateTime.now() : event.getCreatedAt());

        Set<WebSocketSession> sessions = sessionsByOrganization.getOrDefault(organizationId, Set.of());
        sessions.removeIf(session -> !session.isOpen());
        sessions.stream()
                .filter(session -> shouldReceive(session, event))
                .forEach(session -> publishToSession(session, event));
    }

    private boolean shouldReceive(WebSocketSession session, LiveEventDTO event) {
        Object channel = session.getAttributes().get(CHANNEL_KEY);
        if (NOTIFICATIONS_CHANNEL.equals(channel)) {
            return "NOTIFICATION".equals(event.getType());
        }
        return true;
    }

    private void publishToSession(WebSocketSession session, LiveEventDTO event) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
            }
        } catch (Exception exception) {
            removeSession(session);
            if (isClientDisconnect(exception)) {
                log.debug("Conexion live cerrada por el navegador antes de enviar el evento.");
                return;
            }
            log.warn("No se pudo enviar evento live: {}", exception.getMessage());
        }
    }

    private boolean isClientDisconnect(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof IOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Long resolveOrganizationId(URI uri) {
        String token = extractToken(uri);
        if (token == null || token.isBlank()) return null;
        try {
            return jwtUtils.getOrganizationIdFromToken(token);
        } catch (Exception exception) {
            log.warn("Token WebSocket invalido", exception);
            return null;
        }
    }

    private String extractToken(URI uri) {
        return extractQueryValue(uri, "token");
    }

    private String extractQueryValue(URI uri, String key) {
        if (uri == null || uri.getQuery() == null) return null;
        for (String pair : uri.getQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && key.equals(parts[0])) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
