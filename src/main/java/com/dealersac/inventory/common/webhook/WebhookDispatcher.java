package com.dealersac.inventory.common.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Async webhook dispatcher.
 *
 * Listens to all DomainEvents published within the application
 * and forwards them as HTTP POST payloads to a configured webhook URL.
 *
 * Retry: 3 attempts with exponential backoff (1s, 2s, 4s).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher {

    @Value("${app.webhook.url:}")
    private String webhookUrl;

    @Value("${app.webhook.enabled:false}")
    private boolean webhookEnabled;

    private final ObjectMapper objectMapper;
    private final HttpClient   httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Async("asyncExecutor")
    @EventListener
    public void onDomainEvent(DomainEvent event) {
        if (!webhookEnabled || webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Webhook disabled or not configured — skipping event: {}", event.getEventType());
            return;
        }

        Map<String, Object> body = Map.of(
                "eventType",  event.getEventType(),
                "tenantId",   event.getTenantId(),
                "occurredAt", event.getOccurredAt().toString(),
                "payload",    event.getPayload()
        );

        dispatch(body, 0);
    }

    private void dispatch(Map<String, Object> body, int attempt) {
        if (attempt >= 3) {
            log.error("Webhook delivery failed after 3 attempts to: {}", webhookUrl);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<Void> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Webhook delivered successfully: {}", body.get("eventType"));
            } else {
                log.warn("Webhook returned {}, retrying (attempt {})", response.statusCode(), attempt + 1);
                Thread.sleep(1000L * (long) Math.pow(2, attempt));
                dispatch(body, attempt + 1);
            }
        } catch (Exception e) {
            log.warn("Webhook dispatch error (attempt {}): {}", attempt + 1, e.getMessage());
            try {
                Thread.sleep(1000L * (long) Math.pow(2, attempt));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            dispatch(body, attempt + 1);
        }
    }
}
