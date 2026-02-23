package com.careconnect.controller;

import com.careconnect.model.EmailCredential;
import com.careconnect.repository.EmailCredentialRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailCredentialControllerTest {

    @Mock
    private EmailCredentialRepository credRepo;

    @InjectMocks
    private EmailCredentialController controller;

    // ── shared constants ──────────────────────────────────────────────────────

    private static final String USER_ID = "user-123";

    // ── shared helpers ────────────────────────────────────────────────────────

    private EmailCredential credentialWithToken(String accessToken) {
        EmailCredential cred = new EmailCredential();
        cred.setUserId(USER_ID);
        cred.setProvider(EmailCredential.Provider.GMAIL);
        cred.setAccessTokenEnc(accessToken);
        return cred;
    }

    // ── GET /email-credentials/status ─────────────────────────────────────────

    @Nested
    class GetConnectionStatus {

        @Test
        void returnsTrue_whenCredentialExistsWithValidAccessToken() {
            EmailCredential cred = credentialWithToken("valid-token");
            when(credRepo.findFirstByUserIdAndProviderOrderByIdDesc(USER_ID, EmailCredential.Provider.GMAIL))
                    .thenReturn(Optional.of(cred));

            ResponseEntity<Boolean> response = controller.getConnectionStatus(USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isTrue();
        }

        @Test
        void returnsFalse_whenNoCredentialFound() {
            when(credRepo.findFirstByUserIdAndProviderOrderByIdDesc(USER_ID, EmailCredential.Provider.GMAIL))
                    .thenReturn(Optional.empty());

            ResponseEntity<Boolean> response = controller.getConnectionStatus(USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isFalse();
        }

        @Test
        void returnsFalse_whenAccessTokenIsNull() {
            EmailCredential cred = credentialWithToken(null);
            when(credRepo.findFirstByUserIdAndProviderOrderByIdDesc(USER_ID, EmailCredential.Provider.GMAIL))
                    .thenReturn(Optional.of(cred));

            ResponseEntity<Boolean> response = controller.getConnectionStatus(USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isFalse();
        }

        @Test
        void returnsFalse_whenAccessTokenIsEmpty() {
            EmailCredential cred = credentialWithToken("");
            when(credRepo.findFirstByUserIdAndProviderOrderByIdDesc(USER_ID, EmailCredential.Provider.GMAIL))
                    .thenReturn(Optional.of(cred));

            ResponseEntity<Boolean> response = controller.getConnectionStatus(USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isFalse();
        }

        @Test
        void alwaysQueriesGmailProvider() {
            when(credRepo.findFirstByUserIdAndProviderOrderByIdDesc(USER_ID, EmailCredential.Provider.GMAIL))
                    .thenReturn(Optional.empty());

            controller.getConnectionStatus(USER_ID);

            verify(credRepo, times(1))
                    .findFirstByUserIdAndProviderOrderByIdDesc(USER_ID, EmailCredential.Provider.GMAIL);
            verify(credRepo, never())
                    .findFirstByUserIdAndProviderOrderByIdDesc(anyString(), eq(EmailCredential.Provider.OUTLOOK));
        }

        @Test
        void passesUserIdToRepository() {
            String specificUserId = "specific-user-456";
            when(credRepo.findFirstByUserIdAndProviderOrderByIdDesc(specificUserId, EmailCredential.Provider.GMAIL))
                    .thenReturn(Optional.empty());

            controller.getConnectionStatus(specificUserId);

            verify(credRepo).findFirstByUserIdAndProviderOrderByIdDesc(specificUserId, EmailCredential.Provider.GMAIL);
        }

        @Test
        void returnsTrue_whenAccessTokenIsWhitespace() {
            // Whitespace is non-empty, so the filter passes and result is true
            EmailCredential cred = credentialWithToken("   ");
            when(credRepo.findFirstByUserIdAndProviderOrderByIdDesc(USER_ID, EmailCredential.Provider.GMAIL))
                    .thenReturn(Optional.of(cred));

            ResponseEntity<Boolean> response = controller.getConnectionStatus(USER_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isTrue();
        }

        @Test
        void responseBodyIsNeverNull() {
            when(credRepo.findFirstByUserIdAndProviderOrderByIdDesc(USER_ID, EmailCredential.Provider.GMAIL))
                    .thenReturn(Optional.empty());

            ResponseEntity<Boolean> response = controller.getConnectionStatus(USER_ID);

            assertThat(response.getBody()).isNotNull();
        }
    }
}
