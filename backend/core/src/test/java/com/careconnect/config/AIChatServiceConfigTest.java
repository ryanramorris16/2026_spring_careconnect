package com.careconnect.config;

import com.careconnect.service.security.SecurityAuditService;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIChatServiceConfig.
 *
 * Validates configuration bean creation behavior for supported providers
 * (deepseek, bedrock) and verifies that unsupported providers are rejected.
 *
 * Note: The chatModel() method does NOT call validateConfiguration(), so
 * validation-related behavior (API key checks, URL checks) is not exercised
 * by chatModel(). These tests verify actual runtime behavior of chatModel().
 *
 * Mocks SecurityAuditService to satisfy the constructor dependency.
 */
class AIChatServiceConfigTest {

    private SecurityAuditService securityAuditService;
    private AIChatServiceConfig config;

    @BeforeEach
    void setUp() throws Exception {
        // Arrange: Create mock security audit service
        securityAuditService = mock(SecurityAuditService.class);
        config = new AIChatServiceConfig(securityAuditService);
    }

    @Test
    void constructorInitializesSuccessfully() throws Exception {
        // Assert: Constructor should complete without throwing
        assertNotNull(config);
    }

    @Test
    void chatModelBeanCreatedSuccessfullyWithDeepseekProvider() throws Exception {
        // Arrange: Set valid deepseek configuration
        ReflectionTestUtils.setField(config, "provider", "deepseek");
        ReflectionTestUtils.setField(config, "apiKey", "sk-test-1234567890abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(config, "apiUrl", "https://api.deepseek.com/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "deepseek-chat");
        ReflectionTestUtils.setField(config, "temperature", 1.0);

        // Act: Create the ChatModel bean
        final ChatModel chatModel = config.chatModel();

        // Assert: Bean should be created successfully
        assertNotNull(chatModel);
        verify(securityAuditService, never()).logConfigurationValidationError(anyString(), anyString(), anyString());
    }

    @Test
    void chatModelThrowsExceptionForUnsupportedOpenaiProvider() throws Exception {
        // Arrange: "openai" is not a supported provider in chatModel()
        ReflectionTestUtils.setField(config, "provider", "openai");
        ReflectionTestUtils.setField(config, "apiKey", "sk-test-1234567890abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(config, "apiUrl", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "gpt-4o-mini");
        ReflectionTestUtils.setField(config, "temperature", 1.0);

        // Act & Assert: Should throw IllegalStateException wrapped with "AI configuration failed"
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            config.chatModel();
        });

        assertEquals("AI configuration failed", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("unsupported AI provider: openai", exception.getCause().getMessage());
    }

    @Test
    void chatModelThrowsExceptionForUnsupportedProviderWithEmptyApiKey() throws Exception {
        // Arrange: chatModel() does not call validateConfiguration(), so the
        // unsupported provider error takes precedence over missing API key
        ReflectionTestUtils.setField(config, "provider", "openai");
        ReflectionTestUtils.setField(config, "apiKey", "");
        ReflectionTestUtils.setField(config, "apiUrl", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "gpt-4o-mini");
        ReflectionTestUtils.setField(config, "temperature", 1.0);

        // Act & Assert: Should throw due to unsupported provider
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            config.chatModel();
        });

        assertEquals("AI configuration failed", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("unsupported AI provider: openai", exception.getCause().getMessage());
    }

    @Test
    void chatModelThrowsExceptionForUnsupportedProviderWithNullApiKey() throws Exception {
        // Arrange: chatModel() does not call validateConfiguration(), so the
        // unsupported provider error takes precedence over null API key
        ReflectionTestUtils.setField(config, "provider", "openai");
        ReflectionTestUtils.setField(config, "apiKey", null);
        ReflectionTestUtils.setField(config, "apiUrl", "https://api.openai.com/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "gpt-4o-mini");
        ReflectionTestUtils.setField(config, "temperature", 1.0);

        // Act & Assert: Should throw due to unsupported provider
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            config.chatModel();
        });

