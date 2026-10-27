package com.thoonidev.InventoryMgtSystem.services;

public interface ReportService {
    byte[] exportInventoryExcel(String stockFilter, String abcClass);

    byte[] exportTransactionsExcel(String dateFrom, String dateTo, String type, String status);

    byte[] exportInventoryPdf(String stockFilter, String abcClass);

    byte[] exportTransactionsPdf(String dateFrom, String dateTo, String type, String status);
}
