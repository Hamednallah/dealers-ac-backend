package com.dealersac.inventory.integration.twilio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * No-op Twilio stub for the take-home task.
 *
 * To enable real Twilio:
 * 1. Add dependency: com.twilio.sdk:twilio:10.x
 * 2. Configure TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER env vars
 * 3. Replace this stub with:
 *
 *    Twilio.init(accountSid, authToken);
 *    Message.creator(
 *        new PhoneNumber(to),
 *        new PhoneNumber(fromNumber),
 *        body
 *    ).create();
 */
@Slf4j
@Service
public class TwilioNotificationStub implements TwilioNotificationService {

    @Override
    public void sendSms(String to, String body) {
        // Stub — logs only. Replace with real Twilio SDK call in production.
        log.info("[TWILIO STUB] SMS to: {} | Body: {}", to, body);
    }
}
