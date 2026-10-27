package com.thoonidev.InventoryMgtSystem.services.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.thoonidev.InventoryMgtSystem.models.Organization;
import com.thoonidev.InventoryMgtSystem.models.Product;
import com.thoonidev.InventoryMgtSystem.models.Transaction;
import com.thoonidev.InventoryMgtSystem.enums.TransactionStatus;
import com.thoonidev.InventoryMgtSystem.enums.TransactionType;
import com.thoonidev.InventoryMgtSystem.repositories.ProductRepository;
import com.thoonidev.InventoryMgtSystem.repositories.TransactionRepository;
import com.thoonidev.InventoryMgtSystem.services.OrganizationService;
import com.thoonidev.InventoryMgtSystem.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color HEADER_COLOR = new Color(15, 118, 110);
    private static final Color LIGHT_HEADER_COLOR = new Color(236, 253, 245);

    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final OrganizationService organizationService;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportInventoryExcel(String stockFilter, String abcClass) {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Product> products = filterProducts(productRepository.findByOrganizationId(
                organization.getId(),
                Sort.by(Sort.Direction.ASC, "name")
        ), stockFilter, abcClass);

        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("SKU", "Producto", "Categoria", "Stock", "Precio", "Valor inventario", "ABC", "Punto reorden", "Ubicacion"));
        products.forEach(product -> rows.add(List.of(
                nullSafe(product.getSku()),
                nullSafe(product.getName()),
                product.getCategory() == null ? "" : nullSafe(product.getCategory().getName()),
                product.getStockQuantity() == null ? 0 : product.getStockQuantity(),
                money(product.getPrice()),
                money(calculateInventoryValue(product)),
                nullSafe(product.getAbcClass()),
                product.getReorderPoint() == null ? 0 : product.getReorderPoint(),
                nullSafe(product.getWarehouseLocation())
        )));

        BigDecimal totalValue = products.stream()
                .map(this::calculateInventoryValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return createXlsx("Inventario", organization, "Reporte de inventario", rows,
                List.of("Productos", products.size(), "Valor total", money(totalValue)));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTransactionsExcel(String dateFrom, String dateTo, String type, String status) {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Transaction> transactions = filterTransactions(transactionRepository.findByOrganizationId(
                organization.getId(),
                Sort.by(Sort.Direction.DESC, "id")
        ), dateFrom, dateTo, type, status);

        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("Documento", "Tipo", "Estado", "Producto", "Proveedor", "Cantidad", "Total", "Fecha", "Descripcion", "Nota"));
        transactions.forEach(transaction -> rows.add(List.of(
                "TRX-" + String.format("%05d", transaction.getId()),
                transaction.getTransactionType() == null ? "" : transaction.getTransactionType().name(),
                transaction.getStatus() == null ? "" : transaction.getStatus().name(),
                transaction.getProduct() == null ? "" : nullSafe(transaction.getProduct().getName()),
                transaction.getSupplier() == null ? "" : nullSafe(transaction.getSupplier().getName()),
                transaction.getTotalProducts() == null ? 0 : transaction.getTotalProducts(),
                money(transaction.getTotalPrice()),
                formatDate(transaction.getCreatedAt()),
                nullSafe(transaction.getDescription()),
                nullSafe(transaction.getNote())
        )));

        BigDecimal totalAmount = transactions.stream()
                .map(transaction -> transaction.getTotalPrice() == null ? BigDecimal.ZERO : transaction.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return createXlsx("Transacciones", organization, "Reporte de transacciones", rows,
                List.of("Transacciones", transactions.size(), "Importe total", money(totalAmount)));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportInventoryPdf(String stockFilter, String abcClass) {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Product> products = filterProducts(productRepository.findByOrganizationId(
                organization.getId(),
                Sort.by(Sort.Direction.ASC, "name")
        ), stockFilter, abcClass);

        BigDecimal totalValue = products.stream()
                .map(this::calculateInventoryValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return createPdf(
                "Reporte de inventario",
                organization,
                List.of("Productos", String.valueOf(products.size()), "Valor total", "S/ " + money(totalValue)),
                new String[]{"SKU", "Producto", "Stock", "Precio", "Valor", "ABC", "Ubicacion"},
                products.stream()
                        .map(product -> new String[]{
                                nullSafe(product.getSku()),
                                nullSafe(product.getName()),
                                String.valueOf(product.getStockQuantity() == null ? 0 : product.getStockQuantity()),
                                "S/ " + money(product.getPrice()),
                                "S/ " + money(calculateInventoryValue(product)),
                                nullSafe(product.getAbcClass()),
                                nullSafe(product.getWarehouseLocation())
                        })
                        .toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTransactionsPdf(String dateFrom, String dateTo, String type, String status) {
        Organization organization = organizationService.getCurrentUserOrganization();
        List<Transaction> transactions = filterTransactions(transactionRepository.findByOrganizationId(
                organization.getId(),
                Sort.by(Sort.Direction.DESC, "id")
        ), dateFrom, dateTo, type, status);

        BigDecimal totalAmount = transactions.stream()
                .map(transaction -> transaction.getTotalPrice() == null ? BigDecimal.ZERO : transaction.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return createPdf(
                "Reporte de transacciones",
                organization,
                List.of("Registros", String.valueOf(transactions.size()), "Importe total", "S/ " + money(totalAmount)),
                new String[]{"Documento", "Tipo", "Estado", "Producto", "Cantidad", "Total", "Fecha"},
                transactions.stream()
                        .map(transaction -> new String[]{
                                "TRX-" + String.format("%05d", transaction.getId()),
                                transaction.getTransactionType() == null ? "" : transaction.getTransactionType().name(),
                                transaction.getStatus() == null ? "" : transaction.getStatus().name(),
                                transaction.getProduct() == null ? "" : nullSafe(transaction.getProduct().getName()),
                                String.valueOf(transaction.getTotalProducts() == null ? 0 : transaction.getTotalProducts()),
                                "S/ " + money(transaction.getTotalPrice()),
                                formatDate(transaction.getCreatedAt())
                        })
                        .toList()
        );
    }

    private List<Product> filterProducts(List<Product> products, String stockFilter, String abcClass) {
        return products.stream()
                .filter(product -> {
                    String stock = stockFilter == null ? "ALL" : stockFilter.toUpperCase();
                    int currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
                    int reorderPoint = product.getReorderPoint() == null ? -1 : product.getReorderPoint();
                    boolean stockMatches = switch (stock) {
                        case "LOW" -> reorderPoint >= 0 && currentStock <= reorderPoint && currentStock > 0;
                        case "OUT" -> currentStock <= 0;
                        case "OK" -> reorderPoint < 0 || currentStock > reorderPoint;
                        default -> true;
                    };
                    boolean abcMatches = abcClass == null || abcClass.isBlank() || "ALL".equalsIgnoreCase(abcClass)
                            || abcClass.equalsIgnoreCase(product.getAbcClass());
                    return stockMatches && abcMatches;
                })
                .toList();
    }

    private List<Transaction> filterTransactions(
            List<Transaction> transactions,
            String dateFrom,
            String dateTo,
            String type,
            String status
    ) {
        LocalDate from = parseDate(dateFrom);
        LocalDate to = parseDate(dateTo);
        TransactionType transactionType = parseTransactionType(type);
        TransactionStatus transactionStatus = parseTransactionStatus(status);

        return transactions.stream()
                .filter(transaction -> {
                    LocalDate createdDate = transaction.getCreatedAt() == null
                            ? null
                            : transaction.getCreatedAt().toLocalDate();
                    boolean dateMatches = (from == null || (createdDate != null && !createdDate.isBefore(from)))
                            && (to == null || (createdDate != null && !createdDate.isAfter(to)));
                    boolean typeMatches = transactionType == null || transactionType.equals(transaction.getTransactionType());
                    boolean statusMatches = transactionStatus == null || transactionStatus.equals(transaction.getStatus());
                    return dateMatches && typeMatches && statusMatches;
                })
                .toList();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    private TransactionType parseTransactionType(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) return null;
        return TransactionType.valueOf(value);
    }

    private TransactionStatus parseTransactionStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) return null;
        return TransactionStatus.valueOf(value);
    }

    private byte[] createXlsx(String sheetName, Organization organization, String title, List<List<Object>> rows, List<Object> summary) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(outputStream)) {

            put(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                      <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                    </Types>
                    """);
            put(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
            put(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                    </Relationships>
                    """);
            put(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets><sheet name="%s" sheetId="1" r:id="rId1"/></sheets>
                    </workbook>
                    """.formatted(xml(sheetName)));
            put(zip, "xl/styles.xml", stylesXml());
            put(zip, "xl/worksheets/sheet1.xml", sheetXml(organization, title, rows, summary));
            zip.finish();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el archivo Excel.", exception);
        }
    }

    private String sheetXml(Organization organization, String title, List<List<Object>> rows, List<Object> summary) {
        StringBuilder xml = new StringBuilder();
        int lastColumn = rows.stream().mapToInt(List::size).max().orElse(1);
        String lastColumnName = columnName(lastColumn);

        xml.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <cols>
                    <col min="1" max="1" width="18" customWidth="1"/>
                    <col min="2" max="2" width="32" customWidth="1"/>
                    <col min="3" max="3" width="20" customWidth="1"/>
                    <col min="4" max="10" width="16" customWidth="1"/>
                  </cols>
                  <sheetData>
                """);
        int rowNumber = 1;
        xml.append(row(rowNumber++, 1, List.of(title), 1));
        xml.append(row(rowNumber++, 1, List.of("Empresa", organization.getName(), "RUC", organization.getRuc()), 0));
        xml.append(row(rowNumber++, 1, List.of("Generado", formatDate(LocalDateTime.now())), 0));
        xml.append(row(rowNumber++, 1, summary, 2));
        rowNumber++;
        xml.append(row(rowNumber++, 1, rows.get(0), 3));
        for (int i = 1; i < rows.size(); i++) {
            xml.append(row(rowNumber++, 1, rows.get(i), 0));
        }
        xml.append("""
                  </sheetData>
                  <autoFilter ref="A6:%s%s"/>
                </worksheet>
                """.formatted(lastColumnName, Math.max(rowNumber - 1, 6)));
        return xml.toString();
    }

    private String row(int rowNumber, int startColumn, List<Object> values, int style) {
        StringBuilder builder = new StringBuilder("<row r=\"").append(rowNumber).append("\">");
        for (int i = 0; i < values.size(); i++) {
            builder.append(cell(rowNumber, startColumn + i, values.get(i), style));
        }
        builder.append("</row>");
        return builder.toString();
    }

    private String cell(int rowNumber, int column, Object value, int style) {
        String ref = columnName(column) + rowNumber;
        String styleAttribute = style > 0 ? " s=\"" + style + "\"" : "";
        if (value instanceof Number number) {
            return "<c r=\"" + ref + "\"" + styleAttribute + "><v>" + number + "</v></c>";
        }
        return "<c r=\"" + ref + "\" t=\"inlineStr\"" + styleAttribute + "><is><t>" + xml(String.valueOf(value == null ? "" : value)) + "</t></is></c>";
    }

    private String stylesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="2">
                    <font><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><sz val="14"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
                  </fonts>
                  <fills count="3">
                    <fill><patternFill patternType="none"/></fill>
                    <fill><patternFill patternType="gray125"/></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="FF0F766E"/><bgColor indexed="64"/></patternFill></fill>
                  </fills>
                  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="4">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
                  </cellXfs>
                </styleSheet>
                """;
    }

    private byte[] createPdf(String title, Organization organization, List<String> summary, String[] headers, List<String[]> rows) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, HEADER_COLOR);
            Font labelFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY);
            Font bodyFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);

            Paragraph heading = new Paragraph(title, titleFont);
            heading.setSpacingAfter(6);
            document.add(heading);
            document.add(new Paragraph("Empresa: " + organization.getName() + " | RUC: " + organization.getRuc(), bodyFont));
            document.add(new Paragraph("Generado: " + formatDate(LocalDateTime.now()), bodyFont));
            document.add(new Paragraph(" "));

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            for (int i = 0; i < summary.size(); i += 2) {
                summaryCell(summaryTable, summary.get(i), labelFont);
                summaryCell(summaryTable, summary.get(i + 1), bodyFont);
            }
            summaryTable.setSpacingAfter(12);
            document.add(summaryTable);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, labelFont));
                cell.setBackgroundColor(LIGHT_HEADER_COLOR);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                cell.setPadding(5);
                table.addCell(cell);
            }
            for (String[] row : rows) {
                for (String value : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(value, bodyFont));
                    cell.setPadding(4);
                    table.addCell(cell);
                }
            }
            document.add(table);
            document.close();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el PDF.", exception);
        }
    }

    private void summaryCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(6);
        cell.setBorderColor(new Color(220, 220, 220));
        table.addCell(cell);
    }

    private void put(ZipOutputStream zip, String path, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private BigDecimal calculateInventoryValue(Product product) {
        BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        int stock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        return price.multiply(BigDecimal.valueOf(stock));
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ');
    }

    private String xml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String columnName(int column) {
        StringBuilder name = new StringBuilder();
        while (column > 0) {
            int remainder = (column - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            column = (column - 1) / 26;
        }
        return name.toString();
    }
}
