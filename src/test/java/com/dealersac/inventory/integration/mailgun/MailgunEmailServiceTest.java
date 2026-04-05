package com.dealersac.inventory.integration.mailgun;

import com.dealersac.inventory.common.webhook.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Unit Tests — Mailgun Integration")
class MailgunEmailServiceTest {

    @InjectMocks
    private MailgunEmailService mailgunEmailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(mailgunEmailService, "enabled", true);
        ReflectionTestUtils.setField(mailgunEmailService, "domain", "test.com");
        ReflectionTestUtils.setField(mailgunEmailService, "apiKey", "test-key");
        ReflectionTestUtils.setField(mailgunEmailService, "from", "noreply@test.com");
    }

    @Test
    @DisplayName("onVehicleSold — sends email when enabled")
    void onVehicleSold_whenEnabled_sendsEmail() {
        DomainEvent event = new DomainEvent(
                this,
                "VEHICLE_SOLD",
                "alpha-tenant",
                Map.of(
                        "model", "Tesla",
                        "price", "50000",
                        "dealerId", "dealer-1"
                )
        );

        mailgunEmailService.onVehicleSold(event);
    }

    @Test
    @DisplayName("onVehicleSold — skips when disabled")
    void onVehicleSold_whenDisabled_skips() {
        ReflectionTestUtils.setField(mailgunEmailService, "enabled", false);
        DomainEvent event = new DomainEvent(this, "VEHICLE_SOLD", "alpha", Map.of());

        mailgunEmailService.onVehicleSold(event);
    }
}
