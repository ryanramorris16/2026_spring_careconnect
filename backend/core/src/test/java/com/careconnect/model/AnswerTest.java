package com.careconnect.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerTest {

    // ─── No-arg constructor ───────────────────────────────────────────────────

    @Test
    void noArgConstructor_createsInstance() {
        Answer answer = new Answer();

        assertThat(answer).isNotNull();
        assertThat(answer.getId()).isNull();
        assertThat(answer.getCheckIn()).isNull();
        assertThat(answer.getQuestion()).isNull();
        assertThat(answer.getValueText()).isNull();
        assertThat(answer.getValueBoolean()).isNull();
        assertThat(answer.getValueNumber()).isNull();
    }

    // ─── Builder default: createdAt ───────────────────────────────────────────

    @Test
    void builder_createdAt_defaultsToNow() {
        Answer answer = Answer.builder()
                .checkIn(new CheckIn())
                .question(new Question())
                .build();

        assertThat(answer.getCreatedAt()).isNotNull();
    }

    // ─── Builder all fields ───────────────────────────────────────────────────

    @Test
    void builder_allFields() {
        CheckIn checkIn = new CheckIn();
        Question question = new Question();
        OffsetDateTime now = OffsetDateTime.now();

        Answer answer = Answer.builder()
                .id(1L)
                .checkIn(checkIn)
                .question(question)
                .valueText("Some text")
                .valueBoolean(true)
                .valueNumber(new BigDecimal("3.5"))
                .createdAt(now)
                .build();

        assertThat(answer.getId()).isEqualTo(1L);
        assertThat(answer.getCheckIn()).isSameAs(checkIn);
        assertThat(answer.getQuestion()).isSameAs(question);
        assertThat(answer.getValueText()).isEqualTo("Some text");
        assertThat(answer.getValueBoolean()).isTrue();
        assertThat(answer.getValueNumber()).isEqualByComparingTo(new BigDecimal("3.5"));
        assertThat(answer.getCreatedAt()).isEqualTo(now);
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    void setters_updateFields() {
        Answer answer = new Answer();
        CheckIn checkIn = new CheckIn();
        Question question = new Question();
        OffsetDateTime now = OffsetDateTime.now();

        answer.setId(2L);
        answer.setCheckIn(checkIn);
        answer.setQuestion(question);
        answer.setValueText("Another text");
        answer.setValueBoolean(false);
        answer.setValueNumber(new BigDecimal("7.0"));
        answer.setCreatedAt(now);

        assertThat(answer.getId()).isEqualTo(2L);
        assertThat(answer.getCheckIn()).isSameAs(checkIn);
        assertThat(answer.getQuestion()).isSameAs(question);
        assertThat(answer.getValueText()).isEqualTo("Another text");
        assertThat(answer.getValueBoolean()).isFalse();
        assertThat(answer.getValueNumber()).isEqualByComparingTo(new BigDecimal("7.0"));
        assertThat(answer.getCreatedAt()).isEqualTo(now);
    }

    // ─── equals() and hashCode() ──────────────────────────────────────────────

    @Test
    void equals_sameFields_returnsTrue() {
        OffsetDateTime now = OffsetDateTime.now();
        Answer a1 = Answer.builder().id(1L).valueText("hello").createdAt(now).build();
        Answer a2 = Answer.builder().id(1L).valueText("hello").createdAt(now).build();

        assertThat(a1).isEqualTo(a2);
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
    }

    @Test
    void equals_differentFields_returnsFalse() {
        Answer a1 = Answer.builder().id(1L).build();
        Answer a2 = Answer.builder().id(2L).build();

        assertThat(a1).isNotEqualTo(a2);
    }
}
