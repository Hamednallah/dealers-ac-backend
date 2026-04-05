package com.dealersac.inventory.integration;

import com.dealersac.inventory.BaseIntegrationTest;
import com.dealersac.inventory.auth.dto.LoginRequest;
import com.dealersac.inventory.auth.dto.RegisterRequest;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import com.dealersac.inventory.dealer.dto.DealerRequest;
import com.dealersac.inventory.vehicle.domain.VehicleStatus;
import com.dealersac.inventory.vehicle.dto.VehicleRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Integration Tests — Vehicle Controller")
class VehicleControllerIT extends BaseIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static String tokenTenantA;
    private static String tokenTenantB;
    private static UUID dealerIdA;
    private static UUID vehicleIdA;

    @Test @Order(1)
    @DisplayName("Setup: Initial data")
    void setup_initialData() throws Exception {
        tokenTenantA = registerAndLogin("v_userA", "Password@1", "tenant-alpha");
        tokenTenantB = registerAndLogin("v_userB", "Password@1", "tenant-beta");

        // Create dealer in A
        DealerRequest dReq = new DealerRequest();
        dReq.setName("V-Dealer A");
        dReq.setEmail("v@dealer.com");
        dReq.setSubscriptionType(SubscriptionType.BASIC);

        MvcResult dRes = mockMvc.perform(post("/dealers")
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .header("X-Tenant-Id", "tenant-alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dReq)))
                .andExpect(status().isCreated())
                .andReturn();
        dealerIdA = UUID.fromString(objectMapper.readTree(dRes.getResponse().getContentAsString()).get("id").asText());

        // Create vehicle in A
        VehicleRequest vReq = new VehicleRequest();
        vReq.setModel("Model X");
        vReq.setPrice(BigDecimal.valueOf(50000));
        vReq.setStatus(VehicleStatus.AVAILABLE);
        vReq.setDealerId(dealerIdA);

        MvcResult vRes = mockMvc.perform(post("/vehicles")
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .header("X-Tenant-Id", "tenant-alpha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vReq)))
                .andExpect(status().isCreated())
                .andReturn();
        vehicleIdA = UUID.fromString(objectMapper.readTree(vRes.getResponse().getContentAsString()).get("id").asText());
    }

    @Test @Order(2)
    @DisplayName("Get Vehicle — Tenant A sees its own vehicle")
    void getVehicle_ownTenant_returns200() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleIdA)
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .header("X-Tenant-Id", "tenant-alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Model X"));
    }

    @Test @Order(3)
    @DisplayName("Get Vehicle — Tenant B is forbidden from seeing Tenant A's vehicle")
    void getVehicle_otherTenant_returns403() throws Exception {
        mockMvc.perform(get("/vehicles/" + vehicleIdA)
                        .header("Authorization", "Bearer " + tokenTenantB)
                        .header("X-Tenant-Id", "tenant-beta"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(4)
    @DisplayName("Filter Vehicles — by Price")
    void filterVehicles_byPrice_returnsFilteredResults() throws Exception {
        mockMvc.perform(get("/vehicles")
                        .param("priceMin", "40000")
                        .param("priceMax", "60000")
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .header("X-Tenant-Id", "tenant-alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

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

        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}

