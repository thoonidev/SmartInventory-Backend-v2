package com.thoonidev.InventoryMgtSystem.controllers;

import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.dtos.TransactionRequest;
import com.thoonidev.InventoryMgtSystem.enums.TransactionStatus;
import com.thoonidev.InventoryMgtSystem.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {


    private final TransactionService transactionService;

    @PostMapping("/purchase")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Response> purchaseInventory(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.purchase(transactionRequest));
    }

    @PostMapping("/sell")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Response> makeSale(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.sell(transactionRequest));
    }

    @PostMapping("/return")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Response> returnToSupplier(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.returnToSupplier(transactionRequest));
    }

    @PostMapping("/customer-return")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Response> returnFromCustomer(@RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.returnFromCustomer(transactionRequest));
    }

    @GetMapping("/all")
    public ResponseEntity<Response> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size,
            @RequestParam(required = false) String filter) {

        return ResponseEntity.ok(transactionService.getAllTransactions(page, size, filter));
    }


    @GetMapping("/{id}")
    public ResponseEntity<Response> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getAllTransactionById(id));
    }

    @GetMapping("/by-month-year")
    public ResponseEntity<Response> getTransactionByMonthAndYear(
            @RequestParam int month,
            @RequestParam int year) {

        return ResponseEntity.ok(transactionService.getAllTransactionByMonthAndYear(month, year));
    }

    @PutMapping({"/{transactionId}", "/status/{transactionId}"})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Response> updateTransactionStatus(
            @PathVariable Long transactionId,
            @RequestBody TransactionStatus status) {

        return ResponseEntity.ok(transactionService.updateTransactionStatus(transactionId, status));
    }


}

