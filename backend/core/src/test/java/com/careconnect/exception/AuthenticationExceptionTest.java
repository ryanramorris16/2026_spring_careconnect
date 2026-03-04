package com.careconnect.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthenticationException")
class AuthenticationExceptionTest {

    @Test
    @DisplayName("constructor with message sets message")
    void messageConstructor_setsMessage() {
        AuthenticationException ex = new AuthenticationException("bad creds");
        assertEquals("bad creds", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("constructor with message and cause sets both")
    void messageAndCauseConstructor_setsBoth() {
        RuntimeException cause = new RuntimeException("root");
        AuthenticationException ex = new AuthenticationException("bad creds", cause);
        assertEquals("bad creds", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
