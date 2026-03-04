package com.careconnect.util;

import com.careconnect.security.JwtTokenProvider;
import com.careconnect.security.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    @Mock JwtTokenProvider  jwtTokenProvider;
    @Mock HttpServletRequest request;

    @InjectMocks SecurityUtil securityUtil;

    // ─── getCurrentUser() ─────────────────────────────────────────────────────

    @Test
    void getCurrentUser_nullAuthorizationHeader_throwsRuntimeException() {
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThatThrownBy(() -> securityUtil.getCurrentUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Missing or invalid Authorization header");
    }

    @Test
    void getCurrentUser_headerWithoutBearerPrefix_throwsRuntimeException() {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        assertThatThrownBy(() -> securityUtil.getCurrentUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Missing or invalid Authorization header");
    }

    @Test
    void getCurrentUser_validBearerToken_returnsUserInfo() {
        String token = "valid-jwt-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenProvider.getUsername(token)).thenReturn("user@example.com");
        when(jwtTokenProvider.getRole(token)).thenReturn(Role.PATIENT);

        SecurityUtil.UserInfo info = securityUtil.getCurrentUser(request);

        assertThat(info).isNotNull();
        assertThat(info.email).isEqualTo("user@example.com");
        assertThat(info.role).isEqualTo(Role.PATIENT);
    }

    // ─── UserInfo inner class ─────────────────────────────────────────────────

    @Test
    void userInfo_constructorSetsFields() {
        SecurityUtil.UserInfo info = new SecurityUtil.UserInfo("admin@example.com", Role.ADMIN);

        assertThat(info.email).isEqualTo("admin@example.com");
        assertThat(info.role).isEqualTo(Role.ADMIN);
    }
}
