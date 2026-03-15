package com.careconnect.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryEventTest {

    @Test
    @DisplayName("builder sets all fields correctly")
    void builder_setsAllFields() {
        OffsetDateTime eventTime = OffsetDateTime.parse("2026-03-15T10:15:30Z");

        TelemetryEvent event = TelemetryEvent.builder()
                .id(1L)
                .eventName("button_tap")
                .eventTime(eventTime)
                .sessionId("session-123")
                .traceId("trace-123")
                .spanId("span-123")
                .deviceInfo(Map.of("platform", "android"))
                .details(Map.of("screen", "settings"))
                .build();

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getEventName()).isEqualTo("button_tap");
        assertThat(event.getEventTime()).isEqualTo(eventTime);
        assertThat(event.getSessionId()).isEqualTo("session-123");
        assertThat(event.getTraceId()).isEqualTo("trace-123");
        assertThat(event.getSpanId()).isEqualTo("span-123");
        assertThat(event.getDeviceInfo()).containsEntry("platform", "android");
        assertThat(event.getDetails()).containsEntry("screen", "settings");
    }

    @Test
    @DisplayName("setters and getters work")
    void settersAndGetters_work() {
        OffsetDateTime eventTime = OffsetDateTime.parse("2026-03-15T10:15:30Z");

        TelemetryEvent event = new TelemetryEvent();
        event.setId(2L);
        event.setEventName("screen_view");
        event.setEventTime(eventTime);
        event.setSessionId("session-456");
        event.setTraceId("trace-456");
        event.setSpanId("span-456");
        event.setDeviceInfo(Map.of("platform", "ios"));
        event.setDetails(Map.of("screen", "dashboard"));

        assertThat(event.getId()).isEqualTo(2L);
        assertThat(event.getEventName()).isEqualTo("screen_view");
        assertThat(event.getEventTime()).isEqualTo(eventTime);
        assertThat(event.getSessionId()).isEqualTo("session-456");
        assertThat(event.getTraceId()).isEqualTo("trace-456");
        assertThat(event.getSpanId()).isEqualTo("span-456");
        assertThat(event.getDeviceInfo()).containsEntry("platform", "ios");
        assertThat(event.getDetails()).containsEntry("screen", "dashboard");
    }

    @Test
    @DisplayName("onCreate sets eventTime when null")
    void onCreate_setsEventTimeWhenNull() throws Exception {
        TelemetryEvent event = new TelemetryEvent();
        event.setEventName("test_event");

        assertThat(event.getEventTime()).isNull();

        Method onCreate = TelemetryEvent.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(event);

        assertThat(event.getEventTime()).isNotNull();
    }

    @Test
    @DisplayName("onCreate does not overwrite existing eventTime")
    void onCreate_doesNotOverwriteExistingEventTime() throws Exception {
        OffsetDateTime originalTime = OffsetDateTime.parse("2026-03-15T10:15:30Z");

        TelemetryEvent event = new TelemetryEvent();
        event.setEventName("test_event");
        event.setEventTime(originalTime);

        Method onCreate = TelemetryEvent.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(event);

        assertThat(event.getEventTime()).isEqualTo(originalTime);
    }

    @Test
    @DisplayName("all args constructor sets fields correctly")
    void allArgsConstructor_setsFields() {
        OffsetDateTime eventTime = OffsetDateTime.parse("2026-03-15T10:15:30Z");

        TelemetryEvent event = new TelemetryEvent(
                3L,
                "nav_click",
                eventTime,
                "session-789",
                "trace-789",
                "span-789",
                Map.of("platform", "web"),
                Map.of("target", "profile")
        );

        assertThat(event.getId()).isEqualTo(3L);
        assertThat(event.getEventName()).isEqualTo("nav_click");
        assertThat(event.getEventTime()).isEqualTo(eventTime);
        assertThat(event.getSessionId()).isEqualTo("session-789");
        assertThat(event.getTraceId()).isEqualTo("trace-789");
        assertThat(event.getSpanId()).isEqualTo("span-789");
        assertThat(event.getDeviceInfo()).containsEntry("platform", "web");
        assertThat(event.getDetails()).containsEntry("target", "profile");
    }
}