package com.careconnect.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplatesTest {

    @Test
    void instantiation_succeeds() {
        assertThat(new EmailTemplates()).isNotNull();
    }
}
