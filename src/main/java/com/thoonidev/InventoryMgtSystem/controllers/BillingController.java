package com.thoonidev.InventoryMgtSystem.controllers;

import com.thoonidev.InventoryMgtSystem.dtos.PlanCheckoutRequest;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.services.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/checkout")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> createCheckout(@RequestBody @Valid PlanCheckoutRequest request) {
        return ResponseEntity.ok(billingService.createCheckout(request.getTargetPlan()));
    }

    @PostMapping("/payments/{paymentId}/confirm")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> confirmPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(billingService.confirmPayment(paymentId));
    }

    @PostMapping("/payments/{paymentId}/cancel")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> cancelPlanChange(@PathVariable Long paymentId) {
        return ResponseEntity.ok(billingService.cancelPlanChange(paymentId));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> getPayments() {
        return ResponseEntity.ok(Response.builder()
                .status(200)
                .message("Historial de pagos obtenido")
                .planPayments(billingService.getPaymentHistory())
                .build());
    }
}
