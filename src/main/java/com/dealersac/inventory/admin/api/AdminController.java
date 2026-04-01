package com.dealersac.inventory.admin.api;

import com.dealersac.inventory.admin.application.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Global admin endpoints — GLOBAL_ADMIN role required")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dealers/countBySubscription")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    @Operation(
        summary = "Count dealers by subscription type (global)",
        description = """
            Returns the count of dealers grouped by subscription type across ALL tenants.
            
            **Note:** This count is GLOBAL — it aggregates across all tenants, not per-tenant.
            Only GLOBAL_ADMIN can access this endpoint.
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "Counts returned successfully"),
            @ApiResponse(responseCode = "403", description = "Requires GLOBAL_ADMIN role")
        }
    )
    public Map<String, Long> countBySubscription() {
        return adminService.countDealersBySubscription();
    }
}
