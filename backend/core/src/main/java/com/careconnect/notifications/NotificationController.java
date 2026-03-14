package com.careconnect.notifications;

import com.careconnect.notifications.dto.DemoNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/demo")
public class NotificationController {

    private final SesService sesService;
    private final SnsService snsService;
    private final Logger log = LoggerFactory.getLogger(NotificationController.class);

    public NotificationController(SesService sesService, SnsService snsService) {
        this.sesService = sesService;
        this.snsService = snsService;
    }

    @PostMapping("/payment")
    public ResponseEntity<?> sendPaymentNotification(@RequestBody DemoNotificationRequest req) {
        String subject = req.getSubject() != null ? req.getSubject() : "Payment Confirmation";
        String html = "<h1>Payment Received</h1>" +
                "<p>Hi " + safe(req.getRecipientName()) + ",</p>" +
                "<p>We received your payment of <strong>" + safe(req.getAmount()) + "</strong>.</p>" +
                "<p>Thank you.</p>";
        String text = "Payment received for " + safe(req.getAmount());

        try {
            String emailId = sesService.sendEmail(req.getToEmail(), subject, html, text);
            log.info("Sent payment email messageId={}", emailId);
        } catch (Exception e) {
            log.warn("Failed to send payment email: {}", e.getMessage());
        }

        if (req.getToPhone() != null && !req.getToPhone().isEmpty()) {
            try {
                String smsId = snsService.publishSms(req.getToPhone(), "Payment received: " + safe(req.getAmount()));
                log.info("Sent payment SMS messageId={}", smsId);
            } catch (Exception e) {
                log.warn("Failed to send payment SMS: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok().body("queued");
    }

    @PostMapping("/message")
    public ResponseEntity<?> sendMessageNotification(@RequestBody DemoNotificationRequest req) {
        String subject = req.getSubject() != null ? req.getSubject() : "New Message";
        String html = "<h1>New Message</h1><p>" + safe(req.getMessage()) + "</p>";
        String text = safe(req.getMessage());

        try {
            sesService.sendEmail(req.getToEmail(), subject, html, text);
        } catch (Exception e) {
            log.warn("Failed to send message email: {}", e.getMessage());
        }

        if (req.getToPhone() != null && !req.getToPhone().isEmpty()) {
            try {
                snsService.publishSms(req.getToPhone(), text);
            } catch (Exception e) {
                log.warn("Failed to send message SMS: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok().body("queued");
    }

    private String safe(String s) { return s == null ? "" : s; }
}
