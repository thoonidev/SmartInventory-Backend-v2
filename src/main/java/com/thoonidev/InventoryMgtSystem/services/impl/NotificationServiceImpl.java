package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.LiveEventDTO;
import com.thoonidev.InventoryMgtSystem.dtos.NotificationDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.Notification;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.User;
import com.thoonidev.InventoryMgtSystem.repositories.NotificationRepository;
import com.thoonidev.InventoryMgtSystem.services.LiveEventService;
import com.thoonidev.InventoryMgtSystem.services.NotificationService;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final OrganizationService organizationService;
    private final LiveEventService liveEventService;
    private final ModelMapper modelMapper;

    @Override
    public void create(Organization organization, User user, String type, String title, String message, String severity) {
        if (organization == null) return;
        Notification notification = notificationRepository.save(Notification.builder()
                .organization(organization)
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .severity(severity)
                .read(false)
                .build());

        liveEventService.publish(organization.getId(), LiveEventDTO.builder()
                .type("NOTIFICATION")
                .title(title)
                .message(message)
                .severity(severity)
                .entity("Notification")
                .entityId(notification.getId())
                .data(Map.of(
                        "notificationId", notification.getId(),
                        "notificationType", type,
                        "read", false
                ))
                .build());
    }

    @Override
    public Response getCurrentTenantNotifications() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<NotificationDTO> notifications = notificationRepository.findByOrganizationId(
                        organization.getId(),
                        Sort.by(Sort.Direction.DESC, "id")
                ).stream()
                .map(notification -> modelMapper.map(notification, NotificationDTO.class))
                .toList();

        return Response.builder()
                .status(200)
                .message("Notificaciones obtenidas correctamente")
                .notifications(notifications)
                .unreadNotifications(notificationRepository.countByOrganizationIdAndReadFalse(organization.getId()))
                .build();
    }

    @Override
    @Transactional
    public Response markAsRead(Long id) {
        Organization organization = organizationService.getCurrentUserOrganization();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notificacion no encontrada"));
        if (notification.getOrganization() == null || !organization.getId().equals(notification.getOrganization().getId())) {
            throw new NotFoundException("Notificacion no encontrada");
        }
        notification.setRead(true);
        notificationRepository.save(notification);

        return Response.builder()
                .status(200)
                .message("Notificacion marcada como leida")
                .build();
    }

    @Override
    @Transactional
    public Response markAllAsRead() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Notification> notifications = notificationRepository.findByOrganizationId(
                organization.getId(),
                Sort.by(Sort.Direction.DESC, "id")
        );
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);

        return Response.builder()
                .status(200)
                .message("Notificaciones marcadas como leidas")
                .build();
    }
}
