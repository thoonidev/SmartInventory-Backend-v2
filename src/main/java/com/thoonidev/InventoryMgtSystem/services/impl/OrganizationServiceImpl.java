package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.OrganizationDTO;
import com.thoonidev.InventoryMgtSystem.dtos.RegisterOrganizationRequest;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.dtos.UserDTO;
import com.thoonidev.InventoryMgtSystem.enums.OrganizationPlan;
import com.thoonidev.InventoryMgtSystem.enums.UserRole;
import com.thoonidev.InventoryMgtSystem.exceptions.NameValueRequiredException;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.User;
import com.thoonidev.InventoryMgtSystem.repositories.OrganizationRepository;
import com.thoonidev.InventoryMgtSystem.repositories.UserRepository;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import com.thoonidev.InventoryMgtSystem.services.PlanLimitService;
import com.thoonidev.InventoryMgtSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private static final String DEFAULT_RUC = "00000000000";
    private static final String DEFAULT_NAME = "Espacio de trabajo";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final PlanLimitService planLimitService;
    private final ModelMapper modelMapper;
    private final MessageSource messageSource;

    @Override
    @Transactional
    public Response registerOrganization(RegisterOrganizationRequest request) {
        organizationRepository.findByRuc(request.getRuc()).ifPresent(organization -> {
            throw new NameValueRequiredException(getMessage("organization.ruc.exists"));
        });

        userRepository.findByEmail(request.getAdminEmail()).ifPresent(user -> {
            throw new NameValueRequiredException(getMessage("user.email.exists"));
        });

        Organization organization = Organization.builder()
                .name(request.getOrganizationName())
                .ruc(request.getRuc())
                .plan(OrganizationPlan.FREE)
                .active(true)
                .build();
        organizationRepository.save(organization);

        User admin = User.builder()
                .name(request.getAdminName())
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .phoneNumber(request.getAdminPhoneNumber())
                .role(UserRole.ADMIN)
                .active(true)
                .organization(organization)
                .build();
        userRepository.save(admin);

        return Response.builder()
                .status(200)
                .message(getMessage("organization.register.success"))
                .organization(modelMapper.map(organization, OrganizationDTO.class))
                .build();
    }

    @Override
    public Response getCurrentOrganization() {
        Organization organization = getCurrentUserOrganization();
        return Response.builder()
                .status(200)
                .message(getMessage("organization.found"))
                .organization(modelMapper.map(organization, OrganizationDTO.class))
                .planLimits(planLimitService.getLimits(organization))
                .build();
    }

    @Override
    @Transactional
    public Response updateCurrentOrganization(OrganizationDTO organizationDTO) {
        Organization organization = getCurrentUserOrganization();

        if (organizationDTO.getName() != null && !organizationDTO.getName().isBlank()) {
            organization.setName(organizationDTO.getName());
        }
        if (organizationDTO.getRuc() != null && !organizationDTO.getRuc().isBlank()) {
            organization.setRuc(organizationDTO.getRuc());
        }
        if (organizationDTO.getPlan() != null && organizationDTO.getPlan() != organization.getPlan()) {
            throw new NameValueRequiredException("El cambio de plan debe realizarse desde Mi plan y requiere checkout de pago.");
        }
        if (organizationDTO.getActive() != null) {
            organization.setActive(organizationDTO.getActive());
        }

        organizationRepository.save(organization);

        return Response.builder()
                .status(200)
                .message(getMessage("organization.updated"))
                .build();
    }

    @Override
    public Response getCurrentOrganizationUsers() {
        Organization organization = getCurrentUserOrganization();
        List<User> users = userRepository.findByOrganizationId(organization.getId(), Sort.by(Sort.Direction.DESC, "id"));
        users.forEach(user -> user.setTransactions(null));
        List<UserDTO> userDTOS = users.stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
                .toList();

        return Response.builder()
                .status(200)
                .message(getMessage("user.list.success"))
                .users(userDTOS)
                .build();
    }

    @Override
    @Transactional
    public Organization getCurrentUserOrganization() {
        User user = userService.getCurrentLoggedInUser();
        if (user.getOrganization() == null) {
            Organization defaultOrganization = getOrCreateDefaultOrganization();
            user.setOrganization(defaultOrganization);
            userRepository.save(user);
            return defaultOrganization;
        }
        return user.getOrganization();
    }

    @Override
    @Transactional
    public Organization getOrCreateDefaultOrganization() {
        return organizationRepository.findByRuc(DEFAULT_RUC)
                .orElseGet(() -> organizationRepository.save(Organization.builder()
                        .name(DEFAULT_NAME)
                        .ruc(DEFAULT_RUC)
                        .plan(OrganizationPlan.FREE)
                        .active(true)
                        .build()));
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
