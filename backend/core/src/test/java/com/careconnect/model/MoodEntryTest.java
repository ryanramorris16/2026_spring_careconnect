package com.careconnect.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MoodEntryTest {

    // ─── No-arg constructor ───────────────────────────────────────────────────

    @Test
    void noArgConstructor_createsInstance() throws Exception {
        MoodEntry entry = new MoodEntry();

        assertThat(entry).isNotNull();
        assertThat(entry.getId()).isNull();
        assertThat(entry.getPatient()).isNull();
        assertThat(entry.getMoodScore()).isNull();
        assertThat(entry.getTakenAt()).isNull();
    }

    // ─── Builder all fields ───────────────────────────────────────────────────

    @Test
    void builder_allFields() throws Exception {
        Patient patient = new Patient();
        Instant now = Instant.now();

        MoodEntry entry = MoodEntry.builder()
                .id(1L)
                .patient(patient)
                .moodScore(4)
                .takenAt(now)
                .build();

        assertThat(entry.getId()).isEqualTo(1L);
        assertThat(entry.getPatient()).isSameAs(patient);
        assertThat(entry.getMoodScore()).isEqualTo(4);
        assertThat(entry.getTakenAt()).isEqualTo(now);
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    void setters_updateFields() throws Exception {
        MoodEntry entry = new MoodEntry();
        Patient patient = new Patient();
        Instant now = Instant.now();

        entry.setId(2L);
        entry.setPatient(patient);
        entry.setMoodScore(2);
        entry.setTakenAt(now);

        assertThat(entry.getId()).isEqualTo(2L);
        assertThat(entry.getPatient()).isSameAs(patient);
        assertThat(entry.getMoodScore()).isEqualTo(2);
        assertThat(entry.getTakenAt()).isEqualTo(now);
    }

    // ─── equals() and hashCode() ──────────────────────────────────────────────

    @Test
    void equals_sameFields_returnsTrue() throws Exception {
        MoodEntry e1 = MoodEntry.builder().id(1L).moodScore(3).build();
        MoodEntry e2 = MoodEntry.builder().id(1L).moodScore(3).build();

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }
}
