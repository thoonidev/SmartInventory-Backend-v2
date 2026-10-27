package com.thoonidev.InventoryMgtSystem.services.impl;

import com.thoonidev.InventoryMgtSystem.dtos.CustomerDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.exceptions.NotFoundException;
import com.thoonidev.InventoryMgtSystem.models.Customer;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.repositories.CustomerRepository;
import com.thoonidev.InventoryMgtSystem.services.AuditService;
import com.thoonidev.InventoryMgtSystem.services.CustomerService;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import com.thoonidev.InventoryMgtSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final AuditService auditService;
    private final ModelMapper modelMapper;
    private final MessageSource messageSource;

    @Override
    public Response addCustomer(CustomerDTO customerDTO) {
        Organization organization = organizationService.getCurrentUserOrganization();
        Customer customer = Customer.builder()
                .name(customerDTO.getName())
                .documentNumber(customerDTO.getDocumentNumber())
                .contactInfo(customerDTO.getContactInfo())
                .address(customerDTO.getAddress())
                .customerStatus(customerDTO.getCustomerStatus() == null ? "ACTIVE" : customerDTO.getCustomerStatus())
                .organization(organization)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        auditService.log("CREATE", "Customer", savedCustomer.getId(), savedCustomer.getName(), organization, userService.getCurrentLoggedInUser());

        return Response.builder()
                .status(200)
                .message(getMessage("customer.saved"))
                .build();
    }

    @Override
    public Response updateCustomer(Long id, CustomerDTO customerDTO) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(getMessage("customer.not_found")));
        validateCustomerAccess(customer);

        if (customerDTO.getName() != null && !customerDTO.getName().isBlank()) customer.setName(customerDTO.getName());
        if (customerDTO.getDocumentNumber() != null) customer.setDocumentNumber(customerDTO.getDocumentNumber());
        if (customerDTO.getContactInfo() != null && !customerDTO.getContactInfo().isBlank()) customer.setContactInfo(customerDTO.getContactInfo());
        if (customerDTO.getAddress() != null) customer.setAddress(customerDTO.getAddress());
        if (customerDTO.getCustomerStatus() != null && !customerDTO.getCustomerStatus().isBlank()) customer.setCustomerStatus(customerDTO.getCustomerStatus());

        Customer savedCustomer = customerRepository.save(customer);
        auditService.log("UPDATE", "Customer", savedCustomer.getId(), savedCustomer.getName(), organizationService.getCurrentUserOrganization(), userService.getCurrentLoggedInUser());

        return Response.builder()
                .status(200)
                .message(getMessage("customer.updated"))
                .build();
    }

    @Override
    public Response getAllCustomers() {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<CustomerDTO> customers = customerRepository.findByOrganizationIdOrOrganizationIsNull(
                        organization.getId(),
                        Sort.by(Sort.Direction.DESC, "id")
                ).stream()
                .map(customer -> modelMapper.map(customer, CustomerDTO.class))
                .toList();

        return Response.builder()
                .status(200)
                .message(getMessage("customer.list"))
                .customers(customers)
                .build();
    }

    @Override
    public Response getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(getMessage("customer.not_found")));
        validateCustomerAccess(customer);

        return Response.builder()
                .status(200)
                .message(getMessage("customer.found"))
                .customer(modelMapper.map(customer, CustomerDTO.class))
                .build();
    }

    @Override
    public Response deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(getMessage("customer.not_found")));
        validateCustomerAccess(customer);

        customer.setCustomerStatus("INACTIVE");
        customerRepository.save(customer);
        auditService.log("ARCHIVE", "Customer", customer.getId(), customer.getName(), organizationService.getCurrentUserOrganization(), userService.getCurrentLoggedInUser());

        return Response.builder()
                .status(200)
                .message(getMessage("customer.archived"))
                .build();
    }

    private void validateCustomerAccess(Customer customer) {
        Organization organization = organizationService.getCurrentUserOrganization();
        if (customer.getOrganization() == null) {
            customer.setOrganization(organization);
            customerRepository.save(customer);
            return;
        }
        if (!organization.getId().equals(customer.getOrganization().getId())) {
            throw new NotFoundException(getMessage("customer.not_found"));
        }
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
