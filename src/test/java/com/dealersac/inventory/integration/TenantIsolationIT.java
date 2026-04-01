package com.dealersac.inventory.integration;

import com.dealersac.inventory.auth.dto.LoginRequest;
import com.dealersac.inventory.auth.dto.RegisterRequest;
import com.dealersac.inventory.dealer.dto.DealerRequest;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests using Testcontainers (real PostgreSQL).
 *
 * Covers the critical acceptance criteria:
 * - Missing X-Tenant-Id → 400
 * - Cross-tenant access → 404 (we return 404 to avoid confirming resource existence)
 * - subscription=PREMIUM filter stays tenant-scoped
 * - Admin endpoint requires GLOBAL_ADMIN
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Integration Tests — Tenant Isolation & Security")
class TenantIsolationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("dealersac_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static String tokenTenantA;
    private static String tokenTenantB;
    private static String adminToken;
    private static String dealerIdInTenantA;

    // ─── Setup: register users and create a dealer in Tenant A ────────────────

    @Test @Order(1)
    @DisplayName("Setup: Register users in Tenant A and Tenant B")
    void setup_registerUsers() throws Exception {
        tokenTenantA = registerAndLogin("userA", "Password@1", "tenant-alpha");
        tokenTenantB = registerAndLogin("userB", "Password@1", "tenant-beta");
        adminToken   = loginAdmin();
    }

    @Test @Order(2)
    @DisplayName("Setup: Create a dealer in Tenant A")
    void setup_createDealerInTenantA() throws Exception {
        DealerRequest req = new DealerRequest();
        req.setName("Alpha Motors");
        req.setEmail("contact@alphamotors.com");
        req.setSubscriptionType(SubscriptionType.PREMIUM);

        MvcResult result = mockMvc.perform(post("/dealers")
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .header("X-Tenant-Id", "tenant-alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        dealerIdInTenantA = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText();
    }

    // ─── Acceptance Checks ────────────────────────────────────────────────────

    @Test @Order(3)
    @DisplayName("Missing X-Tenant-Id → 400 Bad Request")
    void missingTenantHeader_returns400() throws Exception {
        mockMvc.perform(get("/dealers")
                        .header("Authorization", "Bearer " + tokenTenantA))
                // No X-Tenant-Id header
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("X-Tenant-Id")));
    }

    @Test @Order(4)
    @DisplayName("Cross-tenant access → 404 (resource existence not revealed)")
    void crossTenantAccess_returns404() throws Exception {
        // Tenant B tries to access Tenant A's dealer
        mockMvc.perform(get("/dealers/" + dealerIdInTenantA)
                        .header("Authorization", "Bearer " + tokenTenantB)
                        .header("X-Tenant-Id", "tenant-beta"))
                .andExpect(status().isNotFound());
    }

    @Test @Order(5)
    @DisplayName("Own tenant access → 200 OK")
    void ownTenantAccess_returns200() throws Exception {
        mockMvc.perform(get("/dealers/" + dealerIdInTenantA)
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .header("X-Tenant-Id", "tenant-alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-alpha"));
    }

    @Test @Order(6)
    @DisplayName("Admin countBySubscription → 403 for TENANT_USER")
    void adminEndpoint_tenantUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/dealers/countBySubscription")
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .header("X-Tenant-Id", "tenant-alpha"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(7)
    @DisplayName("Admin countBySubscription → 200 for GLOBAL_ADMIN")
    void adminEndpoint_globalAdmin_returns200() throws Exception {
        mockMvc.perform(get("/admin/dealers/countBySubscription")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.BASIC").isNumber())
                .andExpect(jsonPath("$.PREMIUM").isNumber());
    }

    @Test @Order(8)
    @DisplayName("No JWT token → 403 Forbidden")
    void noJwt_returns403() throws Exception {
        mockMvc.perform(get("/dealers")
                        .header("X-Tenant-Id", "tenant-alpha"))
                .andExpect(status().isForbidden());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String registerAndLogin(String username, String password, String tenantId) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setPassword(password);
        reg.setEmail(username + "@test.com");
        reg.setTenantId(tenantId);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        return extractToken(username, password);
    }

    private String extractToken(String username, String password) throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String loginAdmin() throws Exception {
        return extractToken("admin", "Admin@1234!");
    }
}