        assertEquals("AI configuration failed", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("unsupported AI provider: openai", exception.getCause().getMessage());
    }

    @Test
    void chatModelThrowsExceptionForUnsupportedProviderWithEmptyApiUrl() throws Exception {
        // Arrange: chatModel() does not call validateConfiguration(), so the
        // unsupported provider error takes precedence over missing URL
        ReflectionTestUtils.setField(config, "provider", "openai");
        ReflectionTestUtils.setField(config, "apiKey", "sk-test-1234567890abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(config, "apiUrl", "");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "gpt-4o-mini");
        ReflectionTestUtils.setField(config, "temperature", 1.0);

        // Act & Assert: Should throw due to unsupported provider
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            config.chatModel();
        });

        assertEquals("AI configuration failed", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("unsupported AI provider: openai", exception.getCause().getMessage());
    }

    @Test
    void chatModelThrowsExceptionForUnsupportedProviderWithNullApiUrl() throws Exception {
        // Arrange: chatModel() does not call validateConfiguration(), so the
        // unsupported provider error takes precedence over null URL
        ReflectionTestUtils.setField(config, "provider", "openai");
        ReflectionTestUtils.setField(config, "apiKey", "sk-test-1234567890abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(config, "apiUrl", null);
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "gpt-4o-mini");
        ReflectionTestUtils.setField(config, "temperature", 1.0);

        // Act & Assert: Should throw due to unsupported provider
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            config.chatModel();
        });

        assertEquals("AI configuration failed", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("unsupported AI provider: openai", exception.getCause().getMessage());
    }

    @Test
    void chatModelCreatesDeepseekWithHttpUrl() throws Exception {
        // Arrange: Set configuration with HTTP URL (insecure) for deepseek
        // chatModel() does not call validateConfiguration(), so no HTTPS check occurs
        ReflectionTestUtils.setField(config, "provider", "deepseek");
        ReflectionTestUtils.setField(config, "apiKey", "sk-test-1234567890abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(config, "apiUrl", "http://api.example.com/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "deepseek-chat");
        ReflectionTestUtils.setField(config, "temperature", 1.0);

        // Act: Create ChatModel - should succeed since validateConfiguration is not called
        final ChatModel chatModel = config.chatModel();

        // Assert: Bean created; no audit service interaction
        assertNotNull(chatModel);
        verify(securityAuditService, never()).logConfigurationValidationError(
                anyString(), anyString(), anyString());
    }

    @Test
    void chatModelSucceedsWithDeepseekShortApiKey() throws Exception {
        // Arrange: Set configuration with short API key (< 20 chars) for deepseek
        // chatModel() does not call validateConfiguration(), so no key length check
        ReflectionTestUtils.setField(config, "provider", "deepseek");
        ReflectionTestUtils.setField(config, "apiKey", "short-key-123");
        ReflectionTestUtils.setField(config, "apiUrl", "https://api.deepseek.com/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "deepseek-chat");
        ReflectionTestUtils.setField(config, "temperature", 1.0);

        // Act: Create ChatModel - should succeed
        final ChatModel chatModel = config.chatModel();

        // Assert: Bean created successfully
        assertNotNull(chatModel);
    }

    @Test
    void chatModelSucceedsWithDeepseekNegativeTemperature() throws Exception {
        // Arrange: Set configuration with temperature below 0.0 for deepseek
        ReflectionTestUtils.setField(config, "provider", "deepseek");
        ReflectionTestUtils.setField(config, "apiKey", "sk-test-1234567890abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(config, "apiUrl", "https://api.deepseek.com/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "deepseek-chat");
        ReflectionTestUtils.setField(config, "temperature", -0.5);

        // Act: Create ChatModel - should succeed (no validation called)
        final ChatModel chatModel = config.chatModel();

        // Assert: Bean created successfully
        assertNotNull(chatModel);
    }

    @Test
    void chatModelSucceedsWithDeepseekHighTemperature() throws Exception {
        // Arrange: Set configuration with temperature above 2.0 for deepseek
        ReflectionTestUtils.setField(config, "provider", "deepseek");
        ReflectionTestUtils.setField(config, "apiKey", "sk-test-1234567890abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(config, "apiUrl", "https://api.deepseek.com/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "deepseek-chat");
        ReflectionTestUtils.setField(config, "temperature", 2.5);

        // Act: Create ChatModel - should succeed (no validation called)
        final ChatModel chatModel = config.chatModel();

        // Assert: Bean created successfully
        assertNotNull(chatModel);
    }

    @Test
    void chatModelSucceedsWithDeepseekCaseInsensitive() throws Exception {
        // Arrange: Set configuration with "DeepSeek" (mixed case)
        ReflectionTestUtils.setField(config, "provider", "DeepSeek");
        ReflectionTestUtils.setField(config, "apiKey", "sk-deepseek-1234567890abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(config, "apiUrl", "https://api.deepseek.com/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "deepseek-chat");
        ReflectionTestUtils.setField(config, "temperature", 0.7);

        // Act: Create ChatModel - equalsIgnoreCase should match "deepseek"
        final ChatModel chatModel = config.chatModel();

        // Assert: Bean created successfully
        assertNotNull(chatModel);
        verify(securityAuditService, never()).logConfigurationValidationError(anyString(), anyString(), anyString());
    }

    @Test
    void chatModelSucceedsWithDeepseekTemperatureBoundaries() throws Exception {
        // Arrange: Test lower boundary (0.0)
        ReflectionTestUtils.setField(config, "provider", "deepseek");
        ReflectionTestUtils.setField(config, "apiKey", "sk-test-1234567890abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(config, "apiUrl", "https://api.deepseek.com/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "deepseek-chat");
        ReflectionTestUtils.setField(config, "temperature", 0.0);

        // Act: Create ChatModel with temperature 0.0
        final ChatModel chatModel1 = config.chatModel();

        // Assert: Bean created successfully
        assertNotNull(chatModel1);

        // Arrange: Test upper boundary (2.0)
        ReflectionTestUtils.setField(config, "temperature", 2.0);

        // Act: Create ChatModel with temperature 2.0
        final ChatModel chatModel2 = config.chatModel();

        // Assert: Bean created successfully
        assertNotNull(chatModel2);
    }

    @Test
    void chatModelSucceedsWithDeepseekTypicalTemperatureValues() throws Exception {
        // Arrange: Test common temperature values (0.5, 0.7, 1.0, 1.5)
        ReflectionTestUtils.setField(config, "provider", "deepseek");
        ReflectionTestUtils.setField(config, "apiKey", "sk-test-1234567890abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(config, "apiUrl", "https://api.deepseek.com/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "deepseek-chat");

        final double[] temperatures = {0.5, 0.7, 1.0, 1.5};

        for (final double temp : temperatures) {
            // Arrange: Set temperature
            ReflectionTestUtils.setField(config, "temperature", temp);

            // Act: Create ChatModel
            final ChatModel chatModel = config.chatModel();

            // Assert: Bean created successfully
            assertNotNull(chatModel, "ChatModel should be created with temperature " + temp);
        }
    }

    @Test
    void chatModelThrowsForUnsupportedProviderEvenWithValidConfig() throws Exception {
        // Arrange: Set configuration with all valid values but unsupported provider
        // Verifies that unsupported provider is rejected regardless of other config
        ReflectionTestUtils.setField(config, "provider", "mistral");
        ReflectionTestUtils.setField(config, "apiKey", "sk-test-1234567890abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(config, "apiUrl", "https://api.mistral.ai/v1");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "modelName", "mistral-medium");
        ReflectionTestUtils.setField(config, "temperature", 1.0);

        // Act & Assert: Should throw for unsupported provider
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            config.chatModel();
        });

        assertEquals("AI configuration failed", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("unsupported AI provider: mistral", exception.getCause().getMessage());
        // securityAuditService should not be called since validateConfiguration is never invoked
        verify(securityAuditService, never()).logConfigurationValidationError(
                anyString(), anyString(), anyString());
    }
}
