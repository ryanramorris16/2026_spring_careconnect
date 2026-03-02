package com.careconnect.service;

import com.careconnect.model.PasswordResetToken;
import com.careconnect.model.User;
import com.careconnect.repository.PasswordResetTokenRepo;
import com.careconnect.repository.UserRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserPasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepo passwordResetTokenRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserPasswordService userPasswordService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("oldPassword");
        user.setPasswordHash("oldHash");
        user.setIsVerified(false);
        user.setVerificationToken(null);
    }

    // ========== resetPasswordWithToken - verification token path ==========

    @Test
    @DisplayName("resetPasswordWithToken_verificationToken_unverifiedUser_shouldSetPasswordAndVerify")
    void resetPasswordWithToken_verificationToken_unverifiedUser_shouldSetPasswordAndVerify() {
        user.setVerificationToken("verify-token-abc");
        user.setIsVerified(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        userPasswordService.resetPasswordWithToken("test@example.com", "verify-token-abc", "newPassword123");

        assertEquals("encodedNewPassword", user.getPassword());
        assertEquals("encodedNewPassword", user.getPasswordHash());
        assertTrue(user.getIsVerified());
        assertNull(user.getVerificationToken());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("resetPasswordWithToken_verificationToken_alreadyVerifiedUser_shouldThrowIllegalArgumentException")
    void resetPasswordWithToken_verificationToken_alreadyVerifiedUser_shouldThrowIllegalArgumentException() {
        user.setVerificationToken("verify-token-abc");
        user.setIsVerified(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userPasswordService.resetPasswordWithToken("test@example.com", "verify-token-abc", "newPassword123"));

        assertEquals("Password already set up for this account", ex.getMessage());
    }

    @Test
    @DisplayName("resetPasswordWithToken_userNotFound_shouldThrowIllegalArgumentException")
    void resetPasswordWithToken_userNotFound_shouldThrowIllegalArgumentException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userPasswordService.resetPasswordWithToken("unknown@example.com", "token", "newPass"));

        assertEquals("User not found", ex.getMessage());
    }

    // ========== resetPasswordWithToken - base64 encoded user ID path ==========

    @Test
    @DisplayName("resetPasswordWithToken_base64EncodedUserId_matchingUser_shouldResetPassword")
    void resetPasswordWithToken_base64EncodedUserId_matchingUser_shouldResetPassword() {
        String base64Token = Base64.getUrlEncoder().encodeToString("1".getBytes());
        user.setVerificationToken(null); // Not a verification token
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        userPasswordService.resetPasswordWithToken("test@example.com", base64Token, "newPassword123");

        assertEquals("encodedNewPassword", user.getPassword());
        assertEquals("encodedNewPassword", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("resetPasswordWithToken_base64EncodedUserId_nonMatchingUser_shouldFallToLegacyFlow")
    void resetPasswordWithToken_base64EncodedUserId_nonMatchingUser_shouldFallToLegacyFlow() {
        // Encode a different user ID
        String base64Token = Base64.getUrlEncoder().encodeToString("999".getBytes());
        user.setVerificationToken(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        String tokenHash = DigestUtils.sha256Hex(base64Token);

        // Set up the legacy flow to succeed
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        resetToken.setUsed(false);

        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        userPasswordService.resetPasswordWithToken("test@example.com", base64Token, "newPassword123");

        verify(userRepository).save(user);
        verify(passwordResetTokenRepo).save(resetToken);
        assertTrue(resetToken.isUsed());
    }

    // ========== resetPasswordWithToken - legacy flow ==========

    @Test
    @DisplayName("resetPasswordWithToken_legacyFlow_validToken_sameUser_shouldResetPassword")
    void resetPasswordWithToken_legacyFlow_validToken_sameUser_shouldResetPassword() {
        String rawToken = "legacy-reset-token";
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        user.setVerificationToken(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        resetToken.setUsed(false);

        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        userPasswordService.resetPasswordWithToken("test@example.com", rawToken, "newPassword123");

        assertEquals("encodedNewPassword", user.getPassword());
        assertEquals("encodedNewPassword", user.getPasswordHash());
        assertTrue(resetToken.isUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepo).save(resetToken);
    }

    @Test
    @DisplayName("resetPasswordWithToken_legacyFlow_invalidOrExpiredToken_shouldThrowIllegalArgumentException")
    void resetPasswordWithToken_legacyFlow_invalidOrExpiredToken_shouldThrowIllegalArgumentException() {
        String rawToken = "expired-token";
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        user.setVerificationToken(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.empty());
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.empty());
        when(passwordResetTokenRepo.findAll()).thenReturn(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userPasswordService.resetPasswordWithToken("test@example.com", rawToken, "newPassword123"));

        assertEquals("Invalid or expired reset token", ex.getMessage());
    }

    @Test
    @DisplayName("resetPasswordWithToken_legacyFlow_tokenBelongsToDifferentUser_shouldThrowIllegalArgumentException")
    void resetPasswordWithToken_legacyFlow_tokenBelongsToDifferentUser_shouldThrowIllegalArgumentException() {
        String rawToken = "other-user-token";
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        user.setVerificationToken(null);

        User otherUser = new User();
        otherUser.setId(999L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(otherUser);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        resetToken.setUsed(false);

        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.of(resetToken));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userPasswordService.resetPasswordWithToken("test@example.com", rawToken, "newPassword123"));

        assertEquals("Reset token does not belong to this user", ex.getMessage());
    }

    @Test
    @DisplayName("resetPasswordWithToken_legacyFlow_tokenFoundButExpired_logsExpired_shouldThrow")
    void resetPasswordWithToken_legacyFlow_tokenFoundButExpired_logsExpired_shouldThrow() {
        String rawToken = "expired-token-debug";
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        user.setVerificationToken(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setUser(user);
        expiredToken.setTokenHash(tokenHash);
        expiredToken.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS)); // expired
        expiredToken.setUsed(false);

        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                userPasswordService.resetPasswordWithToken("test@example.com", rawToken, "newPassword123"));
    }

    @Test
    @DisplayName("resetPasswordWithToken_legacyFlow_tokenFoundButUsed_logsUsed_shouldThrow")
    void resetPasswordWithToken_legacyFlow_tokenFoundButUsed_logsUsed_shouldThrow() {
        String rawToken = "used-token-debug";
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        user.setVerificationToken(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        PasswordResetToken usedToken = new PasswordResetToken();
        usedToken.setUser(user);
        usedToken.setTokenHash(tokenHash);
        usedToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        usedToken.setUsed(true);

        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.of(usedToken));
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                userPasswordService.resetPasswordWithToken("test@example.com", rawToken, "newPassword123"));
    }

    @Test
    @DisplayName("resetPasswordWithToken_legacyFlow_tokenNotInDb_runsForEachOnEmpty_shouldThrow")
    void resetPasswordWithToken_legacyFlow_tokenNotInDb_runsForEachOnEmpty_shouldThrow() {
        String rawToken = "non-existent-token";
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        user.setVerificationToken(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.empty());
        when(passwordResetTokenRepo.findAll()).thenReturn(Collections.emptyList());
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                userPasswordService.resetPasswordWithToken("test@example.com", rawToken, "newPassword123"));
    }

    @Test
    @DisplayName("resetPasswordWithToken_legacyFlow_tokenNotInDb_findAllReturnsTokensForOtherUser_shouldThrow")
    void resetPasswordWithToken_legacyFlow_tokenNotInDb_findAllReturnsTokensForOtherUser_shouldThrow() {
        String rawToken = "missing-token";
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        user.setVerificationToken(null);

        User otherUser = new User();
        otherUser.setId(999L);

        PasswordResetToken otherToken = new PasswordResetToken();
        otherToken.setUser(otherUser);
        otherToken.setTokenHash("otherhash");
        otherToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        otherToken.setUsed(false);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.empty());
        when(passwordResetTokenRepo.findAll()).thenReturn(List.of(otherToken));
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                userPasswordService.resetPasswordWithToken("test@example.com", rawToken, "newPassword123"));
    }

    @Test
    @DisplayName("resetPasswordWithToken_legacyFlow_tokenNotInDb_findAllReturnsTokensForSameUser_shouldThrow")
    void resetPasswordWithToken_legacyFlow_tokenNotInDb_findAllReturnsTokensForSameUser_shouldThrow() {
        String rawToken = "missing-token-same-user";
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        user.setVerificationToken(null);

        PasswordResetToken sameUserToken = new PasswordResetToken();
        sameUserToken.setUser(user);
        sameUserToken.setTokenHash("differenthash");
        sameUserToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        sameUserToken.setUsed(false);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.empty());
        when(passwordResetTokenRepo.findAll()).thenReturn(List.of(sameUserToken));
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                userPasswordService.resetPasswordWithToken("test@example.com", rawToken, "newPassword123"));
    }

    // ========== resetPasswordWithToken - verification token does not match ==========

    @Test
    @DisplayName("resetPasswordWithToken_verificationTokenDoesNotMatch_shouldFallToBase64Flow")
    void resetPasswordWithToken_verificationTokenDoesNotMatch_shouldFallToBase64Flow() {
        user.setVerificationToken("different-verify-token");
        String base64Token = Base64.getUrlEncoder().encodeToString("1".getBytes());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        userPasswordService.resetPasswordWithToken("test@example.com", base64Token, "newPassword123");

        assertEquals("encodedNewPassword", user.getPassword());
        verify(userRepository).save(user);
    }

    // ========== setupPasswordWithVerificationToken ==========

    @Test
    @DisplayName("setupPasswordWithVerificationToken_validToken_unverified_shouldSetPassword")
    void setupPasswordWithVerificationToken_validToken_unverified_shouldSetPassword() {
        user.setVerificationToken("setup-token-xyz");
        user.setIsVerified(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        userPasswordService.setupPasswordWithVerificationToken("test@example.com", "setup-token-xyz", "newPassword123");

        assertEquals("encodedNewPassword", user.getPassword());
        assertEquals("encodedNewPassword", user.getPasswordHash());
        assertTrue(user.getIsVerified());
        assertNull(user.getVerificationToken());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("setupPasswordWithVerificationToken_userNotFound_shouldThrowRuntimeException")
    void setupPasswordWithVerificationToken_userNotFound_shouldThrowRuntimeException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userPasswordService.setupPasswordWithVerificationToken("unknown@example.com", "token", "newPass"));

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    @DisplayName("setupPasswordWithVerificationToken_nullVerificationToken_shouldThrowRuntimeException")
    void setupPasswordWithVerificationToken_nullVerificationToken_shouldThrowRuntimeException() {
        user.setVerificationToken(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userPasswordService.setupPasswordWithVerificationToken("test@example.com", "any-token", "newPass"));

        assertEquals("Invalid verification token", ex.getMessage());
    }

    @Test
    @DisplayName("setupPasswordWithVerificationToken_mismatchedToken_shouldThrowRuntimeException")
    void setupPasswordWithVerificationToken_mismatchedToken_shouldThrowRuntimeException() {
        user.setVerificationToken("correct-token");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userPasswordService.setupPasswordWithVerificationToken("test@example.com", "wrong-token", "newPass"));

        assertEquals("Invalid verification token", ex.getMessage());
    }

    @Test
    @DisplayName("setupPasswordWithVerificationToken_alreadyVerified_shouldThrowRuntimeException")
    void setupPasswordWithVerificationToken_alreadyVerified_shouldThrowRuntimeException() {
        user.setVerificationToken("setup-token-xyz");
        user.setIsVerified(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userPasswordService.setupPasswordWithVerificationToken("test@example.com", "setup-token-xyz", "newPass"));

        assertEquals("Password already set up for this account", ex.getMessage());
    }

    // ========== resetPasswordWithToken - base64 decode throws non-parseable ==========

    @Test
    @DisplayName("resetPasswordWithToken_invalidBase64Token_shouldFallToLegacyFlow")
    void resetPasswordWithToken_invalidBase64Token_shouldFallToLegacyFlow() {
        // A token that is not valid base64 will throw an exception in the try block,
        // causing it to fall through to the legacy flow.
        String rawToken = "not!!!base64===token";
        String tokenHash = DigestUtils.sha256Hex(rawToken);
        user.setVerificationToken(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        resetToken.setUsed(false);

        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        userPasswordService.resetPasswordWithToken("test@example.com", rawToken, "newPassword123");

        assertEquals("encodedNewPassword", user.getPassword());
        assertTrue(resetToken.isUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepo).save(resetToken);
    }

    @Test
    @DisplayName("resetPasswordWithToken_base64DecodesToNonNumeric_shouldFallToLegacyFlow")
    void resetPasswordWithToken_base64DecodesToNonNumeric_shouldFallToLegacyFlow() {
        // A token that decodes to a non-numeric string will cause NumberFormatException,
        // which is caught and falls through to legacy flow.
        String base64Token = Base64.getUrlEncoder().encodeToString("not_a_number".getBytes());
        String tokenHash = DigestUtils.sha256Hex(base64Token);
        user.setVerificationToken(null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        resetToken.setUsed(false);

        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        userPasswordService.resetPasswordWithToken("test@example.com", base64Token, "newPassword123");

        assertEquals("encodedNewPassword", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("resetPasswordWithToken_nullVerificationToken_notBase64_shouldUseTokenHashFlow")
    void resetPasswordWithToken_nullVerificationToken_notBase64_shouldUseTokenHashFlow() {
        user.setVerificationToken(null);
        String rawToken = "simple-raw-token";
        String tokenHash = DigestUtils.sha256Hex(rawToken);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        resetToken.setUsed(false);

        when(passwordResetTokenRepo.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordResetTokenRepo.findValid(eq(tokenHash), any(Instant.class))).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPass")).thenReturn("encoded");

        userPasswordService.resetPasswordWithToken("test@example.com", rawToken, "newPass");

        assertEquals("encoded", user.getPassword());
        assertEquals("encoded", user.getPasswordHash());
        assertTrue(resetToken.isUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepo).save(resetToken);
    }
}
