package com.careconnect.controller;

import com.careconnect.service.EmailTestService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTestControllerTest {

    @Mock
    private EmailTestService emailTestService;

    @InjectMocks
    private EmailTestController controller;

    // ── shared constants ──────────────────────────────────────────────────────

    private static final String TEST_EMAIL = "test@example.com";

    // ── shared helpers ────────────────────────────────────────────────────────

    /** Returns a mutable config map so individual tests can override fields. */
    private Map<String, Object> configMap(boolean valid) {
        Map<String, Object> config = new HashMap<>();
        config.put("configurationValid", valid);
        config.put("provider", "console");
        config.put("mailSenderAvailable", true);
        return config;
    }

    // ── POST /v1/api/email-test/send ──────────────────────────────────────────

    @Nested
    class SendTestEmail {

        @Test
        void returns200_whenEmailIsValid() {
            when(emailTestService.testEmailConfiguration(TEST_EMAIL)).thenReturn(Map.of("success", true));

            ResponseEntity<Map<String, Object>> response =
                    controller.sendTestEmail(Map.of("email", TEST_EMAIL));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void returnsServiceResult_whenEmailIsValid() {
            Map<String, Object> serviceResult = Map.of("success", true, "message", "sent");
            when(emailTestService.testEmailConfiguration(TEST_EMAIL)).thenReturn(serviceResult);

            ResponseEntity<Map<String, Object>> response =
                    controller.sendTestEmail(Map.of("email", TEST_EMAIL));

            assertThat(response.getBody()).isEqualTo(serviceResult);
        }

        @Test
        void callsServiceWithCorrectEmail() {
            when(emailTestService.testEmailConfiguration(TEST_EMAIL)).thenReturn(Map.of());

            controller.sendTestEmail(Map.of("email", TEST_EMAIL));

            verify(emailTestService).testEmailConfiguration(TEST_EMAIL);
        }

        @Test
        void returns400_whenEmailKeyAbsent() {
            ResponseEntity<Map<String, Object>> response =
                    controller.sendTestEmail(Collections.emptyMap());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns400_whenEmailIsNull() {
            Map<String, String> body = new HashMap<>();
            body.put("email", null);

            ResponseEntity<Map<String, Object>> response = controller.sendTestEmail(body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns400_whenEmailIsEmpty() {
            ResponseEntity<Map<String, Object>> response =
                    controller.sendTestEmail(Map.of("email", ""));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns400_whenEmailIsBlankWhitespace() {
            ResponseEntity<Map<String, Object>> response =
                    controller.sendTestEmail(Map.of("email", "   "));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void errorBodyContainsRequiredMessage_whenEmailMissing() {
            ResponseEntity<Map<String, Object>> response =
                    controller.sendTestEmail(Collections.emptyMap());

            assertThat(response.getBody())
                    .containsEntry("error", "Email address is required");
        }

        @Test
        void doesNotCallService_whenEmailIsNull() {
            Map<String, String> body = new HashMap<>();
            body.put("email", null);

            controller.sendTestEmail(body);

            verifyNoInteractions(emailTestService);
        }

        @Test
        void doesNotCallService_whenEmailIsEmpty() {
            controller.sendTestEmail(Map.of("email", ""));

            verifyNoInteractions(emailTestService);
        }

        @Test
        void doesNotCallService_whenEmailIsBlank() {
            controller.sendTestEmail(Map.of("email", "   "));

            verifyNoInteractions(emailTestService);
        }
    }

    // ── GET /v1/api/email-test/config ─────────────────────────────────────────

    @Nested
    class GetEmailConfig {

        @Test
        void returns200() {
            when(emailTestService.getEmailConfiguration()).thenReturn(Map.of());

            ResponseEntity<Map<String, Object>> response = controller.getEmailConfig();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void returnsServiceResult() {
            Map<String, Object> config = Map.of("provider", "console", "fromEmail", "noreply@test.com");
            when(emailTestService.getEmailConfiguration()).thenReturn(config);

            ResponseEntity<Map<String, Object>> response = controller.getEmailConfig();

            assertThat(response.getBody()).isEqualTo(config);
        }

        @Test
        void callsGetEmailConfigurationOnce() {
            when(emailTestService.getEmailConfiguration()).thenReturn(Map.of());

            controller.getEmailConfig();

            verify(emailTestService, times(1)).getEmailConfiguration();
        }
    }

    // ── GET /v1/api/email-test/validate ──────────────────────────────────────

    @Nested
    class ValidateEmailConfiguration {

        @Test
        void returns200() {
            when(emailTestService.validateEmailConfiguration()).thenReturn(Map.of());

            ResponseEntity<Map<String, Object>> response = controller.validateEmailConfiguration();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void returnsServiceResult() {
            Map<String, Object> validation = Map.of("overallValid", true, "providerSet", true);
            when(emailTestService.validateEmailConfiguration()).thenReturn(validation);

            ResponseEntity<Map<String, Object>> response = controller.validateEmailConfiguration();

            assertThat(response.getBody()).isEqualTo(validation);
        }

        @Test
        void callsValidateEmailConfigurationOnce() {
            when(emailTestService.validateEmailConfiguration()).thenReturn(Map.of());

            controller.validateEmailConfiguration();

            verify(emailTestService, times(1)).validateEmailConfiguration();
        }
    }

    // ── POST /v1/api/email-test/all ───────────────────────────────────────────

    @Nested
    class TestAllEmailTypes {

        @Test
        void returns200_whenEmailIsValid() {
            when(emailTestService.testAllEmailTypes(TEST_EMAIL)).thenReturn(Map.of());

            ResponseEntity<Map<String, Object>> response =
                    controller.testAllEmailTypes(Map.of("email", TEST_EMAIL));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void returnsServiceResult_whenEmailIsValid() {
            Map<String, Object> results = Map.of(
                    "verificationEmail", "SUCCESS",
                    "passwordResetEmail", "SUCCESS",
                    "passwordSetupEmail", "SUCCESS");
            when(emailTestService.testAllEmailTypes(TEST_EMAIL)).thenReturn(results);

            ResponseEntity<Map<String, Object>> response =
                    controller.testAllEmailTypes(Map.of("email", TEST_EMAIL));

            assertThat(response.getBody()).isEqualTo(results);
        }

        @Test
        void callsServiceWithCorrectEmail() {
            when(emailTestService.testAllEmailTypes(TEST_EMAIL)).thenReturn(Map.of());

            controller.testAllEmailTypes(Map.of("email", TEST_EMAIL));

            verify(emailTestService).testAllEmailTypes(TEST_EMAIL);
        }

        @Test
        void returns400_whenEmailKeyAbsent() {
            ResponseEntity<Map<String, Object>> response =
                    controller.testAllEmailTypes(Collections.emptyMap());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns400_whenEmailIsNull() {
            Map<String, String> body = new HashMap<>();
            body.put("email", null);

            ResponseEntity<Map<String, Object>> response = controller.testAllEmailTypes(body);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns400_whenEmailIsEmpty() {
            ResponseEntity<Map<String, Object>> response =
                    controller.testAllEmailTypes(Map.of("email", ""));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void returns400_whenEmailIsBlankWhitespace() {
            ResponseEntity<Map<String, Object>> response =
                    controller.testAllEmailTypes(Map.of("email", "   "));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void errorBodyContainsRequiredMessage_whenEmailMissing() {
            ResponseEntity<Map<String, Object>> response =
                    controller.testAllEmailTypes(Collections.emptyMap());

            assertThat(response.getBody())
                    .containsEntry("error", "Email address is required");
        }

        @Test
        void doesNotCallService_whenEmailIsNull() {
            Map<String, String> body = new HashMap<>();
            body.put("email", null);

            controller.testAllEmailTypes(body);

            verifyNoInteractions(emailTestService);
        }

        @Test
        void doesNotCallService_whenEmailIsEmpty() {
            controller.testAllEmailTypes(Map.of("email", ""));

            verifyNoInteractions(emailTestService);
        }

        @Test
        void doesNotCallService_whenEmailIsBlank() {
            controller.testAllEmailTypes(Map.of("email", "   "));

            verifyNoInteractions(emailTestService);
        }
    }

    // ── GET /v1/api/email-test/health ─────────────────────────────────────────

    @Nested
    class HealthCheck {

        @Test
        void returns200() {
            when(emailTestService.getEmailConfiguration()).thenReturn(configMap(true));

            ResponseEntity<Map<String, Object>> response = controller.healthCheck();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void statusIsUp_andHealthyIsTrue_whenConfigurationIsValid() {
            when(emailTestService.getEmailConfiguration()).thenReturn(configMap(true));

            Map<String, Object> body = controller.healthCheck().getBody();

            assertThat(body).containsEntry("status", "UP")
                            .containsEntry("healthy", true);
        }

        @Test
        void statusIsDown_andHealthyIsFalse_whenConfigurationIsInvalid() {
            when(emailTestService.getEmailConfiguration()).thenReturn(configMap(false));

            Map<String, Object> body = controller.healthCheck().getBody();

            assertThat(body).containsEntry("status", "DOWN")
                            .containsEntry("healthy", false);
        }

        @Test
        void bodyIncludesProviderFromConfig() {
            Map<String, Object> config = configMap(true);
            config.put("provider", "smtp");
            when(emailTestService.getEmailConfiguration()).thenReturn(config);

            Map<String, Object> body = controller.healthCheck().getBody();

            assertThat(body).containsEntry("provider", "smtp");
        }

        @Test
        void bodyIncludesMailSenderAvailableFromConfig() {
            Map<String, Object> config = configMap(true);
            config.put("mailSenderAvailable", false);
            when(emailTestService.getEmailConfiguration()).thenReturn(config);

            Map<String, Object> body = controller.healthCheck().getBody();

            assertThat(body).containsEntry("mailSenderAvailable", false);
        }

        @Test
        void callsGetEmailConfigurationOnce() {
            when(emailTestService.getEmailConfiguration()).thenReturn(configMap(true));

            controller.healthCheck();

            verify(emailTestService, times(1)).getEmailConfiguration();
        }

        @Test
        void bodyContainsFourKeys() {
            when(emailTestService.getEmailConfiguration()).thenReturn(configMap(true));

            Map<String, Object> body = controller.healthCheck().getBody();

            // healthy, provider, mailSenderAvailable, status
            assertThat(body).containsKeys("healthy", "provider", "mailSenderAvailable", "status");
        }
    }

    // ── GET /v1/api/email-test/test-simple ───────────────────────────────────

    @Nested
    class TestSimpleEmail {

        @Test
        void returns200() {
            when(emailTestService.testSimpleEmail(TEST_EMAIL)).thenReturn(Map.of());

            ResponseEntity<Map<String, Object>> response = controller.testSimpleEmail(TEST_EMAIL);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void returnsServiceResult() {
            Map<String, Object> result = Map.of("success", true, "message", "Simple test email sent successfully");
            when(emailTestService.testSimpleEmail(TEST_EMAIL)).thenReturn(result);

            ResponseEntity<Map<String, Object>> response = controller.testSimpleEmail(TEST_EMAIL);

            assertThat(response.getBody()).isEqualTo(result);
        }

        @Test
        void callsServiceWithCorrectEmail() {
            when(emailTestService.testSimpleEmail(TEST_EMAIL)).thenReturn(Map.of());

            controller.testSimpleEmail(TEST_EMAIL);

            verify(emailTestService).testSimpleEmail(TEST_EMAIL);
        }

        @Test
        void callsServiceOnce() {
            when(emailTestService.testSimpleEmail(TEST_EMAIL)).thenReturn(Map.of());

            controller.testSimpleEmail(TEST_EMAIL);

            verify(emailTestService, times(1)).testSimpleEmail(TEST_EMAIL);
        }
    }
}
