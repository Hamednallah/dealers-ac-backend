package com.dealersac.inventory.vehicle.api;

import com.dealersac.inventory.BaseIntegrationTest;
import com.dealersac.inventory.common.tenant.TenantContext;
import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.dealer.repository.DealerRepository;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import com.dealersac.inventory.vehicle.dto.VehicleRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Integration Tests — Vehicle API")
class VehicleControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DealerRepository dealerRepository;

    private static final String TENANT_ID = "test-tenant-456";
    private Dealer testDealer;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
        testDealer = Dealer.builder()
                .name("Test Dealer")
                .email("vehicle-test@dealer.com")
                .tenantId(TENANT_ID)
                .subscriptionType(SubscriptionType.BASIC)
                .build();
        testDealer = dealerRepository.save(testDealer);
    }

    @AfterEach
    void tearDown() {
        dealerRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    @WithMockUser(roles = "TENANT_USER")
    @DisplayName("POST /vehicles — create successfully")
    void create_createsVehicle() throws Exception {
        var request = new VehicleRequest();
        request.setDealerId(testDealer.getId());
        request.setModel("Tesla Model 3");
        request.setPrice(new BigDecimal("45000.00"));
        request.setStatus(VehicleStatus.AVAILABLE);

        mockMvc.perform(post("/vehicles")
                .header("X-Tenant-Id", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.model").value("Tesla Model 3"))
                .andExpect(jsonPath("$.price").value(45000.00));
    }

    @Test
    @WithMockUser(roles = "TENANT_USER")
    @DisplayName("GET /vehicles — returns paged results with filters")
    void getAll_returnsPagedVehicles() throws Exception {
        mockMvc.perform(get("/vehicles")
                .header("X-Tenant-Id", TENANT_ID)
                .param("model", "Tesla")
                .param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(roles = "TENANT_USER")
    @DisplayName("POST /vehicles/{id}/checkout — reserves vehicle")
    void checkout_reservesVehicle() throws Exception {
        // Create a vehicle first
        var request = new VehicleRequest();
        request.setDealerId(testDealer.getId());
        request.setModel("Tesla Model S");
        request.setPrice(new BigDecimal("90000.00"));
        request.setStatus(VehicleStatus.AVAILABLE);

        var result = mockMvc.perform(post("/vehicles")
                .header("X-Tenant-Id", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String idString = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/vehicles/" + idString + "/checkout")
                .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk());
    }
}
