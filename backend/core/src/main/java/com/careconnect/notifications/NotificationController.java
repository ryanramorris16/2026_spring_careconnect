package com.careconnect.notifications;

import com.careconnect.notifications.dto.DemoNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        try {
            // Send email confirmation
            if (req.getToEmail() != null && !req.getToEmail().isEmpty()) {
                String emailId = sesService.sendPaymentConfirmation(
                    req.getToEmail(),
                    req.getRecipientName() != null ? req.getRecipientName() : "Valued Customer",
                    req.getAmount() != null ? req.getAmount() : "0.00",
                    "TXN-" + System.currentTimeMillis()
                );
                log.info("Sent payment confirmation email messageId={}", emailId);
            }

            // Send SMS confirmation
            if (req.getToPhone() != null && !req.getToPhone().isEmpty()) {
                String smsId = snsService.sendPaymentConfirmationSms(
                    req.getToPhone(),
                    req.getRecipientName() != null ? req.getRecipientName() : "Customer",
                    req.getAmount() != null ? req.getAmount() : "0.00"
                );
                log.info("Sent payment confirmation SMS messageId={}", smsId);
            }

            return ResponseEntity.ok().body("Payment notifications sent successfully");
        } catch (Exception e) {
            log.error("Failed to send payment notifications: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send notifications: " + e.getMessage());
        }
    }

    @PostMapping("/message")
    public ResponseEntity<?> sendMessageNotification(@RequestBody DemoNotificationRequest req) {
        try {
            // Send email message
            if (req.getToEmail() != null && !req.getToEmail().isEmpty()) {
                String emailId = sesService.sendCaregiverMessage(
                    req.getToEmail(),
                    "CareConnect System", // fromName
                    req.getRecipientName() != null ? req.getRecipientName() : "Recipient",
                    req.getMessage() != null ? req.getMessage() : "You have a new message",
                    "normal" // priority
                );
                log.info("Sent message email messageId={}", emailId);
            }

            // Send SMS message
            if (req.getToPhone() != null && !req.getToPhone().isEmpty()) {
                String smsId = snsService.sendCaregiverMessageSms(
                    req.getToPhone(),
                    "CareConnect",
                    req.getMessage() != null ? req.getMessage() : "You have a new message",
                    false // not urgent
                );
                log.info("Sent message SMS messageId={}", smsId);
            }

            return ResponseEntity.ok().body("Message notifications sent successfully");
        } catch (Exception e) {
            log.error("Failed to send message notifications: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send notifications: " + e.getMessage());
        }
    }

    @PostMapping("/medication-reminder")
    public ResponseEntity<?> sendMedicationReminder(@RequestBody DemoNotificationRequest req) {
        try {
            String medicationName = req.getSubject() != null ? req.getSubject() : "Medication";
            String dosage = req.getAmount() != null ? req.getAmount() : "As prescribed";
            String scheduledTime = req.getMessage() != null ? req.getMessage() : "Now";

            // Send email reminder
            if (req.getToEmail() != null && !req.getToEmail().isEmpty()) {
                String emailId = sesService.sendMedicationReminder(
                    req.getToEmail(),
                    req.getRecipientName() != null ? req.getRecipientName() : "Patient",
                    medicationName,
                    dosage,
                    scheduledTime
                );
                log.info("Sent medication reminder email messageId={}", emailId);
            }

            // Send SMS reminder
            if (req.getToPhone() != null && !req.getToPhone().isEmpty()) {
                String smsId = snsService.sendMedicationReminderSms(
                    req.getToPhone(),
                    req.getRecipientName() != null ? req.getRecipientName() : "Patient",
                    medicationName,
                    dosage
                );
                log.info("Sent medication reminder SMS messageId={}", smsId);
            }

            return ResponseEntity.ok().body("Medication reminder notifications sent successfully");
        } catch (Exception e) {
            log.error("Failed to send medication reminder notifications: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send notifications: " + e.getMessage());
        }
    }

    @PostMapping("/appointment-reminder")
    public ResponseEntity<?> sendAppointmentReminder(@RequestBody DemoNotificationRequest req) {
        try {
            String appointmentType = req.getSubject() != null ? req.getSubject() : "Appointment";
            String dateTime = req.getMessage() != null ? req.getMessage() : "Today";
            String location = req.getAmount() != null ? req.getAmount() : "Clinic";

            // Send email reminder
            if (req.getToEmail() != null && !req.getToEmail().isEmpty()) {
                String emailId = sesService.sendAppointmentReminder(
                    req.getToEmail(),
                    req.getRecipientName() != null ? req.getRecipientName() : "Patient",
                    appointmentType,
                    dateTime,
                    location
                );
                log.info("Sent appointment reminder email messageId={}", emailId);
            }

            // Send SMS reminder
            if (req.getToPhone() != null && !req.getToPhone().isEmpty()) {
                String smsId = snsService.sendAppointmentReminderSms(
                    req.getToPhone(),
                    req.getRecipientName() != null ? req.getRecipientName() : "Patient",
                    appointmentType,
                    dateTime
                );
                log.info("Sent appointment reminder SMS messageId={}", smsId);
            }

            return ResponseEntity.ok().body("Appointment reminder notifications sent successfully");
        } catch (Exception e) {
            log.error("Failed to send appointment reminder notifications: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send notifications: " + e.getMessage());
        }
    }

    @PostMapping("/emergency-alert")
    public ResponseEntity<?> sendEmergencyAlert(@RequestBody DemoNotificationRequest req) {
        try {
            String emergencyType = req.getSubject() != null ? req.getSubject() : "Emergency";
            String location = req.getMessage() != null ? req.getMessage() : "Unknown location";

            // Send email alert
            if (req.getToEmail() != null && !req.getToEmail().isEmpty()) {
                String emailId = sesService.sendEmail(
                    req.getToEmail(),
                    "EMERGENCY ALERT - " + emergencyType,
                    null,
                    String.format("EMERGENCY: %s requires immediate attention. Type: %s, Location: %s",
                        req.getRecipientName() != null ? req.getRecipientName() : "Patient",
                        emergencyType, location)
                );
                log.info("Sent emergency alert email messageId={}", emailId);
            }

            // Send SMS alert
            if (req.getToPhone() != null && !req.getToPhone().isEmpty()) {
                String smsId = snsService.sendEmergencyAlertSms(
                    req.getToPhone(),
                    req.getRecipientName() != null ? req.getRecipientName() : "Patient",
                    emergencyType,
                    location
                );
                log.info("Sent emergency alert SMS messageId={}", smsId);
            }

            return ResponseEntity.ok().body("Emergency alert notifications sent successfully");
        } catch (Exception e) {
            log.error("Failed to send emergency alert notifications: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send notifications: " + e.getMessage());
        }
    }

    @PostMapping("/caregiver-message")
    public ResponseEntity<?> sendCaregiverMessage(@RequestBody DemoNotificationRequest req) {
        try {
            String fromName = req.getSubject() != null ? req.getSubject() : "Caregiver";
            String message = req.getMessage() != null ? req.getMessage() : "New message from caregiver";
            boolean urgent = "urgent".equalsIgnoreCase(req.getAmount()); // Using amount field for priority

            // Send email message
            if (req.getToEmail() != null && !req.getToEmail().isEmpty()) {
                String emailId = sesService.sendCaregiverMessage(
                    req.getToEmail(),
                    fromName,
                    req.getRecipientName() != null ? req.getRecipientName() : "Recipient",
                    message,
                    urgent ? "urgent" : "normal"
                );
                log.info("Sent caregiver message email messageId={}", emailId);
            }

            // Send SMS message
            if (req.getToPhone() != null && !req.getToPhone().isEmpty()) {
                String smsId = snsService.sendCaregiverMessageSms(
                    req.getToPhone(),
                    fromName,
                    message,
                    urgent
                );
                log.info("Sent caregiver message SMS messageId={}", smsId);
            }

            return ResponseEntity.ok().body("Caregiver message notifications sent successfully");
        } catch (Exception e) {
            log.error("Failed to send caregiver message notifications: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send notifications: " + e.getMessage());
        }
    }

    // Future enhancement: Bulk notification endpoint
    @PostMapping("/bulk")
    public ResponseEntity<?> sendBulkNotifications(@RequestBody List<DemoNotificationRequest> requests) {
        // TODO: Implement bulk notification sending
        // This would be useful for sending reminders to multiple recipients
        return ResponseEntity.ok().body("Bulk notifications not yet implemented");
    }

    // Future enhancement: Topic-based notifications for groups
    @PostMapping("/topic/{topicName}")
    public ResponseEntity<?> sendTopicNotification(@PathVariable String topicName, @RequestBody DemoNotificationRequest req) {
        // TODO: Implement topic-based notifications
        // This would allow broadcasting to groups of caregivers or patients
        return ResponseEntity.ok().body("Topic notifications not yet implemented");
    }
}
