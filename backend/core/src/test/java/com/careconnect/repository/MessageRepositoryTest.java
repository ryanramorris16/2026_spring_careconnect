package com.careconnect.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@code default void test()} method in {@link MessageRepository}.
 * All other methods are abstract query signatures with no executable code.
 */
class MessageRepositoryTest {

    private final MessageRepository repo = Mockito.mock(MessageRepository.class, Mockito.CALLS_REAL_METHODS);

    @Test
    @DisplayName("test() prints initialization message to stdout")
    void test_printsInitializationMessage() throws Exception {
        final PrintStream original = System.out;
        final ByteArrayOutputStream capture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capture));
        try {
            repo.test();
        } finally {
            System.setOut(original);
        }

        assertTrue(capture.toString().contains("MessageRepository initialized"));
    }
}
