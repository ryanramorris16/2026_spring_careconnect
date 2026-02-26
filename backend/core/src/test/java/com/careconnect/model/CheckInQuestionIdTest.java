package com.careconnect.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckInQuestionIdTest {

    // ─── Default constructor ──────────────────────────────────────────────────

    @Test
    void defaultConstructor_fieldsAreNull() {
        CheckInQuestionId id = new CheckInQuestionId();

        assertThat(id.getCheckInId()).isNull();
        assertThat(id.getQuestionId()).isNull();
    }

    // ─── Parameterized constructor ────────────────────────────────────────────

    @Test
    void parameterizedConstructor_setsFields() {
        CheckInQuestionId id = new CheckInQuestionId(10L, 20L);

        assertThat(id.getCheckInId()).isEqualTo(10L);
        assertThat(id.getQuestionId()).isEqualTo(20L);
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    void setters_updateFields() {
        CheckInQuestionId id = new CheckInQuestionId();
        id.setCheckInId(5L);
        id.setQuestionId(15L);

        assertThat(id.getCheckInId()).isEqualTo(5L);
        assertThat(id.getQuestionId()).isEqualTo(15L);
    }

    // ─── equals() ────────────────────────────────────────────────────────────

    @Test
    void equals_sameValues_returnsTrue() {
        CheckInQuestionId id1 = new CheckInQuestionId(1L, 2L);
        CheckInQuestionId id2 = new CheckInQuestionId(1L, 2L);

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    void equals_sameReference_returnsTrue() {
        CheckInQuestionId id = new CheckInQuestionId(1L, 2L);
        assertThat(id).isEqualTo(id);
    }

    @Test
    void equals_differentCheckInId_returnsFalse() {
        CheckInQuestionId id1 = new CheckInQuestionId(1L, 2L);
        CheckInQuestionId id2 = new CheckInQuestionId(9L, 2L);

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void equals_differentQuestionId_returnsFalse() {
        CheckInQuestionId id1 = new CheckInQuestionId(1L, 2L);
        CheckInQuestionId id2 = new CheckInQuestionId(1L, 9L);

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void equals_null_returnsFalse() {
        CheckInQuestionId id = new CheckInQuestionId(1L, 2L);
        assertThat(id).isNotEqualTo(null);
    }

    @Test
    void equals_differentType_returnsFalse() {
        CheckInQuestionId id = new CheckInQuestionId(1L, 2L);
        assertThat(id).isNotEqualTo("not-an-id");
    }

    // ─── hashCode() ──────────────────────────────────────────────────────────

    @Test
    void hashCode_sameValues_sameHashCode() {
        CheckInQuestionId id1 = new CheckInQuestionId(1L, 2L);
        CheckInQuestionId id2 = new CheckInQuestionId(1L, 2L);

        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
