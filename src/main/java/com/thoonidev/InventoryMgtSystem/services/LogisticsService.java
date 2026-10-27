package com.thoonidev.InventoryMgtSystem.services;

import com.thoonidev.InventoryMgtSystem.dtos.Response;

public interface LogisticsService {
    Response getKpis();

    Response getStockAlerts();

    Response getReorderSuggestions();

    Response getAbcAnalysis();
}
