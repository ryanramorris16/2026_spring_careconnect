package com.careconnect.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WebClientConfig}.
 *
 * WebClientConfig creates a Spring {@code @Bean} of type {@link RestTemplate} that
 * wraps its underlying HTTP factory in a {@link BufferingClientHttpRequestFactory}.
 * Buffering is required so that response bodies can be read more than once —
 * for example by both a logging interceptor and the calling service.
 *
 * One test ({@link #shouldCreateRestTemplateBean}) spins up a minimal Spring context
 * using {@link AnnotationConfigApplicationContext} to verify that the bean is properly
 * registered and retrievable via the container. The remaining tests instantiate
 * {@link WebClientConfig} directly — no Spring context needed — to check implementation
 * details of the returned {@link RestTemplate}.
 */
class WebClientConfigTest {

    @Test
    void shouldCreateRestTemplateBean() {
        // Verifies that WebClientConfig successfully registers a RestTemplate bean
        // in a real (minimal) Spring context, confirming Spring can discover and wire it.
        // The context is closed immediately after to avoid resource leaks.
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(WebClientConfig.class);

        RestTemplate restTemplate = context.getBean(RestTemplate.class);

        assertNotNull(restTemplate);
        context.close();
    }

    @Test
    void shouldUseBufferingRequestFactory() {
        // Verifies that the RestTemplate uses a BufferingClientHttpRequestFactory,
        // which caches the response body in memory so it can be read multiple times
        // (necessary for logging interceptors that consume the body before the caller).
        WebClientConfig config = new WebClientConfig();
        RestTemplate restTemplate = config.restTemplate();

        ClientHttpRequestFactory factory = restTemplate.getRequestFactory();

        assertTrue(factory instanceof BufferingClientHttpRequestFactory);
    }

    @Test
    void restTemplateShouldBeFunctional() {
        // Verifies the RestTemplate is fully configured: it has a non-null request
        // factory and at least the default set of message converters (JSON, form data,
        // etc.), confirming it is ready to make and deserialise HTTP calls.
        WebClientConfig config = new WebClientConfig();
        RestTemplate restTemplate = config.restTemplate();

        assertNotNull(restTemplate.getRequestFactory());
        assertNotNull(restTemplate.getMessageConverters());
        assertFalse(restTemplate.getMessageConverters().isEmpty());
    }

    @Test
    void shouldReturnNewInstanceEachCall() {
        // Verifies that each call to restTemplate() produces a distinct instance.
        // This is the expected behaviour for a @Bean method without @Scope("singleton")
        // when called directly (outside the Spring proxy), and ensures no shared state
        // bleeds between different injection points if the method is called multiple times.
        WebClientConfig config = new WebClientConfig();

        RestTemplate r1 = config.restTemplate();
        RestTemplate r2 = config.restTemplate();

        assertNotSame(r1, r2);
    }
}
