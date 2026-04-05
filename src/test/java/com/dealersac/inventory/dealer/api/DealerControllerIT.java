package com.dealersac.inventory.dealer.api;

import com.dealersac.inventory.BaseIntegrationTest;
import com.dealersac.inventory.common.tenant.TenantContext;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.dealer.dto.DealerRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Integration Tests — Dealer API")
class DealerControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "test-tenant-123";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @WithMockUser(roles = "TENANT_USER")
    @DisplayName("POST /dealers — create successfully")
    void create_createsDealer() throws Exception {
        var request = new DealerRequest();
        request.setName("Test Dealer");
        request.setEmail("test@dealer.com");
        request.setSubscriptionType(SubscriptionType.BASIC);

        mockMvc.perform(post("/dealers")
                .header("X-Tenant-Id", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Dealer"))
                .andExpect(jsonPath("$.email").value("test@dealer.com"));
    }

    @Test
    @WithMockUser(roles = "TENANT_USER")
    @DisplayName("GET /dealers — returns paged results")
    void getAll_returnsPagedDealers() throws Exception {
        mockMvc.perform(get("/dealers")
                .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(roles = "TENANT_USER")
    @DisplayName("GET /dealers/{id} — returns 404 for unknown id")
    void getById_returns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/dealers/" + UUID.randomUUID())
                .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isNotFound());
    }
}
