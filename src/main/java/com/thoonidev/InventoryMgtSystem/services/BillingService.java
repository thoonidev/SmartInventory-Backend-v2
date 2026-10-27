package com.thoonidev.InventoryMgtSystem.services;

import com.thoonidev.InventoryMgtSystem.dtos.PlanPaymentDTO;
import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.enums.OrganizationPlan;

import java.util.List;

public interface BillingService {
    Response createCheckout(OrganizationPlan targetPlan);
    Response confirmPayment(Long paymentId);
    Response cancelPlanChange(Long paymentId);
    List<PlanPaymentDTO> getPaymentHistory();
}
