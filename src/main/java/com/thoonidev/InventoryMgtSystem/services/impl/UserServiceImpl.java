package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.LoginRequest;
import com.thoonidev.InventoryMgtSystem.dtos.RegisterRequest;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.dtos.UserDTO;
import com.thoonidev.InventoryMgtSystem.dtos.OrganizationDTO;
import com.thoonidev.InventoryMgtSystem.enums.UserRole;
import com.thoonidev.InventoryMgtSystem.exceptions.InvalidCredentialsException;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.User;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.enums.OrganizationPlan;
import com.thoonidev.InventoryMgtSystem.exceptions.NameValueRequiredException;
import com.thoonidev.InventoryMgtSystem.repositories.OrganizationRepository;
import com.thoonidev.InventoryMgtSystem.repositories.UserRepository;
import com.thoonidev.InventoryMgtSystem.security.JwtUtils;
import com.thoonidev.InventoryMgtSystem.services.AuditService;
import com.thoonidev.InventoryMgtSystem.services.PlanLimitService;
import com.thoonidev.InventoryMgtSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private static final String DEFAULT_RUC = "00000000000";
    private static final String DEFAULT_NAME = "Espacio de trabajo";

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final JwtUtils jwtUtils;
    private final MessageSource messageSource;
    private final AuditService auditService;
    private final PlanLimitService planLimitService;

    @Override
    public Response registerUser(RegisterRequest registerRequest) {
        userRepository.findByEmail(registerRequest.getEmail()).ifPresent(user -> {
            throw new NameValueRequiredException(
                    messageSource.getMessage("user.email.exists", null, LocaleContextHolder.getLocale()));
        });

        UserRole role = UserRole.MANAGER;
        if (registerRequest.getRole() != null) {
            role = registerRequest.getRole();
        }

        Organization organization = resolveOrganizationForRegister(registerRequest.getOrganizationId());
        planLimitService.validateCanCreateUser(organization);

        User userToSave = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .phoneNumber(registerRequest.getPhoneNumber())
                .role(role)
                .active(true)
                .organization(organization)
                .build();

        userRepository.save(userToSave);

        String message = messageSource.getMessage("user.register.success", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .build();
    }

    @Override
    public Response loginUser(LoginRequest loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new NotFoundException(
                        messageSource.getMessage("user.email.not_found", null, LocaleContextHolder.getLocale())));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException(
                    messageSource.getMessage("user.password.mismatch", null, LocaleContextHolder.getLocale()));
        }
        Organization organization = ensureUserOrganization(user);
        if (Boolean.FALSE.equals(user.getActive())) {
            throw new InvalidCredentialsException(
                    messageSource.getMessage("user.inactive", null, LocaleContextHolder.getLocale()));
        }
        if (Boolean.FALSE.equals(organization.getActive())) {
            throw new InvalidCredentialsException(
                    messageSource.getMessage("organization.inactive", null, LocaleContextHolder.getLocale()));
        }

        user.setLastLogin(java.time.LocalDateTime.now());
        userRepository.save(user);
        String token = jwtUtils.generateToken(user);
        auditService.log("LOGIN", "User", user.getId(), user.getEmail(), organization, user);

        String message = messageSource.getMessage("user.login.success", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .role(user.getRole())
                .token(token)
                .organization(modelMapper.map(organization, OrganizationDTO.class))
                .expirationTime("6 months")
                .build();
    }

    @Override
    public Response getAllUsers() {

        Organization organization = getCurrentUserOrganization();
        List<User> users = userRepository.findByOrganizationId(organization.getId(), Sort.by(Sort.Direction.DESC, "id"));
        users.forEach(user -> user.setTransactions(null));

        List<UserDTO> userDTOS = users.stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
                .toList();

        String message = messageSource.getMessage("user.list.success", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .users(userDTOS)
                .build();
    }

    @Override
    public User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(
                        messageSource.getMessage("user.not_found", null, LocaleContextHolder.getLocale())));
        ensureUserOrganization(user);
        user.setTransactions(null);

        return user;
    }

    @Override
    public Response getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messageSource.getMessage("user.not_found", null, LocaleContextHolder.getLocale())));

        validateSameOrganization(user);

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        userDTO.setTransactions(null);

        String message = messageSource.getMessage("user.detail.success", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .user(userDTO)
                .build();
    }

    @Override
    public Response updateUser(Long id, UserDTO userDTO) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messageSource.getMessage("user.not_found", null, LocaleContextHolder.getLocale())));
        validateSameOrganization(existingUser);

        if (userDTO.getEmail() != null) existingUser.setEmail(userDTO.getEmail());
        if (userDTO.getPhoneNumber() != null) existingUser.setPhoneNumber(userDTO.getPhoneNumber());
        if (userDTO.getName() != null) existingUser.setName(userDTO.getName());
        if (userDTO.getRole() != null) existingUser.setRole(userDTO.getRole());
        if (userDTO.getActive() != null) existingUser.setActive(userDTO.getActive());

        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }
        userRepository.save(existingUser);
        auditService.log("UPDATE", "User", existingUser.getId(), existingUser.getEmail(), getCurrentUserOrganization(), getCurrentLoggedInUser());

        String message = messageSource.getMessage("user.update.success", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .build();
    }

    @Override
    public Response deleteUser(Long id) {
        User currentUser = getCurrentLoggedInUser();
        Organization currentOrganization = ensureUserOrganization(currentUser);

        User userToArchive = userRepository.findById(id)
                .map(user -> {
                    validateSameOrganization(user);
                    return user;
                })
                .orElseThrow(() -> new NotFoundException(
                    messageSource.getMessage("user.not_found", null, LocaleContextHolder.getLocale())));

        if (Objects.equals(currentUser.getId(), id)) {
            throw new IllegalArgumentException("No puedes desactivar tu propio usuario desde esta accion.");
        }

        userToArchive.setActive(false);
        userRepository.save(userToArchive);
        auditService.log("ARCHIVE", "User", id, "Usuario desactivado", currentOrganization, currentUser);

        String message = messageSource.getMessage("user.archive.success", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .build();
    }

    @Override
    public Response getUserTransactions(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messageSource.getMessage("user.not_found", null, LocaleContextHolder.getLocale())));

        validateSameOrganization(user);

        UserDTO userDTO = modelMapper.map(user, UserDTO.class);

        if (userDTO.getTransactions() != null) {
            userDTO.getTransactions().forEach(transactionDTO -> {
                transactionDTO.setUser(null);
                transactionDTO.setSupplier(null);
            });
        }

        String message = messageSource.getMessage("user.transactions.success", null, LocaleContextHolder.getLocale());

        return Response.builder()
                .status(200)
                .message(message)
                .user(userDTO)
                .build();
    }

    private Organization resolveOrganizationForRegister(Long organizationId) {
        if (organizationId != null) {
            return organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new NotFoundException(
                            messageSource.getMessage("organization.not_found", null, LocaleContextHolder.getLocale())));
        }
        return getOrCreateDefaultOrganization();
    }

    private Organization getCurrentUserOrganization() {
        User currentUser = getCurrentLoggedInUser();
        return ensureUserOrganization(currentUser);
    }

    private Organization ensureUserOrganization(User user) {
        if (user.getOrganization() != null) {
            return user.getOrganization();
        }
        Organization defaultOrganization = getOrCreateDefaultOrganization();
        user.setOrganization(defaultOrganization);
        userRepository.save(user);
        return defaultOrganization;
    }

    private Organization getOrCreateDefaultOrganization() {
        return organizationRepository.findByRuc(DEFAULT_RUC)
                .orElseGet(() -> organizationRepository.save(Organization.builder()
                        .name(DEFAULT_NAME)
                        .ruc(DEFAULT_RUC)
                        .plan(OrganizationPlan.FREE)
                        .active(true)
                        .build()));
    }

    private void validateSameOrganization(User user) {
        Organization currentOrganization = getCurrentUserOrganization();
        Organization userOrganization = ensureUserOrganization(user);
        if (!currentOrganization.getId().equals(userOrganization.getId())) {
            throw new NotFoundException(
                    messageSource.getMessage("user.not_found", null, LocaleContextHolder.getLocale()));
        }
    }
}
