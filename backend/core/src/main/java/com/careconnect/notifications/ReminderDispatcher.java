package com.careconnect.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderDispatcher {

    private final SesService sesService;
    private final SnsService snsService;
    private final Logger log = LoggerFactory.getLogger(ReminderDispatcher.class);

    @Value("${notifications.demo.toEmail:}")
    private String demoEmail;

    @Value("${notifications.demo.toPhone:}")
    private String demoPhone;

    public ReminderDispatcher(SesService sesService, SnsService snsService) {
        this.sesService = sesService;
        this.snsService = snsService;
    }

    @Scheduled(fixedRateString = "${notifications.reminder.fixedRate:600000}")
    public void sendDemoReminder() {
        if ((demoEmail == null || demoEmail.isEmpty()) && (demoPhone == null || demoPhone.isEmpty())) {
            log.debug("No demo addresses configured for ReminderDispatcher");
            return;
        }

        String subject = "Reminder: Upcoming Appointment";
        String html = "<h1>Appointment Reminder</h1><p>This is a demo reminder for your upcoming appointment.</p>";
        String text = "This is a demo reminder for your upcoming appointment.";

        try {
            if (demoEmail != null && !demoEmail.isEmpty()) {
                String id = sesService.sendEmail(demoEmail, subject, html, text);
                log.info("Reminder email queued messageId={}", id);
            }
        } catch (Exception e) {
            log.warn("Failed to send demo reminder email: {}", e.getMessage());
        }

        try {
            if (demoPhone != null && !demoPhone.isEmpty()) {
                String id = snsService.publishSms(demoPhone, text);
                log.info("Reminder SMS queued messageId={}", id);
            }
        } catch (Exception e) {
            log.warn("Failed to send demo reminder SMS: {}", e.getMessage());
        }
    }
}
