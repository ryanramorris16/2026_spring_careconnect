package com.careconnect.config;

import com.careconnect.security.JwtAuthenticationFilter;
import com.careconnect.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtTokenProvider jwt,
                                    UserDetailsService uds,
                                    CorsConfigurationSource corsConfigurationSource) throws Exception {

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwt, uds);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(
                        (req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Basic Authentication Required")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                .authorizeHttpRequests(auth -> auth
                        /* ---------- Swagger/OpenAPI docs ------------------------------ */
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/v3/api-docs",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/swagger-ui/index.html",
                                "/api-docs/**",
                                "/configuration/ui",
                                "/configuration/security"
                        ).permitAll()

                /* ---------- public API endpoints ------------------------ */
                .requestMatchers(
                        "/v1/api/auth/**",
                        "/api/v1/auth/**",  // Support both URL patterns
                        "/api/auth/**",     // Support auth endpoints under /api/auth/
                        "/v1/api/users/reset-password",  // Allow password reset (current)
                        "/v1/api/users/setup-password",
                        // "/v1/api/email-test/**" removed from permitAll — now requires ADMIN role (see below)
                        "/v1/api/test/**", // Allow test endpoints (health check, swagger info)
                        "/v1/api/subscriptions/webhook/**", // Stripe webhook callbacks (no JWT)
                        "/oauth/**"// Permit OAuth paths
                ).permitAll()

                /* ---------- public static assets ------------------------ */
                .requestMatchers(
                        "/", "/index.html", "/favicon.ico", "/static/**"
                ).permitAll()

                        /* ---------- Static assets ------------------------------------- */
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/static/**").permitAll()

                        /* ---------- Admin-only endpoints ------------------------------- */
                        .requestMatchers("/v1/api/debug/**").hasRole("ADMIN")
                        .requestMatchers("/v1/api/email-test/**").hasRole("ADMIN")

                        /* ---------- Require JWT for these APIs ------------------------ */
                        .requestMatchers("/v1/api/patients/**").authenticated()
                        .requestMatchers("/v1/api/caregivers/**").authenticated()
                        .requestMatchers("/v1/api/allergies/**").authenticated()
                        .requestMatchers("/v1/api/symptoms/**").authenticated()
                        .requestMatchers("/v1/api/ai/**", "/api/ai/**").authenticated()
                        .requestMatchers("/v1/api/ai/deepseek/**").authenticated()
                        .requestMatchers("/v1/api/family-members/**").authenticated()
                        .requestMatchers("/v1/api/ai-chat/**").authenticated()
                        .requestMatchers("/v1/api/users/**").authenticated()
                        .requestMatchers("/v1/api/tasks/**").authenticated()
                        .requestMatchers("/v2/api/tasks/**").authenticated()
                        .requestMatchers("/v1/api/messages/**").authenticated()
                        .requestMatchers("/v1/api/invoices/**").authenticated()
                        .requestMatchers("/v1/api/subscriptions/**").authenticated()
                        .requestMatchers("/v1/api/evv/**").authenticated()
                        .requestMatchers("/v1/api/notifications/**").authenticated()
                        .requestMatchers("/v1/api/notification-settings/**").authenticated()
                        .requestMatchers("/v1/api/friends/**").authenticated()
                        .requestMatchers("/v1/api/connection-requests/**").authenticated()
                        .requestMatchers("/v1/api/feed/**").authenticated()
                        .requestMatchers("/v1/api/comments/**").authenticated()
                        .requestMatchers("/v1/api/files/**").authenticated()
                        .requestMatchers("/v1/api/templates/**").authenticated()
                        .requestMatchers("/v1/api/analytics/**").authenticated()
                        .requestMatchers("/v1/api/scheduled-visits/**").authenticated()
                        .requestMatchers("/v1/api/patient-notetaker/**").authenticated()
                        .requestMatchers("/v1/api/link-management/**").authenticated()
                        .requestMatchers("/v1/api/caregiver-patient-links/**").authenticated()
                        .requestMatchers("/v1/api/symptoms-entry/**").authenticated()
                        .requestMatchers("/v1/api/alexa/**").authenticated()
                        .requestMatchers("/v1/api/usps/**", "/api/usps/**").authenticated()
                        .requestMatchers("/v1/api/questions/**", "/api/questions/**").authenticated()
                        .requestMatchers("/v1/checkins/**", "/api/checkins/**").authenticated()
                        .requestMatchers("/api/patient/**").authenticated()
                        .requestMatchers("/api/gamification/**").authenticated()
                        .requestMatchers("/api/websocket/**").authenticated()
                        .requestMatchers("/api/email-credentials/**").authenticated()

                        /* ---------- Everything else: deny (safer default) ------------- */
                        .anyRequest().denyAll()
                )
                .build();
    }


    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
