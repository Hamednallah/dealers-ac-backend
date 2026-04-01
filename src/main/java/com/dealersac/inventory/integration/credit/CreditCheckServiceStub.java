package com.dealersac.inventory.integration.credit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock 700Credit implementation for the take-home task.
 * Returns a synthetic credit score.
 *
 * Real implementation steps:
 * 1. Add 700Credit API key to environment (CREDIT_API_KEY)
 * 2. POST to https://api.700credit.com/v2/creditcheck with dealer details
 * 3. Parse JSON response: { "score": 720, "decision": "APPROVE" }
 * 4. Map to CreditCheckResult and return
 */
@Slf4j
@Service
public class CreditCheckServiceStub implements CreditCheckService {

    @Override
    public CreditCheckResult checkCredit(String dealerId, String tenantId) {
        log.info("[700CREDIT STUB] Credit check for dealer: {} in tenant: {}", dealerId, tenantId);

        // Simulated response — replace with real HTTP call in production
        return CreditCheckResult.builder()
                .dealerId(dealerId)
                .creditScore(720)
                .approved(true)
                .recommendation("APPROVE")
                .build();
    }
}
