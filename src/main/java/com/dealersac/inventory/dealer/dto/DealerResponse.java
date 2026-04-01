package com.dealersac.inventory.dealer.dto;

import com.dealersac.inventory.dealer.domain.Dealer;
import com.dealersac.inventory.dealer.domain.SubscriptionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Dealer response")
public class DealerResponse {

    private UUID             id;
    private String           tenantId;
    private String           name;
    private String           email;
    private SubscriptionType subscriptionType;
    private LocalDateTime    createdAt;
    private LocalDateTime    updatedAt;

    public static DealerResponse from(Dealer d) {
        return DealerResponse.builder()
                .id(d.getId())
                .tenantId(d.getTenantId())
                .name(d.getName())
                .email(d.getEmail())
                .subscriptionType(d.getSubscriptionType())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
