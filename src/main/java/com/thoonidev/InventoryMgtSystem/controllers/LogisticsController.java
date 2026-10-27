package com.thoonidev.InventoryMgtSystem.controllers;

import com.thoonidev.InventoryMgtSystem.dtos.Response;
import com.thoonidev.InventoryMgtSystem.services.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;

    @GetMapping("/kpi")
    public ResponseEntity<Response> getKpis() {
        return ResponseEntity.ok(logisticsService.getKpis());
    }

    @GetMapping("/alerts")
    public ResponseEntity<Response> getStockAlerts() {
        return ResponseEntity.ok(logisticsService.getStockAlerts());
    }

    @GetMapping("/reorder-suggestions")
    public ResponseEntity<Response> getReorderSuggestions() {
        return ResponseEntity.ok(logisticsService.getReorderSuggestions());
    }

    @GetMapping("/abc-analysis")
    public ResponseEntity<Response> getAbcAnalysis() {
        return ResponseEntity.ok(logisticsService.getAbcAnalysis());
    }
}
