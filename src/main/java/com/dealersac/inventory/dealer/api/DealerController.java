package com.dealersac.inventory.dealer.api;

import com.dealersac.inventory.common.tenant.TenantContext;
import com.dealersac.inventory.dealer.application.DealerService;
import com.dealersac.inventory.dealer.application.DealerReportService;
import com.dealersac.inventory.dealer.dto.DealerPatchRequest;
import com.dealersac.inventory.dealer.dto.DealerRequest;
import com.dealersac.inventory.dealer.dto.DealerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/dealers")
@RequiredArgsConstructor
@Tag(name = "Dealers", description = "Dealer CRUD — requires X-Tenant-Id header")
public class DealerController {

    private final DealerService       dealerService;
    private final DealerReportService reportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a dealer")
    public DealerResponse create(@Valid @RequestBody DealerRequest request) {
        return dealerService.create(request, TenantContext.getTenantId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dealer by ID")
    public DealerResponse getById(
            @Parameter(description = "Dealer UUID") @PathVariable UUID id) {
        return dealerService.findById(id, TenantContext.getTenantId());
    }

    @GetMapping
    @Operation(summary = "List all dealers (paged + sorted)")
    public Page<DealerResponse> getAll(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return dealerService.findAll(TenantContext.getTenantId(), pageable);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a dealer")
    public DealerResponse patch(
            @PathVariable UUID id,
            @Valid @RequestBody DealerPatchRequest request) {
        return dealerService.update(id, request, TenantContext.getTenantId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a dealer")
    public void delete(@PathVariable UUID id) {
        dealerService.delete(id, TenantContext.getTenantId());
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "Download PDF inventory report for a dealer",
               description = "Returns a PDF listing all vehicles for this dealer.")
    public void getReport(@PathVariable UUID id,
                          HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"dealer-" + id + "-report.pdf\"");
        reportService.generateReport(id, TenantContext.getTenantId(), response.getOutputStream());
    }
}
