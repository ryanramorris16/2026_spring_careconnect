package com.careconnect.controller;

import com.careconnect.model.Mood;
import com.careconnect.service.MoodService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoodControllerTest {

    @Mock private MoodService moodService;

    @InjectMocks
    private MoodController controller;

    private static final Long USER_ID      = 1L;
    private static final Long CAREGIVER_ID = 10L;

    private Mood makeMood(Long userId, int score, String label) {
        Mood m = new Mood(userId, score, label);
        m.setCreatedAt(LocalDateTime.now());
        return m;
    }

    // ─── saveMood ─────────────────────────────────────────────────────────────

    @Test
    void saveMood_returns200_withSavedMood() {
        Mood saved = makeMood(USER_ID, 8, "Happy");
        when(moodService.saveMood(USER_ID, 8, "Happy")).thenReturn(saved);

        Map<String, Object> payload = Map.of("score", 8, "label", "Happy");
        ResponseEntity<Mood> response = controller.saveMood(USER_ID, payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(saved);
    }

    @Test
    void saveMood_returnsCorrectMoodValues() {
        Mood saved = makeMood(USER_ID, 3, "Sad");
        when(moodService.saveMood(USER_ID, 3, "Sad")).thenReturn(saved);

        Map<String, Object> payload = Map.of("score", 3, "label", "Sad");
        ResponseEntity<Mood> response = controller.saveMood(USER_ID, payload);

        assertThat(response.getBody().getScore()).isEqualTo(3);
        assertThat(response.getBody().getLabel()).isEqualTo("Sad");
    }

    // ─── getCaregiverMoodSummaries ────────────────────────────────────────────

    @Test
    void getCaregiverMoodSummaries_allPatientsHaveMoods() {
        // The controller hardcodes patientIds = [1, 2, 3]
        Mood mood1 = makeMood(1L, 7, "Good");
        Mood mood2 = makeMood(2L, 5, "Neutral");
        Mood mood3 = makeMood(3L, 9, "Excellent");

        when(moodService.getMoods(1L)).thenReturn(List.of(mood1));
        when(moodService.getMoods(2L)).thenReturn(List.of(mood2));
        when(moodService.getMoods(3L)).thenReturn(List.of(mood3));

        ResponseEntity<Map<String, Object>> response = controller.getCaregiverMoodSummaries(CAREGIVER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("caregiverId")).isEqualTo(CAREGIVER_ID);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> summaries = (List<Map<String, Object>>) body.get("summaries");
        assertThat(summaries).hasSize(3);
        assertThat(summaries.get(0).get("score")).isEqualTo(7);
        assertThat(summaries.get(0).get("label")).isEqualTo("Good");
    }

    @Test
    void getCaregiverMoodSummaries_somePatientsNoMoods() {
        // patient 1 has moods, patient 2 and 3 do not
        Mood mood1 = makeMood(1L, 7, "Good");
        when(moodService.getMoods(1L)).thenReturn(List.of(mood1));
        when(moodService.getMoods(2L)).thenReturn(List.of());
        when(moodService.getMoods(3L)).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.getCaregiverMoodSummaries(CAREGIVER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> summaries =
                (List<Map<String, Object>>) response.getBody().get("summaries");
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).get("patientId")).isEqualTo(1L);
    }

    @Test
    void getCaregiverMoodSummaries_noPatientsHaveMoods_emptySummaries() {
        when(moodService.getMoods(1L)).thenReturn(List.of());
        when(moodService.getMoods(2L)).thenReturn(List.of());
        when(moodService.getMoods(3L)).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.getCaregiverMoodSummaries(CAREGIVER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> summaries =
                (List<Map<String, Object>>) response.getBody().get("summaries");
        assertThat(summaries).isEmpty();
    }

    @Test
    void getCaregiverMoodSummaries_summaryContainsCreatedAt() {
        Mood mood = makeMood(1L, 6, "Okay");
        when(moodService.getMoods(1L)).thenReturn(List.of(mood));
        when(moodService.getMoods(2L)).thenReturn(List.of());
        when(moodService.getMoods(3L)).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.getCaregiverMoodSummaries(CAREGIVER_ID);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> summaries =
                (List<Map<String, Object>>) response.getBody().get("summaries");
        assertThat(summaries.get(0)).containsKey("createdAt");
    }

    // ─── getMoods ─────────────────────────────────────────────────────────────

    @Test
    void getMoods_returns200_withMoodList() {
        List<Mood> moods = List.of(makeMood(USER_ID, 7, "Good"), makeMood(USER_ID, 5, "Okay"));
        when(moodService.getMoods(USER_ID)).thenReturn(moods);

        ResponseEntity<List<Mood>> response = controller.getMoods(USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(moods);
    }

    @Test
    void getMoods_returns200_emptyList() {
        when(moodService.getMoods(USER_ID)).thenReturn(List.of());

        ResponseEntity<List<Mood>> response = controller.getMoods(USER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
