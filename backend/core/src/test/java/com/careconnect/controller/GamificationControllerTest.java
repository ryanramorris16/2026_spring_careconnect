package com.careconnect.controller;

import com.careconnect.model.Achievement;
import com.careconnect.model.UserAchievement;
import com.careconnect.model.XPProgress;
import com.careconnect.service.GamificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/*
 * GamificationController has one constructor-injected dependency (GamificationService).
 * @InjectMocks uses constructor injection since there is a single-arg constructor.
 */
class GamificationControllerTest {

    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private GamificationController controller;

    private static final Long USER_ID = 42L;

    // ── awardXp() ─────────────────────────────────────────────────────────────

    @Test
    void awardXp_returns200_withUpdatedProgress() {
        /*
         * Covers: successful XP award delegation.
         * Body is a plain Map — no branches inside awardXp().
         */
        XPProgress progress = mock(XPProgress.class);
        when(gamificationService.awardXp(USER_ID, 50)).thenReturn(progress);

        Map<String, Object> body = Map.of("userId", USER_ID, "amount", 50);

        ResponseEntity<?> response = controller.awardXp(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(progress);
    }

    // ── getXpProgress() ───────────────────────────────────────────────────────

    @Test
    void getXpProgress_returns401_whenAuthenticationIsNull() {
        /*
         * Covers: authentication == null → short-circuit → 401.
         */
        ResponseEntity<?> response = controller.getXpProgress(USER_ID, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Authentication required");
    }

    @Test
    void getXpProgress_returns401_whenNotAuthenticated() {
        /*
         * Covers: authentication != null but isAuthenticated() == false → 401.
         */
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        ResponseEntity<?> response = controller.getXpProgress(USER_ID, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getXpProgress_returns200_whenProgressFound() {
        /*
         * Covers: authenticated, gamificationService returns non-empty Optional → 200.
         */
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("user@example.com");

        XPProgress progress = mock(XPProgress.class);
        when(gamificationService.getXpProgress(USER_ID)).thenReturn(Optional.of(progress));

        ResponseEntity<?> response = controller.getXpProgress(USER_ID, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(progress);
    }

    @Test
    void getXpProgress_returns404_whenProgressNotFound() {
        /*
         * Covers: authenticated, gamificationService returns empty Optional → 404.
         */
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("user@example.com");

        when(gamificationService.getXpProgress(USER_ID)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getXpProgress(USER_ID, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.valueOf(404));
    }

    // ── getUserAchievements() ─────────────────────────────────────────────────

    @Test
    void getUserAchievements_returns200_withList() {
        /*
         * Covers: single delegation path for getUserAchievements().
         */
        List<UserAchievement> achievements = List.of(mock(UserAchievement.class));
        when(gamificationService.getUserAchievements(USER_ID)).thenReturn(achievements);

        ResponseEntity<List<UserAchievement>> response = controller.getUserAchievements(USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(achievements);
    }

    // ── getAllAchievements() ──────────────────────────────────────────────────

    @Test
    void getAllAchievements_returns200_withList() {
        /*
         * Covers: single delegation path for getAllAchievements().
         */
        List<Achievement> achievements = List.of(mock(Achievement.class));
        when(gamificationService.getAllAchievements()).thenReturn(achievements);

        ResponseEntity<List<Achievement>> response = controller.getAllAchievements();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(achievements);
    }
}
