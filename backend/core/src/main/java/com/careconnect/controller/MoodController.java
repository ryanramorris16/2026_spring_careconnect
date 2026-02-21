package com.careconnect.controller;

import com.careconnect.model.Mood;
import com.careconnect.service.MoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*") // Critical: Allows Flutter Web (Chrome) to talk to Spring Boot
@RequestMapping("v1/api/patient")
public class MoodController {

    @Autowired
    private MoodService moodService;

    /**
     * Saves a mood entry from the Flutter app.
     * Maps Flutter's JSON (mood_score, note) to Java's Mood model (score, label).
     */
    @PostMapping("/{userId}/mood")
    public ResponseEntity<Mood> saveMood(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> payload) {

        // Extracting values from the Flutter JSON payload
        // Flutter uses 'mood_score' for the integer value
        Object scoreRaw = payload.get("mood_score");
        int score = (scoreRaw instanceof Number) ? ((Number) scoreRaw).intValue() : 3;

        // Flutter uses 'note' for the optional text string
        String note = (String) payload.getOrDefault("note", "");

        // Mapping 'note' to your Mood model's 'label' field
        Mood savedMood = moodService.saveMood(userId, score, note);
        
        System.out.println("[API] Success: Saved mood for user " + userId);
        return ResponseEntity.ok(savedMood);
    }

    /**
     * Returns a summary of moods for multiple patients.
     * Used by the Caregiver view.
     */
    @GetMapping("/caregiver/{caregiverId}/moods")
    public ResponseEntity<Map<String, Object>> getCaregiverMoodSummaries(@PathVariable Long caregiverId) {
        Map<String, Object> data = new HashMap<>();

        // Demo logic: using hardcoded IDs that match your frontend test data
        List<Long> patientIds = List.of(1L, 2L, 3L);
        List<Map<String, Object>> summaries = new ArrayList<>();

        for (Long patientId : patientIds) {
            List<Mood> moods = moodService.getMoods(patientId);
            if (moods != null && !moods.isEmpty()) {
                Mood latest = moods.get(0);
                Map<String, Object> summary = new HashMap<>();
                summary.put("patientId", patientId);
                summary.put("score", latest.getScore());
                summary.put("label", latest.getLabel());
                summary.put("createdAt", latest.getCreatedAt());
                summaries.add(summary);
            }
        }

        data.put("caregiverId", caregiverId);
        data.put("summaries", summaries);
        return ResponseEntity.ok(data);
    }

    /**
     * Returns all mood logs for a specific patient.
     */
    @GetMapping("/{userId}/mood")
    public ResponseEntity<List<Mood>> getMoods(@PathVariable Long userId) {
        List<Mood> moods = moodService.getMoods(userId);
        return ResponseEntity.ok(moods);
    }
}