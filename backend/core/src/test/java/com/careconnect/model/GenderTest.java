package com.careconnect.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenderTest {

    // ─── getDisplayName() ────────────────────────────────────────────────────

    @Test
    void getDisplayName_male() {
        assertThat(Gender.MALE.getDisplayName()).isEqualTo("Male");
    }

    @Test
    void getDisplayName_female() {
        assertThat(Gender.FEMALE.getDisplayName()).isEqualTo("Female");
    }

    @Test
    void getDisplayName_other() {
        assertThat(Gender.OTHER.getDisplayName()).isEqualTo("Other");
    }

    @Test
    void getDisplayName_preferNotToSay() {
        assertThat(Gender.PREFER_NOT_TO_SAY.getDisplayName()).isEqualTo("Prefer not to say");
    }

    // ─── getValue() ──────────────────────────────────────────────────────────

    @Test
    void getValue_returnsLowercaseName() {
        assertThat(Gender.MALE.getValue()).isEqualTo("male");
        assertThat(Gender.FEMALE.getValue()).isEqualTo("female");
        assertThat(Gender.OTHER.getValue()).isEqualTo("other");
        assertThat(Gender.PREFER_NOT_TO_SAY.getValue()).isEqualTo("prefer_not_to_say");
    }

    // ─── fromString() ────────────────────────────────────────────────────────

    @Test
    void fromString_null_returnsNull() {
        assertThat(Gender.fromString(null)).isNull();
    }

    @Test
    void fromString_male_uppercase() {
        assertThat(Gender.fromString("MALE")).isEqualTo(Gender.MALE);
    }

    @Test
    void fromString_female_lowercase() {
        assertThat(Gender.fromString("female")).isEqualTo(Gender.FEMALE);
    }

    @Test
    void fromString_other_mixedCase() {
        assertThat(Gender.fromString("Other")).isEqualTo(Gender.OTHER);
    }

    @Test
    void fromString_M_abbreviation() {
        assertThat(Gender.fromString("M")).isEqualTo(Gender.MALE);
    }

    @Test
    void fromString_F_abbreviation() {
        assertThat(Gender.fromString("F")).isEqualTo(Gender.FEMALE);
    }

    @Test
    void fromString_PREFER_NOT_TO_SAY() {
        assertThat(Gender.fromString("PREFER_NOT_TO_SAY")).isEqualTo(Gender.PREFER_NOT_TO_SAY);
    }

    @Test
    void fromString_PREFERNOTTOSAY() {
        assertThat(Gender.fromString("PREFERNOTTOSAY")).isEqualTo(Gender.PREFER_NOT_TO_SAY);
    }

    @Test
    void fromString_NOT_SAY() {
        assertThat(Gender.fromString("NOT_SAY")).isEqualTo(Gender.PREFER_NOT_TO_SAY);
    }

    @Test
    void fromString_preferNotToSay_withSpaces() {
        assertThat(Gender.fromString("PREFER NOT TO SAY")).isEqualTo(Gender.PREFER_NOT_TO_SAY);
    }

    @Test
    void fromString_invalid_throwsException() {
        assertThatThrownBy(() -> Gender.fromString("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid gender value");
    }

    // ─── toString() ──────────────────────────────────────────────────────────

    @Test
    void toString_returnsDisplayName() {
        assertThat(Gender.MALE.toString()).isEqualTo("Male");
        assertThat(Gender.FEMALE.toString()).isEqualTo("Female");
        assertThat(Gender.OTHER.toString()).isEqualTo("Other");
        assertThat(Gender.PREFER_NOT_TO_SAY.toString()).isEqualTo("Prefer not to say");
    }

    // ─── enum values ─────────────────────────────────────────────────────────

    @Test
    void values_containsAllExpected() {
        assertThat(Gender.values()).containsExactly(
                Gender.MALE, Gender.FEMALE, Gender.OTHER, Gender.PREFER_NOT_TO_SAY);
    }
}
