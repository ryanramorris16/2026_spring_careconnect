package com.careconnect.model.checkins;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckInTest {

    // ─── No-arg constructor ───────────────────────────────────────────────────

    @Test
    void noArgConstructor_createsInstance() throws Exception {
        CheckIn checkIn = new CheckIn();
        assertThat(checkIn).isNotNull();
    }

    // ─── Package-private constructor with template ────────────────────────────

    @Test
    void templateConstructor_setsTemplate() throws Exception {
        CheckInTemplate template = new CheckInTemplate();
        CheckIn checkIn = new CheckIn(template);
        assertThat(checkIn).isNotNull();
    }
}
