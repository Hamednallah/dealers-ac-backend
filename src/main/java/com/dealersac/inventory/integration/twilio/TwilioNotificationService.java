package com.dealersac.inventory.integration.twilio;

/**
 * Twilio SMS notification service interface.
 *
 * Demonstrates awareness of Twilio integration (listed in JD under APIs & Integrations).
 *
 * In production, this would be implemented using the Twilio Java SDK:
 *   com.twilio.sdk:twilio:10.x
 *
 * Real implementation would call:
 *   Message.creator(new PhoneNumber(to), new PhoneNumber(from), body).create();
 */
public interface TwilioNotificationService {

    /**
     * Send an SMS notification.
     *
     * @param to   Recipient phone number (E.164 format: +1234567890)
     * @param body SMS message body (max 160 chars for single segment)
     */
    void sendSms(String to, String body);
}
