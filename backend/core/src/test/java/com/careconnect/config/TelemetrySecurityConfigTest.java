package com.careconnect.config;

import com.careconnect.controller.dev.DevTelemetryController;
import com.careconnect.security.JwtTokenProvider;
import com.careconnect.service.TelemetryService;
import com.careconnect.service.TelemetryToggleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;

@WebMvcTest(controllers = DevTelemetryController.class)
@Import({SecurityConfig.class, SecurityConfigTelemetryTest.TestCorsConfig.class})
@ActiveProfiles("dev")
class SecurityConfigTelemetryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelemetryService telemetryService;

    @MockBean
    private TelemetryToggleService telemetryToggleService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @TestConfiguration
    static class TestCorsConfig {
        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            return request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of("*"));
                config.setAllowedMethods(List.of("*"));
                config.setAllowedHeaders(List.of("*"));
                return config;
            };
        }
    }

    @Test
    @DisplayName("passwordEncoder bean is BCrypt")
    void passwordEncoderBeanExists() {
        SecurityConfig config = new SecurityConfig();
        var encoder = config.passwordEncoder();

        org.junit.jupiter.api.Assertions.assertNotNull(encoder);
        org.junit.jupiter.api.Assertions.assertInstanceOf(
                org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class,
                encoder
        );
    }

    @Test
    @DisplayName("POST telemetry is permitted without authentication in dev profile")
    void postTelemetry_permitAll() throws Exception {
        when(telemetryToggleService.isEnabled()).thenReturn(false);

        mockMvc.perform(post("/v1/api/dev/telemetry")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventName": "button_tap",
                                  "sessionId": "session-1"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET telemetry enabled requires authentication")
    void getEnabled_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/api/dev/telemetry/enabled"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT telemetry enabled requires authentication")
    void putEnabled_requiresAuthentication() throws Exception {
        mockMvc.perform(put("/v1/api/dev/telemetry/enabled")
                        .param("enabled", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET telemetry recent requires authentication")
    void getRecent_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/api/dev/telemetry/recent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("authenticated non-admin cannot GET telemetry enabled")
    void getEnabled_forbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/v1/api/dev/telemetry/enabled")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("authenticated non-admin cannot PUT telemetry enabled")
    void putEnabled_forbiddenForNonAdmin() throws Exception {
        mockMvc.perform(put("/v1/api/dev/telemetry/enabled")
                        .param("enabled", "true")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("authenticated non-admin cannot GET telemetry recent")
    void getRecent_forbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/v1/api/dev/telemetry/recent")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin can GET telemetry enabled")
    void getEnabled_allowedForAdmin() throws Exception {
        when(telemetryToggleService.isEnabled()).thenReturn(true);

        mockMvc.perform(get("/v1/api/dev/telemetry/enabled")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("admin can PUT telemetry enabled")
    void putEnabled_allowedForAdmin() throws Exception {
        when(telemetryToggleService.setEnabled(true)).thenReturn(true);

        mockMvc.perform(put("/v1/api/dev/telemetry/enabled")
                        .param("enabled", "true")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("admin can GET telemetry recent")
    void getRecent_allowedForAdmin() throws Exception {
        when(telemetryService.recent(50)).thenReturn(List.of());

        mockMvc.perform(get("/v1/api/dev/telemetry/recent")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OPTIONS request is permitted")
    void optionsRequest_permitted() throws Exception {
        mockMvc.perform(options("/v1/api/dev/telemetry/enabled"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("unknown dev endpoint is denied even for admin")
    void unknownDevEndpoint_denied() throws Exception {
        mockMvc.perform(get("/v1/api/dev/unknown")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}