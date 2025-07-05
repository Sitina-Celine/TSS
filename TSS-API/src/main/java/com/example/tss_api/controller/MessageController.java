package com.example.tss_api.controller;

import com.example.tss_api.model.Message;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * MessageController simulates the TSS API.
 * It allows:
 * - Testing API connection
 * - Fetching sample messages
 * - Simulating sending EMAIL and SMS
 * - Updating message status
 */
@RestController
public class MessageController {

    /**
     * Root endpoint to verify API is up and running.
     * Access via: GET http://localhost:8080/
     */
    @GetMapping("/")
    public String home() {
        return "TSS API is working!";
    }

    /**
     * Simulates fetching messages to be sent.
     * Returns a hardcoded list of messages (1 SMS, 1 EMAIL).
     * Access via: GET http://localhost:8080/messages
     */
    @GetMapping("/messages")
    public List<Message> getMessages() {
        return Arrays.asList(
            new Message(1, "SMS", "+254700000001", "Hello Doe, your code is working!", false),
            new Message(2, "EMAIL", "Doe@email.com", "You have mail from TSS Worker!", true)
        );
    }

    /**
     * Simulates sending an email.
     * Triggered by a POST request from the worker.
     * Access via: POST http://localhost:8080/email
     * Request Body: { "to": "...", "message": "..." }
     */
    @PostMapping("/email")
    public String sendEmail(@RequestBody Map<String, String> email) {
        System.out.println("📧 Simulated sending EMAIL to: " + email.get("to"));
        System.out.println("✉️  Message: " + email.get("message"));
        return "Email sent successfully";
    }

    /**
     * Simulates sending an SMS.
     * Triggered by a POST request from the worker.
     * Access via: POST http://localhost:8080/sms
     * Request Body: { "to": "...", "message": "..." }
     */
    @PostMapping("/sms")
    public String sendSms(@RequestBody Map<String, String> sms) {
        System.out.println("Simulated sending SMS to: " + sms.get("to"));
        System.out.println(" Message: " + sms.get("message"));
        return "SMS sent successfully";
    }

    /**
     * Updates the message status to "sent = true".
     * Called by the worker once a message is successfully sent.
     * Access via: POST http://localhost:8080/messages/update
     * Request Body: { "id": 1, "sent": true }
     */
    @PostMapping("/messages/update")
    public String updateMessageStatus(@RequestBody Map<String, Object> payload) {
        int id = (int) payload.get("id");
        boolean sent = (boolean) payload.get("sent");

        // Simulate database or log update
        System.out.println("✅ Marked message ID " + id + " as sent: " + sent);

        return "Message status updated successfully";
    }
}
