package com.dealersac.inventory.integration.credit;

import lombok.Builder;
import lombok.Getter;

/**
 * Result of a credit check for a dealer.
 *
 * Demonstrates: Payment / Credit APIs (e.g., 700Credit) — JD requirement.
 */
@Getter
@Builder
public class CreditCheckResult {
    private String  dealerId;
    private int     creditScore;      // e.g., 300–850
    private boolean approved;
    private String  recommendation;   // APPROVE, REVIEW, DECLINE
}
