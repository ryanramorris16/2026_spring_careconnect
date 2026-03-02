package com.careconnect.service;

import com.careconnect.dto.CaregiverRegistration;
import com.careconnect.dto.LoginRequest;
import com.careconnect.dto.PatientRegistration;
import com.careconnect.dto.AddressDto;
import com.careconnect.dto.RegisterRequest;
import com.careconnect.model.Caregiver;
import com.careconnect.model.Patient;
import com.careconnect.model.User;
import com.careconnect.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedConstruction;
import static org.mockito.Mockito.mockConstruction;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletResponse;

// exceptions and web client classes used by new tests
import com.careconnect.exception.AuthenticationException;
import com.careconnect.exception.AppException;
import com.careconnect.exception.OAuthException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// repositories and utility classes used by tests
import com.careconnect.repository.UserRepository;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.CaregiverRepository;
import com.careconnect.repository.FamilyMemberRepository;

import com.careconnect.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.careconnect.service.GamificationService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private AuthService authService;

    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PatientRepository patients;
    @Mock private CaregiverRepository caregivers;
    @Mock private FamilyMemberRepository familyMembers;
    @Mock private JwtTokenProvider jwt;
    @Mock private GamificationService gamificationService;

    @Captor ArgumentCaptor<User> userCaptor;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(authService, "users", userRepository);
        ReflectionTestUtils.setField(authService, "emailService", emailService);
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(authService, "patients", patients);
        ReflectionTestUtils.setField(authService, "caregivers", caregivers);
        ReflectionTestUtils.setField(authService, "familyMembers", familyMembers);
        ReflectionTestUtils.setField(authService, "jwt", jwt);
        ReflectionTestUtils.setField(authService, "gamificationService", gamificationService);
        // Set simple backend url for tests
        ReflectionTestUtils.setField(authService, "backendUrl", "http://localhost:8080");
    }

    @Test
    void register_withInvalidRole_returnsBadRequest() {
        RegisterRequest req = new PatientRegistration();
        req.setEmail("x@example.com");
        req.setPassword("p");
        req.setRole("NO_SUCH_ROLE");

        ResponseEntity<?> resp = authService.register(req);

        assertThat(resp.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map)resp.getBody()).get("error")).isNotNull();
    }

    @Test
    void register_existingUnverified_resendsVerificationEmail() {
        PatientRegistration req = new PatientRegistration();
        req.setEmail("u@example.com");
        req.setPassword("p");
        req.setRole("PATIENT");

        User existing = new User();
        existing.setEmail(req.getEmail());
        existing.setIsVerified(false);
        when(userRepository.findByEmailAndRole(anyString(), any())).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> resp = authService.register(req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(emailService).sendVerificationEmail(eq(existing.getEmail()), anyString());
    }

    @Test
    void register_existingVerified_returnsConflict() {
        PatientRegistration req = new PatientRegistration();
        req.setEmail("v@example.com");
        req.setPassword("p");
        req.setRole("PATIENT");

        User existing = new User();
        existing.setEmail(req.getEmail());
        existing.setIsVerified(true);
        when(userRepository.findByEmailAndRole(anyString(), any())).thenReturn(Optional.of(existing));

        ResponseEntity<?> resp = authService.register(req);

        assertThat(resp.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void register_newPatient_savesPatientAndSendsEmail() {
        PatientRegistration req = new PatientRegistration();
        req.setEmail("new@example.com");
        req.setPassword("p");
        req.setRole("PATIENT");
        req.setFirstName("Joe");
        req.setLastName("Smith");
        // address required by registerRole
        req.setAddress(new AddressDto("123 Main St", "Apt 4", "Anytown", "CA", "12345", "555-1234"));

        when(userRepository.findByEmailAndRole(anyString(), any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(i -> {
            User u = (User) i.getArgument(0);
            u.setId(42L);
            return u;
        });

        ResponseEntity<?> resp = authService.register(req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(patients).save(any(Patient.class));
        verify(emailService).sendVerificationEmail(eq(req.getEmail()), anyString());
    }

    @Test
    void validateUser_success_and_failures() {
        User user = new User();
        user.setEmail("a@b.com");
        user.setPasswordHash("HASH");
        user.setIsVerified(true);
        user.setRole(Role.PATIENT);

        when(userRepository.findByEmailAndRole(eq("a@b.com"), eq(Role.PATIENT))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "HASH")).thenReturn(true);

        Optional<User> ok = authService.validateUser("a@b.com", "pw", "PATIENT");
        assertThat(ok).isPresent();

        // wrong password
        when(passwordEncoder.matches("bad", "HASH")).thenReturn(false);
        Optional<User> bad = authService.validateUser("a@b.com", "bad", "PATIENT");
        assertThat(bad).isEmpty();

        // not verified
        user.setIsVerified(false);
        when(userRepository.findByEmailAndRole(eq("a@b.com"), eq(Role.PATIENT))).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "HASH")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> authService.validateUser("a@b.com", "pw", "PATIENT"));
    }

    @Test
    void verifyToken_success_and_invalid() {
        User u = new User();
        u.setEmail("e@e.com");
        u.setVerificationToken("tok");
        u.setIsVerified(false);

        when(userRepository.findByVerificationToken("tok")).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> ok = authService.verifyToken("tok");
        assertThat(ok.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(u.getIsVerified()).isTrue();
        assertThat(u.getVerificationToken()).isNull();

        ResponseEntity<?> bad = authService.verifyToken("nope");
        assertThat(bad.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void resendVerificationEmail_behaviour() {
        when(userRepository.findByEmail("missing@x.com")).thenReturn(Optional.empty());
        ResponseEntity<?> r1 = authService.resendVerificationEmail("missing@x.com");
        assertThat(((Map)r1.getBody()).get("message")).isNotNull();

        User verified = new User(); verified.setEmail("v@x.com"); verified.setIsVerified(true);
        when(userRepository.findByEmail("v@x.com")).thenReturn(Optional.of(verified));
        ResponseEntity<?> r2 = authService.resendVerificationEmail("v@x.com");
        assertThat(r2.getStatusCode().value()).isEqualTo(400);

        User unverified = new User(); unverified.setEmail("u@x.com"); unverified.setIsVerified(false);
        when(userRepository.findByEmail("u@x.com")).thenReturn(Optional.of(unverified));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ResponseEntity<?> r3 = authService.resendVerificationEmail("u@x.com");
        assertThat(r3.getStatusCode().is2xxSuccessful()).isTrue();
        verify(emailService).sendVerificationEmail(eq("u@x.com"), anyString());
    }

    @Test
    void checkEmailVerificationStatus_variants() {
        when(userRepository.findByEmail("no@one.com")).thenReturn(Optional.empty());
        ResponseEntity<?> r1 = authService.checkEmailVerificationStatus("no@one.com");
        assertThat(((Map)r1.getBody()).get("verified")).isEqualTo(false);

        User u = new User(); u.setEmail("y@x.com"); u.setIsVerified(true);
        when(userRepository.findByEmail("y@x.com")).thenReturn(Optional.of(u));
        ResponseEntity<?> r2 = authService.checkEmailVerificationStatus("y@x.com");
        assertThat(((Map)r2.getBody()).get("verified")).isEqualTo(true);
    }

    @Test
    void loginV2_and_loginOAuth_and_changePassword_and_setupPassword() {
        // loginV2 path
        LoginRequest req = new LoginRequest();
        req.setEmail("me@x.com"); req.setPassword("pw"); req.setRole("PATIENT");

        User u = new User(); u.setId(7L); u.setEmail("me@x.com"); u.setPasswordHash("H"); u.setRole(Role.PATIENT); u.setIsVerified(true); u.setStatus("ACTIVE");
        when(userRepository.findByEmailAndRole("me@x.com", Role.PATIENT)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "H")).thenReturn(true);
        when(jwt.createToken(anyString(), any())).thenReturn("TOK");
        when(patients.findByUserId(7L)).thenReturn(Optional.of(new Patient()));

        HttpServletResponse resp = mock(HttpServletResponse.class);
        var lr = authService.loginV2(req, resp);
        assertThat(lr.token()).isEqualTo("TOK");
        verify(gamificationService).unlockAchievement(7L, "First Login", 50);
        verify(resp).addHeader(anyString(), anyString());

        // loginOAuth
        when(userRepository.findByEmail("me@x.com")).thenReturn(Optional.of(u));
        var lro = authService.loginOAuth("me@x.com", resp);
        assertThat(lro.token()).isEqualTo("TOK");

        // changePassword - wrong current
        when(userRepository.findByEmail("me@x.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong", "H")).thenReturn(false);
        var bad = authService.changePassword("me@x.com", "wrong", "new");
        assertThat(bad.getStatusCode().value()).isEqualTo(400);

        // changePassword - success
        when(passwordEncoder.matches("cur", "H")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("NEW");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var ok = authService.changePassword("me@x.com", "cur", "new");
        assertThat(ok.getStatusCode().is2xxSuccessful()).isTrue();

        // setupPassword - invalid token
        when(userRepository.findByVerificationToken("bad")).thenReturn(Optional.empty());
        var sbad = authService.setupPassword("bad", "np");
        assertThat(sbad.getStatusCode().value()).isEqualTo(400);

        // setupPassword - success
        User utok = new User(); utok.setVerificationToken("ok"); utok.setEmail("x@x.com");
        when(userRepository.findByVerificationToken("ok")).thenReturn(Optional.of(utok));
        when(passwordEncoder.encode("np")).thenReturn("EP");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var sok = authService.setupPassword("ok", "np");
        assertThat(sok.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void loginV2_errorPaths_and_streakUnlock() {
        LoginRequest req = new LoginRequest();
        req.setEmail("me@x.com"); req.setPassword("pw"); req.setRole("PATIENT");

        User u = new User();
        u.setId(7L);
        u.setEmail("me@x.com");
        u.setPasswordHash("H");
        u.setRole(Role.PATIENT);
        u.setIsVerified(true);
        u.setStatus("ACTIVE");
        u.setLastLoginDate(LocalDate.now().minusDays(1));
        u.setLoginStreak(4);

        when(userRepository.findByEmailAndRole("me@x.com", Role.PATIENT)).thenReturn(Optional.of(u));
        // accept any password but only treat "pw" as valid, so subsequent changes don't trigger stubbing complaints
        lenient().when(passwordEncoder.matches(anyString(), eq("H")))
                .thenAnswer(invocation -> "pw".equals(invocation.getArgument(0)));
        when(jwt.createToken(anyString(), any())).thenReturn("TOK");
        when(patients.findByUserId(7L)).thenReturn(Optional.of(new Patient()));

        HttpServletResponse resp = mock(HttpServletResponse.class);
        var lr2 = authService.loginV2(req, resp);
        assertThat(lr2.token()).isEqualTo("TOK");
        verify(gamificationService).unlockAchievement(7L, "First Login", 50);
        verify(gamificationService).unlockAchievement(7L, "5-Day Streak", 100);

        // invalid credentials - update request password too
        req.setPassword("pw2");
        // encoder now returns false for any password other than "pw"
        assertThrows(AuthenticationException.class, () -> authService.loginV2(req, resp));

        // invalid role string
        req.setRole("BADROLE");
        assertThrows(AuthenticationException.class, () -> authService.loginV2(req, resp));

        // inactive account (status rather than active flag)
        u.setIsVerified(true);
        u.setStatus("INACTIVE");
        req.setRole("PATIENT");
        assertThrows(AuthenticationException.class, () -> authService.loginV2(req, resp));
    }

    @Test
    void loginOAuth_errorCases() {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(userRepository.findByEmail("not@here.com")).thenReturn(Optional.empty());
        assertThrows(AuthenticationException.class, () -> authService.loginOAuth("not@here.com", resp));

        User u = new User(); u.setEmail("a@b"); u.setStatus("INACTIVE");
        when(userRepository.findByEmail("a@b")).thenReturn(Optional.of(u));
        assertThrows(AuthenticationException.class, () -> authService.loginOAuth("a@b", resp));
    }

    @Test
    void changePassword_internalError() {
        User u = new User(); u.setEmail("e@e.com"); u.setPasswordHash("H");
        when(userRepository.findByEmail("e@e.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("cur", "H")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("N");
        when(userRepository.save(any())).thenThrow(new RuntimeException("db fail"));

        ResponseEntity<?> r = authService.changePassword("e@e.com", "cur", "new");
        assertThat(r.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void unsupportedRegisterRole_throwsAppException() {
        RegisterRequest req = new RegisterRequest() {};
        req.setEmail("x@x.com");
        req.setPassword("p");
        req.setRole("ADMIN");
        when(userRepository.findByEmailAndRole(anyString(), any())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> authService.register(req));
    }

    @Test
    void validateUser_invalidRole_returnsEmpty() {
        Optional<User> res = authService.validateUser("a@b.com", "pw", "NOPE");
        assertThat(res).isEmpty();
    }

    @Test
    void googleOAuthHelper_methods() throws Exception {
        AuthService spy = spy(authService);
        ReflectionTestUtils.setField(spy, "googleTokenUri", "http://token");
        ReflectionTestUtils.setField(spy, "googleUserInfoUri", "http://userinfo");
        ReflectionTestUtils.setField(spy, "googleClientId", "cid");
        ReflectionTestUtils.setField(spy, "googleClientSecret", "secret");
        ReflectionTestUtils.setField(spy, "backendUrl", "http://localhost");

        // prepare errors for each REST call
        HttpClientErrorException badGrant = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request");
        HttpClientErrorException tokenErr = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);

        // intercept every new RestTemplate so we can simulate both token and userinfo failures
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) -> {
            when(mock.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class)))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0);
                    if (url.contains("userinfo")) {
                        throw tokenErr;
                    } else {
                        throw badGrant;
                    }
                });
        })) {
            // first constructed template is used by exchangeCodeForToken
            assertThrows(OAuthException.class, () -> ReflectionTestUtils.invokeMethod(spy, "exchangeCodeForToken", "code"));
            // second constructed template is used by getUserInfoFromGoogle
            assertThrows(OAuthException.class, () -> ReflectionTestUtils.invokeMethod(spy, "getUserInfoFromGoogle", "tok"));
        }
    }

    @Test
    void buildGoogleOAuthUrl_and_validateGoogleToken() {
        ReflectionTestUtils.setField(authService, "googleAuthUri", "https://auth.example/authorize");
        ReflectionTestUtils.setField(authService, "googleClientId", "CLIENT123");
        ReflectionTestUtils.setField(authService, "backendUrl", "http://localhost:8080");

        String u = authService.buildGoogleOAuthUrl();
        assertThat(u).contains("CLIENT123");
        assertThat(u).contains("authorize");

        assertThrows(UnsupportedOperationException.class, () -> authService.validateGoogleToken("x"));
    }

}
