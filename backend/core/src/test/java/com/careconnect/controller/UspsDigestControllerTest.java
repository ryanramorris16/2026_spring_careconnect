package com.careconnect.controller;

import com.careconnect.model.USPSDigest;
import com.careconnect.security.AuthorizationService;
import com.careconnect.service.USPSDigestService;
import com.careconnect.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UspsDigestControllerTest {

    @Mock
    private USPSDigestService uspsDigestService;

    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private com.careconnect.controller.UspsDigestController controller;

    private USPSDigest digest() throws Exception {
        return new USPSDigest(null, List.of(), List.of());
    }

    // ─── getLatestDigest ──────────────────────────────────────────────────────

    @Test
    void getLatestDigest_dateProvided_callsDigestForDate_returnsOk() throws Exception {
        LocalDate date = LocalDate.of(2025, 6, 1);
        USPSDigest d = digest();
        when(uspsDigestService.digestForDate("user1", date)).thenReturn(Optional.of(d));

        ResponseEntity<USPSDigest> response = controller.getLatestDigest("user1", date);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(d);
        verify(uspsDigestService).digestForDate("user1", date);
    }

    @Test
    void getLatestDigest_dateNull_callsLatestForUser_returnsOk() throws Exception {
        USPSDigest d = digest();
        when(uspsDigestService.latestForUser("demo-user")).thenReturn(Optional.of(d));

        ResponseEntity<USPSDigest> response = controller.getLatestDigest("demo-user", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(d);
        verify(uspsDigestService).latestForUser("demo-user");
    }

    @Test
    void getLatestDigest_notFound_returnsNoContent() throws Exception {
        when(uspsDigestService.latestForUser("demo-user")).thenReturn(Optional.empty());

        ResponseEntity<USPSDigest> response = controller.getLatestDigest("demo-user", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ─── search ───────────────────────────────────────────────────────────────

    @Test
    void search_returnsOkWithResults() throws Exception {
        List<Map<String, Object>> results = List.of(Map.of("key", "value"));
        when(uspsDigestService.search("user1", "invoice")).thenReturn(results);

        ResponseEntity<List<Map<String, Object>>> response = controller.search("user1", "invoice");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(results);
        verify(uspsDigestService).search("user1", "invoice");
    }

    // ─── clearCache ───────────────────────────────────────────────────────────

    @Test
    void clearCache_returnsOkWithMessage() throws Exception {
        doNothing().when(uspsDigestService).clearCacheForUser("user1");

        ResponseEntity<String> response = controller.clearCache("user1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("user1");
        verify(uspsDigestService).clearCacheForUser("user1");
    }
}
