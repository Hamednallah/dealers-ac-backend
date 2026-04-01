package com.dealersac.inventory.integration.mailgun;

import com.dealersac.inventory.common.webhook.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Sends transactional emails via Mailgun REST API when a vehicle is sold.
 *
 * Demonstrates: Mailgun (Email APIs), 3rd-party API Integration (JD requirements)
 *
 * Configuration (via env vars / application.yml):
 *   MAILGUN_API_KEY, MAILGUN_DOMAIN, MAILGUN_FROM, MAILGUN_ENABLED
 */
@Slf4j
@Component
public class MailgunEmailService {

    @Value("${app.mailgun.api-key:}")
    private String apiKey;

    @Value("${app.mailgun.domain:}")
    private String domain;

    @Value("${app.mailgun.from:noreply@dealersac.com}")
    private String from;

    @Value("${app.mailgun.enabled:false}")
    private boolean enabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Async("asyncExecutor")
    @EventListener(condition = "#event.eventType == 'VEHICLE_SOLD'")
    public void onVehicleSold(DomainEvent event) {
        if (!enabled) {
            log.debug("Mailgun disabled — skipping email for VEHICLE_SOLD event");
            return;
        }

        String model    = (String) event.getPayload().get("model");
        String dealerId = (String) event.getPayload().get("dealerId");
        String price    = (String) event.getPayload().get("price");

        String subject = "Vehicle Sold: " + model;
        String body    = String.format("""
                A vehicle has been sold in your inventory.
                
                Vehicle:  %s
                Price:    $%s
                Dealer:   %s
                Tenant:   %s
                
                This is an automated notification from Dealers AC.
                """, model, price, dealerId, event.getTenantId());

        sendEmail("dealer-notifications@" + domain, subject, body);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            String credentials = Base64.getEncoder()
                    .encodeToString(("api:" + apiKey).getBytes(StandardCharsets.UTF_8));

            String formBody = "from=" + encode(from)
                    + "&to=" + encode(to)
                    + "&subject=" + encode(subject)
                    + "&text=" + encode(text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mailgun.net/v3/" + domain + "/messages"))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Mailgun email sent successfully to: {}", to);
            } else {
                log.warn("Mailgun returned: {} — Body: {}", response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to send Mailgun email: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
