package com.careconnect.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthExceptionHandlerTest {

    @InjectMocks
    private AuthExceptionHandler handler;

    // ─── handleAuthenticationException() ─────────────────────────────────────

    @Test
    void handleAuthenticationException_returns401WithErrorBody() {
        AuthenticationException ex = new AuthenticationException("Invalid credentials");

        ResponseEntity<Object> response = handler.handleAuthenticationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "Invalid credentials");
    }

    @Test
    void handleAuthenticationException_differentMessage_returnsCorrectBody() {
        AuthenticationException ex = new AuthenticationException("Token expired");

        ResponseEntity<Object> response = handler.handleAuthenticationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "Token expired");
    }

    // ─── handleRegistrationException() ───────────────────────────────────────

    @Test
    void handleRegistrationException_returns409WithErrorBody() {
        RegistrationException ex = new RegistrationException("Email already in use");

        ResponseEntity<Object> response = handler.handleRegistrationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "Email already in use");
    }

    @Test
    void handleRegistrationException_differentMessage_returnsCorrectBody() {
        RegistrationException ex = new RegistrationException("Username taken");

        ResponseEntity<Object> response = handler.handleRegistrationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "Username taken");
    }
}
