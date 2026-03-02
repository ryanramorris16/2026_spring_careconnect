package com.careconnect.model.evv;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvvLocationRoleTest {

    @Test
    void values_containsAllExpected() {
        assertThat(EvvLocationRole.values()).containsExactly(
                EvvLocationRole.CHECK_IN,
                EvvLocationRole.CHECK_OUT
        );
    }

    @Test
    void valueOf_returnsCorrectConstant() {
        assertThat(EvvLocationRole.valueOf("CHECK_IN")).isEqualTo(EvvLocationRole.CHECK_IN);
        assertThat(EvvLocationRole.valueOf("CHECK_OUT")).isEqualTo(EvvLocationRole.CHECK_OUT);
    }
}
