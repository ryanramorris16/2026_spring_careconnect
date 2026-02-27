package com.careconnect.model.checkins;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckInTemplateTest {

    @Test
    void defaultConstructor_createsInstance() {
        CheckInTemplate template = new CheckInTemplate();
        assertThat(template).isNotNull();
    }
}
