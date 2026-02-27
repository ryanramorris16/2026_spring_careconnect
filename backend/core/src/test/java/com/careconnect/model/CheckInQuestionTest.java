package com.careconnect.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckInQuestionTest {

    // ─── Default constructor ──────────────────────────────────────────────────

    @Test
    void defaultConstructor_createsInstance() {
        CheckInQuestion cq = new CheckInQuestion();
        assertThat(cq).isNotNull();
        assertThat(cq.getId()).isNull();
        assertThat(cq.getCheckIn()).isNull();
        assertThat(cq.getQuestion()).isNull();
    }

    // ─── Parameterized constructor ────────────────────────────────────────────

    @Test
    void parameterizedConstructor_setsFields() {
        CheckIn checkIn = CheckIn.builder().id(1L).build();
        Question question = Question.builder().id(2L).prompt("How are you?").type(QuestionType.TEXT).build();

        CheckInQuestion cq = new CheckInQuestion(checkIn, question, true, 1);

        assertThat(cq.getCheckIn()).isSameAs(checkIn);
        assertThat(cq.getQuestion()).isSameAs(question);
        assertThat(cq.isRequired()).isTrue();
        assertThat(cq.getOrdinal()).isEqualTo(1);
        assertThat(cq.getId()).isNotNull();
        assertThat(cq.getId().getCheckInId()).isEqualTo(1L);
        assertThat(cq.getId().getQuestionId()).isEqualTo(2L);
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    void setters_updateFields() {
        CheckInQuestion cq = new CheckInQuestion();
        CheckInQuestionId embeddedId = new CheckInQuestionId(5L, 10L);
        CheckIn checkIn = new CheckIn();
        Question question = new Question();

        cq.setId(embeddedId);
        cq.setCheckIn(checkIn);
        cq.setQuestion(question);
        cq.setRequired(false);
        cq.setOrdinal(3);

        assertThat(cq.getId()).isSameAs(embeddedId);
        assertThat(cq.getCheckIn()).isSameAs(checkIn);
        assertThat(cq.getQuestion()).isSameAs(question);
        assertThat(cq.isRequired()).isFalse();
        assertThat(cq.getOrdinal()).isEqualTo(3);
    }
}
