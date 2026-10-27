package com.thoonidev.InventoryMgtSystem.services;

import com.thoonidev.InventoryMgtSystem.dtos.CustomerDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;

public interface CustomerService {
    Response addCustomer(CustomerDTO customerDTO);

    Response updateCustomer(Long id, CustomerDTO customerDTO);

    Response getAllCustomers();

    Response getCustomerById(Long id);

    Response deleteCustomer(Long id);
}
