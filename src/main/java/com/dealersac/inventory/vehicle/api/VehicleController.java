package com.dealersac.inventory.vehicle.api;

import com.dealersac.inventory.common.tenant.TenantContext;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.vehicle.application.VehicleService;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import com.dealersac.inventory.vehicle.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Vehicle CRUD with filtering — requires X-Tenant-Id header")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a vehicle")
    public VehicleResponse create(@Valid @RequestBody VehicleRequest request) {
        return vehicleService.create(request, TenantContext.getTenantId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vehicle by ID")
    public VehicleResponse getById(@PathVariable UUID id) {
        return vehicleService.findById(id, TenantContext.getTenantId());
    }

    @GetMapping
    @Operation(summary = "List vehicles with optional filters",
               description = """
                   Supports filtering by: model (partial match), status, priceMin, priceMax, subscription.
                   `subscription=PREMIUM` returns only vehicles whose dealer has PREMIUM subscription,
                   still scoped to the caller's tenant.
                   """)
    public Page<VehicleResponse> getAll(
            @Parameter(description = "Partial model name match") @RequestParam(required = false) String model,
            @Parameter(description = "AVAILABLE or SOLD")        @RequestParam(required = false) VehicleStatus status,
            @Parameter(description = "Minimum price")            @RequestParam(required = false) BigDecimal priceMin,
            @Parameter(description = "Maximum price")            @RequestParam(required = false) BigDecimal priceMax,
            @Parameter(description = "Filter by dealer subscription: PREMIUM or BASIC")
                                                                 @RequestParam(required = false) SubscriptionType subscription,
            @PageableDefault(size = 20, sort = "model", direction = Sort.Direction.ASC) Pageable pageable) {

        VehicleFilter filter = VehicleFilter.builder()
                .tenantId(TenantContext.getTenantId())
                .model(model)
                .status(status)
                .priceMin(priceMin)
                .priceMax(priceMax)
                .subscription(subscription)
                .build();

        return vehicleService.findAll(filter, pageable);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a vehicle",
               description = "Setting status to SOLD fires a VEHICLE_SOLD event (email + webhook).")
    public VehicleResponse patch(@PathVariable UUID id,
                                 @Valid @RequestBody VehiclePatchRequest request) {
        return vehicleService.update(id, request, TenantContext.getTenantId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a vehicle")
    public void delete(@PathVariable UUID id) {
        vehicleService.delete(id, TenantContext.getTenantId());
    }
}
