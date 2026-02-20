package com.careconnect.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CorsFilterConfig}.
 *
 * CorsFilterConfig produces a Spring Security {@code CorsConfigurationSource} bean that
 * controls which origins, methods, and headers are allowed for cross-origin requests.
 *
 * The class reads allowed origins from a {@code @Value}-injected list, which Spring does
 * not populate when the object is instantiated directly (outside a Spring context).
 * {@link ReflectionTestUtils#setField} is therefore used in {@code setUp} to inject a
 * controlled list of origins, keeping these tests fast and free of a full application
 * context while still exercising the real configuration logic.
 */
class CorsFilterConfigTest {

    private CorsFilterConfig corsFilterConfig;

    @BeforeEach
    void setUp() {
        corsFilterConfig = new CorsFilterConfig();

        // Inject allowed origins manually (since @Value won't run without a Spring context)
        ReflectionTestUtils.setField(
                corsFilterConfig,
                "allowedOrigins",
                List.of("http://localhost:3000", "https://careconnect.com")
        );
    }

    @Test
    void corsConfigurationSource_IsCreated() {
        // Verifies that corsFilter() returns a non-null UrlBasedCorsConfigurationSource,
        // which is the expected concrete type used to map CORS rules to URL patterns.
        CorsConfigurationSource source = corsFilterConfig.corsFilter();
        assertNotNull(source);
        assertTrue(source instanceof UrlBasedCorsConfigurationSource);
    }

    @Test
    void corsConfiguration_HasCorrectAllowedOrigins() {
        // Verifies that the injected origins are registered as allowed origin patterns
        // on the CORS configuration mapped to "/**" (all paths).
        CorsConfigurationSource source = corsFilterConfig.corsFilter();
        UrlBasedCorsConfigurationSource urlSource =
                (UrlBasedCorsConfigurationSource) source;

        CorsConfiguration config =
                urlSource.getCorsConfigurations().get("/**");

        assertNotNull(config);
        assertEquals(
                List.of("http://localhost:3000", "https://careconnect.com"),
                config.getAllowedOriginPatterns()
        );
    }

    @Test
    void corsConfiguration_AllowsCredentials() {
        // Verifies that credentials (cookies, Authorization headers) are permitted,
        // which is required for JWT cookie-based authentication flows.
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) corsFilterConfig.corsFilter();

        CorsConfiguration config =
                source.getCorsConfigurations().get("/**");

        assertTrue(config.getAllowCredentials());
    }

    @Test
    void corsConfiguration_AllowsAllHeaders() {
        // Verifies that both allowedHeaders and exposedHeaders are set to "*", meaning
        // any request header is accepted and any response header is visible to the client.
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) corsFilterConfig.corsFilter();

        CorsConfiguration config =
                source.getCorsConfigurations().get("/**");

        assertEquals(List.of("*"), config.getAllowedHeaders());
        assertEquals(List.of("*"), config.getExposedHeaders());
    }

    @Test
    void corsConfiguration_HasCorrectAllowedMethods() {
        // Verifies that the five standard HTTP methods required by the API are listed,
        // including OPTIONS which is needed for preflight CORS requests from browsers.
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) corsFilterConfig.corsFilter();

        CorsConfiguration config =
                source.getCorsConfigurations().get("/**");

        assertEquals(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"),
                config.getAllowedMethods()
        );
    }

    @Test
    void corsConfiguration_IsRegisteredForAllPaths() {
        // Verifies that the CORS policy is applied globally (mapped to "/**"),
        // ensuring no endpoint is inadvertently left without CORS protection.
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) corsFilterConfig.corsFilter();

        assertTrue(source.getCorsConfigurations().containsKey("/**"));
    }
}
