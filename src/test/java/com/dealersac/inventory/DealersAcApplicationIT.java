package com.dealersac.inventory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Unit Tests — Application Context")
class DealersAcApplicationIT extends BaseIntegrationTest {

    @Test
    @DisplayName("Application Context — should load successfully")
    void contextLoads() {
        // This test ensures the Spring application context starts up without errors.
        // It uses the containers defined in BaseIntegrationTest.
    }
}
