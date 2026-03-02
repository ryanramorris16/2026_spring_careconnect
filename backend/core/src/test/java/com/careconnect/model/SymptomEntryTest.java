package com.careconnect.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SymptomEntryTest {

    // ─── No-arg constructor ───────────────────────────────────────────────────

    @Test
    void noArgConstructor_createsInstance() {
        SymptomEntry e = new SymptomEntry();

        assertThat(e).isNotNull();
        assertThat(e.getId()).isNull();
        assertThat(e.getPatient()).isNull();
        assertThat(e.getCaregiver()).isNull();
        assertThat(e.getCompleted()).isTrue(); // @Builder.Default initialises to true in no-arg ctor
        assertThat(e.getSymptomKey()).isNull();
        assertThat(e.getSymptomValue()).isNull();
        assertThat(e.getSeverity()).isNull();
        assertThat(e.getTakenAt()).isNull();
        assertThat(e.getNotes()).isNull();
    }

    // ─── Builder defaults ─────────────────────────────────────────────────────

    @Test
    void builder_defaults() {
        SymptomEntry e = SymptomEntry.builder()
                .takenAt(Instant.now())
                .build();

        assertThat(e.getCompleted()).isTrue();
    }

    // ─── Builder all fields ───────────────────────────────────────────────────

    @Test
    void builder_allFields() {
        Patient patient = new Patient();
        Caregiver caregiver = new Caregiver();
        Instant now = Instant.now();

        SymptomEntry e = SymptomEntry.builder()
                .id(1L)
                .patient(patient)
                .caregiver(caregiver)
                .completed(false)
                .symptomKey("headache")
                .symptomValue("mild")
                .severity(2)
                .takenAt(now)
                .notes("Patient reports headache since morning")
                .build();

        assertThat(e.getId()).isEqualTo(1L);
        assertThat(e.getPatient()).isSameAs(patient);
        assertThat(e.getCaregiver()).isSameAs(caregiver);
        assertThat(e.getCompleted()).isFalse();
        assertThat(e.getSymptomKey()).isEqualTo("headache");
        assertThat(e.getSymptomValue()).isEqualTo("mild");
        assertThat(e.getSeverity()).isEqualTo(2);
        assertThat(e.getTakenAt()).isEqualTo(now);
        assertThat(e.getNotes()).isEqualTo("Patient reports headache since morning");
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    void setters_updateFields() {
        SymptomEntry e = new SymptomEntry();
        Instant now = Instant.now();

        e.setSymptomKey("cough");
        e.setSymptomValue("severe");
        e.setSeverity(4);
        e.setTakenAt(now);
        e.setCompleted(true);
        e.setNotes("Dry cough");

        assertThat(e.getSymptomKey()).isEqualTo("cough");
        assertThat(e.getSymptomValue()).isEqualTo("severe");
        assertThat(e.getSeverity()).isEqualTo(4);
        assertThat(e.getTakenAt()).isEqualTo(now);
        assertThat(e.getCompleted()).isTrue();
        assertThat(e.getNotes()).isEqualTo("Dry cough");
    }
}
