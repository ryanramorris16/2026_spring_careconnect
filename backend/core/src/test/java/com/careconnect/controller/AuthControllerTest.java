package com.careconnect.controller;

import com.careconnect.dto.*;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.UserRepository;
import com.careconnect.security.JwtTokenProvider;
import com.careconnect.security.TokenHashService;
import com.careconnect.service.AlexaCodeStoreService;
import com.careconnect.service.AuthService;
import com.careconnect.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link AuthController}, covering the HTTP layer of all
 * authentication and SSO endpoints.
 *
 * <p><b>Why @WebMvcTest + MockMvc?</b><br>
 * {@code @WebMvcTest} spins up only the Spring MVC slice (controllers, filters,
 * argument resolvers) without loading a full application context or a real
 * database.  This makes the tests fast and focused: they verify that the
 * controller routes requests to the correct service methods, applies the right
 * HTTP status codes, and serialises/deserialises JSON properly — without caring
 * about the actual business logic inside the services.
 *
 * <p>All service and repository collaborators are replaced with Mockito mocks
 * via {@code @MockBean} so that each test exercises only the controller layer
 * in isolation.  Security filters are disabled with
 * {@code @AutoConfigureMockMvc(addFilters = false)} so that most tests can
 * focus on happy-path or input-validation behaviour; the tests that do require
 * auth enforcement (e.g. missing JWT cookie) do so by omitting the cookie
 * rather than relying on the full security filter chain.
 *
 * <p>Test properties supply the minimum configuration values required by beans
 * that are wired into the controller context (frontend URL, Alexa OAuth
 * credentials).
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "frontend.base-url=http://localhost:3000",
        "alexa.oauth.client-id=test-client-id",
        "alexa.oauth.client-secret=test-client-secret"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Mocked collaborators ---
    // Each bean below is replaced with a Mockito stub so the controller can be
    // instantiated without real infrastructure (DB, JWT signing keys, etc.).

    @MockBean
    private AuthService authService;

    @MockBean
    private PasswordResetService reset;

    @MockBean
    private JwtTokenProvider jwt;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PatientRepository patientRepository;

    @MockBean
    private TokenHashService tokenHashService;

    @MockBean
    private AlexaCodeStoreService alexaCodeStore;

    // ==========================================
    // REGISTER
    // ==========================================

    /**
     * Verifies that POST /v1/api/auth/register returns HTTP 200 when the
     * {@link AuthService#register} call succeeds.
     *
     * <p>MockMvc is used so we can assert the HTTP status without starting a
     * real server.  The service is stubbed to return a 200 response, allowing
     * the test to confirm that the controller correctly forwards the result to
     * the caller.
     */
    @Test
    void shouldRegisterUserSuccessfully() throws Exception {

        PatientRegistration request = new PatientRegistration();
        request.setEmail("test@test.com");
        request.setPassword("password");
        request.setName("Test");
        request.setRole("PATIENT");

        when(authService.register(any(RegisterRequest.class)))
                .thenAnswer(inv -> ResponseEntity.ok(Map.of("message", "success")));

        mockMvc.perform(post("/v1/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ==========================================
    // LOGIN
    // ==========================================

    /**
     * Verifies that POST /v1/api/auth/login returns HTTP 200 and includes the
     * JWT token in the JSON response body on a successful login.
     *
     * <p>The {@link AuthService#loginV2} method is stubbed to return a
     * {@link LoginResponse} containing a known token value.  The test then
     * asserts both the status code and the JSON field {@code $.token}, ensuring
     * that the controller serialises the response object correctly.
     */
    @Test
    void shouldLoginSuccessfully() throws Exception {

        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setPassword("password");

        LoginResponse response = LoginResponse.builder()
                .token("jwt-token")
                .build();

        when(authService.loginV2(any(),any()))
                .thenReturn(response);

        mockMvc.perform(post("/v1/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    // ==========================================
    // RESEND VERIFICATION - BAD REQUEST
    // ==========================================

    /**
     * Verifies that POST /v1/api/auth/resend-verification returns HTTP 400
     * when the request body does not contain a required {@code email} field.
     *
     * <p>Input validation is enforced by Bean Validation annotations on the
     * request DTO.  Sending an empty JSON object ({@code {}}) triggers a
     * constraint violation, and the controller (or a Spring MVC exception
     * handler) should respond with 400 Bad Request.  This test confirms that
     * the validation layer is correctly wired — no service stub is needed
     * because the request should be rejected before reaching the service.
     */
    @Test
    void resendVerificationShouldReturnBadRequestIfEmailMissing() throws Exception {

        mockMvc.perform(post("/v1/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // CHECK VERIFICATION - BAD REQUEST
    // ==========================================

    /**
     * Verifies that GET /v1/api/auth/check-verification returns HTTP 400 when
     * the {@code email} query parameter is present but empty.
     *
     * <p>An empty string is semantically equivalent to a missing email address.
     * This test ensures that the controller (or its validation layer) rejects
     * blank values rather than propagating them downstream, protecting the
     * service from acting on invalid input.
     */
    @Test
    void checkVerificationShouldReturnBadRequestIfEmailMissing() throws Exception {

        mockMvc.perform(get("/v1/api/auth/check-verification")
                        .param("email", ""))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // FORGOT PASSWORD
    // ==========================================

    /**
     * Verifies that POST /v1/api/auth/password/forgot returns HTTP 200
     * regardless of whether the supplied email belongs to a real account.
     *
     * <p>Returning 200 for both known and unknown addresses is an intentional
     * security design: it prevents user enumeration by not revealing whether an
     * account exists.  No service stub is needed here because the default
     * Mockito behaviour (returning {@code null} / void) is sufficient to
     * confirm the endpoint accepts the request and returns the expected status.
     */
    @Test
    void forgotPasswordShouldReturnOk() throws Exception {

        mockMvc.perform(post("/v1/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\"}"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // CHANGE PASSWORD - NO TOKEN
    // ==========================================

    /**
     * Verifies that POST /v1/api/auth/password/change returns HTTP 401 when
     * no authentication token is provided.
     *
     * <p>Changing a password is a privileged operation that must require a
     * valid authenticated session.  By omitting the JWT cookie/header this
     * test confirms that the controller guards the endpoint correctly and does
     * not allow unauthenticated callers to modify credentials.
     */
    @Test
    void changePasswordShouldReturnUnauthorizedIfNoToken() throws Exception {

        ChangePasswordRequest req =
                new ChangePasswordRequest("old","new");

        mockMvc.perform(post("/v1/api/auth/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // VALIDATE RESET TOKEN
    // ==========================================

    /**
     * Verifies that GET /v1/api/auth/password/reset returns HTTP 200 when the
     * supplied reset token is valid.
     *
     * <p>{@link PasswordResetService#isTokenValid} is stubbed to return
     * {@code true} for the token {@code "abc"}, simulating a token that has not
     * yet expired.  The test confirms that the controller correctly delegates
     * validation to the service and maps a valid result to a 200 response,
     * which the frontend uses to decide whether to render the reset-password
     * form.
     */
    @Test
    void validateResetTokenShouldReturnOkIfValid() throws Exception {

        when(reset.isTokenValid("abc")).thenReturn(true);

        mockMvc.perform(get("/v1/api/auth/password/reset")
                        .param("token","abc"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // ALEXA CODE - MISSING TOKEN
    // ==========================================

    /**
     * Verifies that POST /v1/api/auth/sso/alexa/code returns HTTP 401 when no
     * authentication cookie is present.
     *
     * <p>The Alexa SSO code generation endpoint must be restricted to
     * authenticated users.  Sending the request without an {@code AUTH} cookie
     * confirms that the controller rejects unauthenticated callers before
     * attempting to generate or return a code.
     */
    @Test
    void generateAlexaCodeShouldReturnUnauthorizedIfMissingToken() throws Exception {

        mockMvc.perform(post("/v1/api/auth/sso/alexa/code"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // ALEXA CODE - VALID TOKEN
    // ==========================================

    /**
     * Verifies that POST /v1/api/auth/sso/alexa/code returns HTTP 200 and a
     * JSON body containing a {@code code} field when a valid {@code AUTH}
     * cookie is present.
     *
     * <p>The JWT provider is stubbed to confirm the token is valid and to
     * resolve the associated email address.  The Alexa code store is stubbed
     * to return a predictable code string.  The test then asserts that the
     * response body contains the {@code $.code} field, confirming end-to-end
     * that the controller correctly reads the cookie, validates the token,
     * delegates to the code store, and wraps the result in JSON.
     */
    @Test
    void generateAlexaCodeShouldReturnCodeIfValidToken() throws Exception {

        when(jwt.getEmailFromToken("token")).thenReturn("test@test.com");
        when(jwt.validateToken("token")).thenReturn(true);
        when(alexaCodeStore.generateCode("token")).thenReturn("test-alexa-code");

        mockMvc.perform(post("/v1/api/auth/sso/alexa/code")
                        .cookie(new Cookie("AUTH","token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }
}
