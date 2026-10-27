package com.thoonidev.InventoryMgtSystem.controllers;

import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import com.thoonidev.InventoryMgtSystem.services.PlanLimitService;
import com.thoonidev.InventoryMgtSystem.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final OrganizationService organizationService;
    private final PlanLimitService planLimitService;

    @GetMapping("/inventory/excel")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<byte[]> exportInventoryExcel(
            @RequestParam(required = false) String stockFilter,
            @RequestParam(required = false) String abcClass
    ) {
        validateReportAccess();
        return downloadable(reportService.exportInventoryExcel(stockFilter, abcClass), "inventario.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/transactions/excel")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<byte[]> exportTransactionsExcel(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        validateReportAccess();
        return downloadable(reportService.exportTransactionsExcel(dateFrom, dateTo, type, status), "transacciones.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/inventory/pdf")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<byte[]> exportInventoryPdf(
            @RequestParam(required = false) String stockFilter,
            @RequestParam(required = false) String abcClass
    ) {
        validateReportAccess();
        return downloadable(reportService.exportInventoryPdf(stockFilter, abcClass), "inventario.pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    @GetMapping("/transactions/pdf")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<byte[]> exportTransactionsPdf(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        validateReportAccess();
        return downloadable(reportService.exportTransactionsPdf(dateFrom, dateTo, type, status), "transacciones.pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    private void validateReportAccess() {
        Organization organization = organizationService.getCurrentUserOrganization();
        planLimitService.validateReportsEnabled(organization);
    }

    private ResponseEntity<byte[]> downloadable(byte[] bytes, String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
