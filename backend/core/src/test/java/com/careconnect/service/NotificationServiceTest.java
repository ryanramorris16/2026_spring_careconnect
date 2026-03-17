package com.careconnect.service;

import com.careconnect.dto.FirebaseNotificationRequest;
import com.careconnect.dto.NotificationResponse;
import com.careconnect.model.DeviceToken;
import com.careconnect.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NotificationService}.
 *
 * All external dependencies (AWS SES/SNS, repositories) are mocked so tests
 * exercise only the notification orchestration logic.  Users with ID 1-3 are
 * pre-stubbed in setUp() with email and phone so that notification channels
 * are available for all tests.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private com.careconnect.notifications.SesService sesService;
    @Mock
    private com.careconnect.notifications.SnsService snsService;
    @Mock
    private com.careconnect.repository.DeviceTokenRepository deviceTokenRepository;
    @Mock
    private com.careconnect.repository.NotificationSettingRepository notificationSettingRepository;
    @Mock
    private com.careconnect.repository.UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() throws Exception {
        // Pre-stub users 1-3 so that sendNotificationToUser, sendVitalAlert,
        // sendMedicationReminder, sendEmergencyAlert, and registerDeviceToken
        // can look them up without throwing.
        for (long id = 1; id <= 3; id++) {
            User user = new User();
            user.setId(id);
            user.setEmail("user" + id + "@example.com");
            user.setPhone("+1555000000" + id);
            user.setName("Test User" + id);
            lenient().when(userRepository.findById(id)).thenReturn(Optional.of(user));
        }

        // SES / SNS stubs — return a stable message ID for assertions
        lenient().when(sesService.sendEmail(anyString(), anyString(), any(), anyString()))
                .thenReturn("ses-msg-id");
        lenient().when(sesService.sendMedicationReminder(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("ses-med-id");
        lenient().when(snsService.publishSms(anyString(), anyString()))
                .thenReturn("sns-sms-id");
        lenient().when(snsService.sendVitalAlertSms(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("sns-vital-id");
        lenient().when(snsService.sendMedicationReminderSms(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("sns-med-id");
        lenient().when(snsService.sendEmergencyAlertSms(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("sns-emerg-id");

        // No registered device tokens by default
        lenient().when(deviceTokenRepository.findByUserIdAndIsActiveTrue(anyLong()))
                .thenReturn(Collections.emptyList());
    }

    // ========== sendNotification tests ==========

    @Test
    @DisplayName("sendNotification_validRequest_returnsSuccessResponse")
    void sendNotification_validRequest_returnsSuccessResponse() throws Exception {
        final FirebaseNotificationRequest request = FirebaseNotificationRequest.builder()
                .title("Test Title")
                .body("Test Body")
                .targetToken("fcm-token-123")
                .targetUserId(1L)
                .userType("PATIENT")
                .notificationType("VITAL_ALERT")
                .build();

        final NotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        // Non-phone tokens receive a push-placeholder message ID
        assertNotNull(response.getMessageId());
        assertTrue(response.getMessageId().startsWith("push-placeholder-"));
    }

    @Test
    @DisplayName("sendNotification_requestWithAllFields_returnsSuccessResponse")
    void sendNotification_requestWithAllFields_returnsSuccessResponse() throws Exception {
        final Map<String, String> data = new HashMap<>();
        data.put("key1", "value1");

        final FirebaseNotificationRequest request = FirebaseNotificationRequest.builder()
                .title("Alert")
                .body("Your vitals are abnormal")
                .imageUrl("https://example.com/image.png")
                .targetToken("fcm-token-456")
                .targetUserId(2L)
                .userType("CAREGIVER")
                .notificationType("EMERGENCY")
                .deepLink("careconnect://vitals/2")
                .data(data)
                .build();

        final NotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getMessageId());
    }

    @Test
    @DisplayName("sendNotification_requestWithMinimalFields_returnsSuccessResponse")
    void sendNotification_requestWithMinimalFields_returnsSuccessResponse() throws Exception {
        final FirebaseNotificationRequest request = FirebaseNotificationRequest.builder()
                .title("Reminder")
                .body("Take your medication")
                .build();

        final NotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getMessageId());
    }

    @Test
    @DisplayName("sendNotification_nullRequest_returnsFailureResponse")
    void sendNotification_nullRequest_returnsFailureResponse() throws Exception {
        // Null request causes an NPE inside sendNotification, caught by the
        // try/catch which returns a failure response.
        final NotificationResponse response = notificationService.sendNotification(null);

        assertNotNull(response);
        assertFalse(response.isSuccess());
    }

    // ========== sendBulkNotifications tests ==========

    @Test
    @DisplayName("sendBulkNotifications_singleRequestList_returnsListWithOneSuccessResponse")
    void sendBulkNotifications_singleRequestList_returnsListWithOneSuccessResponse() throws Exception {
        final FirebaseNotificationRequest request = FirebaseNotificationRequest.builder()
                .title("Bulk Title")
                .body("Bulk Body")
                .targetToken("token-1")
                .build();

        final List<NotificationResponse> responses = notificationService.sendBulkNotifications(List.of(request));

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertTrue(responses.get(0).isSuccess());
        assertNotNull(responses.get(0).getMessageId());
    }

    @Test
    @DisplayName("sendBulkNotifications_multipleRequests_returnsListWithSuccessResponse")
    void sendBulkNotifications_multipleRequests_returnsListWithSuccessResponse() throws Exception {
        final FirebaseNotificationRequest request1 = FirebaseNotificationRequest.builder()
                .title("Title 1")
                .body("Body 1")
                .targetToken("token-1")
                .notificationType("VITAL_ALERT")
                .build();

        final FirebaseNotificationRequest request2 = FirebaseNotificationRequest.builder()
                .title("Title 2")
                .body("Body 2")
                .targetToken("token-2")
                .notificationType("MEDICATION_REMINDER")
                .build();

        final List<NotificationResponse> responses = notificationService.sendBulkNotifications(List.of(request1, request2));

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertTrue(responses.get(0).isSuccess());
    }

    @Test
    @DisplayName("sendBulkNotifications_emptyRequestList_returnsEmptyList")
    void sendBulkNotifications_emptyRequestList_returnsEmptyList() throws Exception {
        // An empty input list produces an empty output list via stream().map()
        final List<NotificationResponse> responses = notificationService.sendBulkNotifications(List.of());

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    // ========== sendNotificationToUser tests ==========

    @Test
    @DisplayName("sendNotificationToUser_validUserWithAllParameters_returnsSuccessResponseList")
    void sendNotificationToUser_validUserWithAllParameters_returnsSuccessResponseList() throws Exception {
        final Map<String, String> data = Map.of("alertLevel", "HIGH");

        final List<NotificationResponse> responses = notificationService.sendNotificationToUser(
                1L, "Vital Alert", "Heart rate is abnormal", "VITAL_ALERT", data);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
    }

    @Test
    @DisplayName("sendNotificationToUser_nullDataMap_returnsSuccessResponseList")
    void sendNotificationToUser_nullDataMap_returnsSuccessResponseList() throws Exception {
        final List<NotificationResponse> responses = notificationService.sendNotificationToUser(
                2L, "Medication Reminder", "Time to take Aspirin", "MEDICATION_REMINDER", null);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
    }

    @Test
    @DisplayName("sendNotificationToUser_emergencyNotificationType_returnsSuccessResponseList")
    void sendNotificationToUser_emergencyNotificationType_returnsSuccessResponseList() throws Exception {
        final Map<String, String> data = Map.of("location", "Home");

        final List<NotificationResponse> responses = notificationService.sendNotificationToUser(
                3L, "Emergency", "Fall detected", "EMERGENCY", data);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
    }

    // ========== sendVitalAlert tests ==========

    @Test
    @DisplayName("sendVitalAlert_validVitalData_returnsCompletedFutureWithSuccessResponse")
    void sendVitalAlert_validVitalData_returnsCompletedFutureWithSuccessResponse() throws Exception {
        final CompletableFuture<List<NotificationResponse>> future =
                notificationService.sendVitalAlert(1L, "HEART_RATE", "120", "HIGH");

        assertNotNull(future);

        final List<NotificationResponse> responses = future.get();
        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
    }

    @Test
    @DisplayName("sendVitalAlert_bloodPressureVitalType_returnsCompletedFutureWithSuccess")
    void sendVitalAlert_bloodPressureVitalType_returnsCompletedFutureWithSuccess() throws Exception {
        final CompletableFuture<List<NotificationResponse>> future =
                notificationService.sendVitalAlert(2L, "BLOOD_PRESSURE", "180/110", "CRITICAL");

        assertNotNull(future);
        final List<NotificationResponse> responses = future.get();
        assertFalse(responses.isEmpty());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
    }

    @Test
    @DisplayName("sendVitalAlert_lowAlertLevel_returnsCompletedFutureWithSuccess")
    void sendVitalAlert_lowAlertLevel_returnsCompletedFutureWithSuccess() throws Exception {
        final CompletableFuture<List<NotificationResponse>> future =
                notificationService.sendVitalAlert(3L, "OXYGEN_SATURATION", "95", "LOW");

        assertNotNull(future);
        final List<NotificationResponse> responses = future.get();
        assertFalse(responses.isEmpty());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
    }

    // ========== sendMedicationReminder tests ==========

    @Test
    @DisplayName("sendMedicationReminder_validMedicationData_returnsCompletedFutureWithSuccessResponse")
    void sendMedicationReminder_validMedicationData_returnsCompletedFutureWithSuccessResponse() throws Exception {
        final CompletableFuture<List<NotificationResponse>> future =
                notificationService.sendMedicationReminder(1L, "Aspirin", "100mg", "08:00 AM");

        assertNotNull(future);

        final List<NotificationResponse> responses = future.get();
        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
    }

    @Test
    @DisplayName("sendMedicationReminder_differentMedication_returnsCompletedFutureWithSuccess")
    void sendMedicationReminder_differentMedication_returnsCompletedFutureWithSuccess() throws Exception {
        final CompletableFuture<List<NotificationResponse>> future =
                notificationService.sendMedicationReminder(2L, "Metformin", "500mg", "12:00 PM");

        assertNotNull(future);
        final List<NotificationResponse> responses = future.get();
        assertFalse(responses.isEmpty());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
    }

    @Test
    @DisplayName("sendMedicationReminder_eveningSchedule_returnsCompletedFutureWithSuccess")
    void sendMedicationReminder_eveningSchedule_returnsCompletedFutureWithSuccess() throws Exception {
        final CompletableFuture<List<NotificationResponse>> future =
                notificationService.sendMedicationReminder(3L, "Lisinopril", "10mg", "09:00 PM");

        assertNotNull(future);
        final List<NotificationResponse> responses = future.get();
        assertFalse(responses.isEmpty());
    }

    // ========== sendEmergencyAlert tests ==========

    @Test
    @DisplayName("sendEmergencyAlert_validEmergencyData_returnsCompletedFutureWithSuccessResponse")
    void sendEmergencyAlert_validEmergencyData_returnsCompletedFutureWithSuccessResponse() throws Exception {
        final CompletableFuture<List<NotificationResponse>> future =
                notificationService.sendEmergencyAlert(1L, "FALL_DETECTED", "Living Room");

        assertNotNull(future);

        final List<NotificationResponse> responses = future.get();
        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
    }

    @Test
    @DisplayName("sendEmergencyAlert_cardiacEmergency_returnsCompletedFutureWithSuccess")
    void sendEmergencyAlert_cardiacEmergency_returnsCompletedFutureWithSuccess() throws Exception {
        final CompletableFuture<List<NotificationResponse>> future =
                notificationService.sendEmergencyAlert(2L, "CARDIAC_EVENT", "Bedroom");

        assertNotNull(future);
        final List<NotificationResponse> responses = future.get();
        assertFalse(responses.isEmpty());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
    }

    @Test
    @DisplayName("sendEmergencyAlert_nullLocation_returnsCompletedFutureWithSuccess")
    void sendEmergencyAlert_nullLocation_returnsCompletedFutureWithSuccess() throws Exception {
        final CompletableFuture<List<NotificationResponse>> future =
                notificationService.sendEmergencyAlert(3L, "SOS", null);

        assertNotNull(future);
        final List<NotificationResponse> responses = future.get();
        assertFalse(responses.isEmpty());
        assertTrue(responses.stream().allMatch(NotificationResponse::isSuccess));
    }

    // ========== registerDeviceToken tests ==========

    @Test
    @DisplayName("registerDeviceToken_androidDevice_completesWithoutException")
    void registerDeviceToken_androidDevice_completesWithoutException() throws Exception {
        assertDoesNotThrow(() ->
                notificationService.registerDeviceToken(1L, "fcm-token-abc", "device-123", DeviceToken.DeviceType.ANDROID));
    }

    @Test
    @DisplayName("registerDeviceToken_iosDevice_completesWithoutException")
    void registerDeviceToken_iosDevice_completesWithoutException() throws Exception {
        assertDoesNotThrow(() ->
                notificationService.registerDeviceToken(2L, "fcm-token-def", "device-456", DeviceToken.DeviceType.IOS));
    }

    @Test
    @DisplayName("registerDeviceToken_webDevice_completesWithoutException")
    void registerDeviceToken_webDevice_completesWithoutException() throws Exception {
        assertDoesNotThrow(() ->
                notificationService.registerDeviceToken(3L, "fcm-token-ghi", "device-789", DeviceToken.DeviceType.WEB));
    }

    @Test
    @DisplayName("registerDeviceToken_nullUserId_throwsBecauseUserNotFound")
    void registerDeviceToken_nullUserId_throwsBecauseUserNotFound() throws Exception {
        // Production calls userRepository.findById(null).orElseThrow() which
        // throws because no user matches a null ID.
        assertThrows(Exception.class, () ->
                notificationService.registerDeviceToken(null, null, null, null));
    }

    // ========== unregisterDeviceToken tests ==========

    @Test
    @DisplayName("unregisterDeviceToken_validToken_completesWithoutException")
    void unregisterDeviceToken_validToken_completesWithoutException() throws Exception {
        assertDoesNotThrow(() ->
                notificationService.unregisterDeviceToken("fcm-token-abc"));
    }

    @Test
    @DisplayName("unregisterDeviceToken_nullToken_completesWithoutException")
    void unregisterDeviceToken_nullToken_completesWithoutException() throws Exception {
        assertDoesNotThrow(() ->
                notificationService.unregisterDeviceToken(null));
    }

    @Test
    @DisplayName("unregisterDeviceToken_emptyToken_completesWithoutException")
    void unregisterDeviceToken_emptyToken_completesWithoutException() throws Exception {
        assertDoesNotThrow(() ->
                notificationService.unregisterDeviceToken(""));
    }
}
