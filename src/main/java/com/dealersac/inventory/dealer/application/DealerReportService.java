package com.dealersac.inventory.dealer.application;

import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.repository.DealerRepository;
import com.dealersac.inventory.vehicle.domain.Vehicle;
import com.dealersac.inventory.vehicle.repository.VehicleRepository;
import com.dealersac.inventory.common.exception.ResourceNotFoundException;
import com.dealersac.inventory.common.exception.CrossTenantAccessException;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Generates a PDF inventory report for a given dealer using iText 7.
 *
 * Demonstrates: Document Processing, PDF Generation, File Handling (JD requirements)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DealerReportService {

    private final DealerRepository  dealerRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public void generateReport(UUID dealerId, String tenantId, OutputStream out) throws IOException {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer", dealerId));

        if (!dealer.getTenantId().equals(tenantId)) {
            throw new CrossTenantAccessException();
        }

        List<Vehicle> vehicles = vehicleRepository.findAllByDealerIdAndTenantId(dealerId, tenantId);

        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf  = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // ── Title ──────────────────────────────────────────────────────
            document.add(new Paragraph("Dealer Inventory Report")
                    .setFontSize(22)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            document.add(new Paragraph("Generated: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // ── Dealer Info ────────────────────────────────────────────────
            document.add(new Paragraph("Dealer Details").setBold().setFontSize(14));
            document.add(new Paragraph("Name: " + dealer.getName()));
            document.add(new Paragraph("Email: " + dealer.getEmail()));
            document.add(new Paragraph("Tenant: " + dealer.getTenantId()));
            document.add(new Paragraph("Subscription: " + dealer.getSubscriptionType()).setMarginBottom(15));

            // ── Vehicle Table ──────────────────────────────────────────────
            document.add(new Paragraph("Vehicles (" + vehicles.size() + ")")
                    .setBold().setFontSize(14).setMarginBottom(5));

            Table table = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Header row
            for (String header : new String[]{"Model", "Price ($)", "Status"}) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(header).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER));
            }

            // Data rows
            for (Vehicle vehicle : vehicles) {
                table.addCell(vehicle.getModel());
                table.addCell(new Cell().add(
                        new Paragraph(vehicle.getPrice().toPlainString()))
                        .setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(
                        new Paragraph(vehicle.getStatus().name()))
                        .setTextAlignment(TextAlignment.CENTER));
            }

            document.add(table);

            // ── Summary ────────────────────────────────────────────────────
            long available = vehicles.stream()
                    .filter(v -> v.getStatus().name().equals("AVAILABLE")).count();
            long sold = vehicles.size() - available;

            document.add(new Paragraph("\nSummary")
                    .setBold().setFontSize(12).setMarginTop(15));
            document.add(new Paragraph("Total Vehicles: " + vehicles.size()));
            document.add(new Paragraph("Available: " + available));
            document.add(new Paragraph("Sold: " + sold));
        }

        log.info("PDF report generated for dealer: {}", dealerId);
    }
}
