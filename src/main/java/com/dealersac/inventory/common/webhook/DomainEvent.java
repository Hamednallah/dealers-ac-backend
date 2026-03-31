package com.dealersac.inventory.common.webhook;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.Map;

/**
 * Base domain event published via Spring ApplicationEventPublisher.
 * Async listeners dispatch these as HTTP webhooks.
 */
@Getter
public class DomainEvent extends ApplicationEvent {

    private final String   eventType;
    private final String   tenantId;
    private final Instant  occurredAt;
    private final Map<String, Object> payload;

    public DomainEvent(Object source, String eventType, String tenantId,
                       Map<String, Object> payload) {
        super(source);
        this.eventType  = eventType;
        this.tenantId   = tenantId;
        this.occurredAt = Instant.now();
        this.payload    = payload;
    }
}
