package com.dealersac.inventory.integration.credit;

/**
 * Credit check service interface — demonstrates 700Credit / payment API integration awareness.
 *
 * The real implementation would call the 700Credit API:
 *   POST https://api.700credit.com/v2/creditcheck
 *   Headers: Authorization: Bearer <API_KEY>
 *   Body: { "dealerId": "...", "ssn": "...", "businessEin": "..." }
 *
 * Response is parsed into a CreditCheckResult.
 */
public interface CreditCheckService {

    /**
     * Run a credit check for a dealer.
     *
     * @param dealerId   Dealer UUID
     * @param tenantId   Caller's tenant (for authorization)
     * @return           CreditCheckResult with score and recommendation
     */
    CreditCheckResult checkCredit(String dealerId, String tenantId);
}
