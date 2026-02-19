package com.careconnect.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketModeConfigTest {

    @Mock
    private Environment env;

    private WebSocketModeConfig config;

    @BeforeEach
    void setUp() {
        config = new WebSocketModeConfig();
    }

    @Test
    void returnsLocalWhenAwsEndpointPropertyIsNull() {
        when(env.getProperty("AWS_WEBSOCKET_API_ENDPOINT")).thenReturn(null);

        String mode = config.websocketMode(env);

        assertEquals("local", mode);
    }

    @Test
    void returnsLocalWhenAwsEndpointPropertyIsEmpty() {
        when(env.getProperty("AWS_WEBSOCKET_API_ENDPOINT")).thenReturn("");

        String mode = config.websocketMode(env);

        assertEquals("local", mode);
    }

    @Test
    void returnsLocalWhenAwsEndpointPropertyIsBlank() {
        when(env.getProperty("AWS_WEBSOCKET_API_ENDPOINT")).thenReturn("   ");

        String mode = config.websocketMode(env);

        assertEquals("local", mode);
    }

    @Test
    void returnsAwsWhenAwsEndpointPropertyIsSet() {
        when(env.getProperty("AWS_WEBSOCKET_API_ENDPOINT")).thenReturn("https://abc123.execute-api.us-east-1.amazonaws.com/prod");

        String mode = config.websocketMode(env);

        assertEquals("aws", mode);
    }

    @Test
    void returnsAwsWhenAwsEndpointPropertyIsMinimalNonBlankString() {
        when(env.getProperty("AWS_WEBSOCKET_API_ENDPOINT")).thenReturn("wss://example.com");

        String mode = config.websocketMode(env);

        assertEquals("aws", mode);
    }
}
