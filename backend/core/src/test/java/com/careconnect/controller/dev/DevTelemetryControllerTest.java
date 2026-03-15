package com.careconnect.controller.dev;

import com.careconnect.model.TelemetryEvent;
import com.careconnect.service.TelemetryService;
import com.careconnect.service.TelemetryToggleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DevTelemetryController.class)
@ActiveProfiles("dev")
class DevTelemetryControllerTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_ROLE = "ADMIN";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TelemetryService telemetryService;

    @MockBean
    private TelemetryToggleService telemetryToggleService;

    @Test
    @DisplayName("POST returns 204 and does not record when telemetry is disabled")
    void emit_returns204_whenTelemetryDisabled() throws Exception {
        when(telemetryToggleService.isEnabled()).thenReturn(false);

        mockMvc.perform(post("/v1/api/dev/telemetry")
                        .with(user(ADMIN_USERNAME).roles(ADMIN_ROLE))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventName": "button_tap",
                                  "sessionId": "session-1"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(telemetryToggleService).isEnabled();
        verifyNoInteractions(telemetryService);
    }

    @Test
    @DisplayName("POST records telemetry and returns recorded payload when telemetry is enabled")
    void emit_recordsTelemetry_whenEnabled() throws Exception {
        when(telemetryToggleService.isEnabled()).thenReturn(true);

        TelemetryEvent recorded = new TelemetryEvent();
        recorded.setEventName("button_tap");
        recorded.setSessionId("session-123");
        recorded.setTraceId("trace-123");
        recorded.setSpanId("span-123");
        recorded.setDetails(Map.of("screen", "settings"));
        recorded.setDeviceInfo(Map.of("platform", "android"));

        when(telemetryService.record(any(TelemetryEvent.class))).thenReturn(recorded);

        mockMvc.perform(post("/v1/api/dev/telemetry")
                        .with(user(ADMIN_USERNAME).roles(ADMIN_ROLE))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventName": "button_tap",
                                  "sessionId": "session-123",
                                  "traceId": "trace-123",
                                  "spanId": "span-123",
                                  "details": { "screen": "settings" },
                                  "deviceInfo": { "platform": "android" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventName").value("button_tap"))
                .andExpect(jsonPath("$.sessionId").value("session-123"))
                .andExpect(jsonPath("$.traceId").value("trace-123"))
                .andExpect(jsonPath("$.spanId").value("span-123"))
                .andExpect(jsonPath("$.details.screen").value("settings"))
                .andExpect(jsonPath("$.deviceInfo.platform").value("android"));

        ArgumentCaptor<TelemetryEvent> captor = ArgumentCaptor.forClass(TelemetryEvent.class);
        verify(telemetryService).record(captor.capture());

        TelemetryEvent sent = captor.getValue();
        assertThat(sent.getEventName()).isEqualTo("button_tap");
        assertThat(sent.getSessionId()).isEqualTo("session-123");
        assertThat(sent.getTraceId()).isEqualTo("trace-123");
        assertThat(sent.getSpanId()).isEqualTo("span-123");
        assertThat(sent.getDetails()).containsEntry("screen", "settings");
        assertThat(sent.getDeviceInfo()).containsEntry("platform", "android");
        assertThat(sent.getEventTime()).isNotNull();
    }

    @Test
    @DisplayName("POST defaults eventName to dev_emit when missing")
    void emit_defaultsEventName_whenMissing() throws Exception {
        when(telemetryToggleService.isEnabled()).thenReturn(true);

        TelemetryEvent recorded = new TelemetryEvent();
        recorded.setEventName("dev_emit");

        when(telemetryService.record(any(TelemetryEvent.class))).thenReturn(recorded);

        mockMvc.perform(post("/v1/api/dev/telemetry")
                        .with(user(ADMIN_USERNAME).roles(ADMIN_ROLE))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventName").value("dev_emit"));

        ArgumentCaptor<TelemetryEvent> captor = ArgumentCaptor.forClass(TelemetryEvent.class);
        verify(telemetryService).record(captor.capture());

        assertThat(captor.getValue().getEventName()).isEqualTo("dev_emit");
    }

    @Test
    @DisplayName("POST sets details and deviceInfo to null when values are not maps")
    void emit_setsNullForNonMapPayloadSections() throws Exception {
        when(telemetryToggleService.isEnabled()).thenReturn(true);

        TelemetryEvent recorded = new TelemetryEvent();
        recorded.setEventName("button_tap");
        recorded.setDetails(null);
        recorded.setDeviceInfo(null);

        when(telemetryService.record(any(TelemetryEvent.class))).thenReturn(recorded);

        mockMvc.perform(post("/v1/api/dev/telemetry")
                        .with(user(ADMIN_USERNAME).roles(ADMIN_ROLE))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventName": "button_tap",
                                  "details": "not-a-map",
                                  "deviceInfo": 123
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventName").value("button_tap"));

        ArgumentCaptor<TelemetryEvent> captor = ArgumentCaptor.forClass(TelemetryEvent.class);
        verify(telemetryService).record(captor.capture());

        TelemetryEvent sent = captor.getValue();
        assertThat(sent.getDetails()).isNull();
        assertThat(sent.getDeviceInfo()).isNull();
    }

    @Test
    @DisplayName("GET recent returns telemetry service results")
    void recent_returnsServiceResults() throws Exception {
        TelemetryEvent first = new TelemetryEvent();
        first.setEventName("screen_view");
        first.setSessionId("session-1");

        TelemetryEvent second = new TelemetryEvent();
        second.setEventName("button_tap");
        second.setSessionId("session-2");

        when(telemetryService.recent(5)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/v1/api/dev/telemetry/recent")
                        .with(user(ADMIN_USERNAME).roles(ADMIN_ROLE))
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventName").value("screen_view"))
                .andExpect(jsonPath("$[0].sessionId").value("session-1"))
                .andExpect(jsonPath("$[1].eventName").value("button_tap"))
                .andExpect(jsonPath("$[1].sessionId").value("session-2"));

        verify(telemetryService).recent(5);
    }

    @Test
    @DisplayName("GET enabled returns current toggle state")
    void enabled_returnsToggleState() throws Exception {
        when(telemetryToggleService.isEnabled()).thenReturn(true);

        mockMvc.perform(get("/v1/api/dev/telemetry/enabled")
                        .with(user(ADMIN_USERNAME).roles(ADMIN_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(telemetryToggleService).isEnabled();
    }

    @Test
    @DisplayName("PUT enabled updates toggle state")
    void setEnabled_updatesToggleState() throws Exception {
        when(telemetryToggleService.setEnabled(true)).thenReturn(true);

        mockMvc.perform(put("/v1/api/dev/telemetry/enabled")
                        .with(user(ADMIN_USERNAME).roles(ADMIN_ROLE))
                        .with(csrf())
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(telemetryToggleService).setEnabled(true);
    }

    @Test
    @DisplayName("PUT enabled can disable telemetry")
    void setEnabled_canDisable() throws Exception {
        when(telemetryToggleService.setEnabled(false)).thenReturn(false);

        mockMvc.perform(put("/v1/api/dev/telemetry/enabled")
                        .with(user(ADMIN_USERNAME).roles(ADMIN_ROLE))
                        .with(csrf())
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        verify(telemetryToggleService).setEnabled(false);
    }
}