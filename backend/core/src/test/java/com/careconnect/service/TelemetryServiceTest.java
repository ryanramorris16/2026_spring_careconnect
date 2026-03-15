package com.careconnect.service;

import com.careconnect.model.TelemetryEvent;
import com.careconnect.repository.TelemetryEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryEventRepository repository;

    @Mock
    private TelemetryToggleService toggle;

    @InjectMocks
    private TelemetryService telemetryService;

    private TelemetryEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = TelemetryEvent.builder()
                .eventName("button_tap")
                .traceId("trace-123")
                .spanId("span-123")
                .details(Map.of("screen", "dashboard"))
                .deviceInfo(Map.of("platform", "web"))
                .eventTime(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("record returns original event and does not save when telemetry is disabled")
    void record_returnsOriginalEvent_whenTelemetryDisabled() {
        when(toggle.isEnabled()).thenReturn(false);

        TelemetryEvent result = telemetryService.record(sampleEvent);

        assertThat(result).isSameAs(sampleEvent);
        verify(toggle).isEnabled();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("record saves and returns persisted event when telemetry is enabled")
    void record_savesEvent_whenTelemetryEnabled() {
        when(toggle.isEnabled()).thenReturn(true);
        when(repository.save(sampleEvent)).thenReturn(sampleEvent);

        TelemetryEvent result = telemetryService.record(sampleEvent);

        assertThat(result).isSameAs(sampleEvent);
        verify(toggle).isEnabled();
        verify(repository).save(sampleEvent);
    }

    @Test
    @DisplayName("recent returns empty list when repository returns null")
    void recent_returnsEmptyList_whenRepositoryReturnsNull() {
        when(repository.findTop50ByOrderByEventTimeDesc()).thenReturn(null);

        List<TelemetryEvent> result = telemetryService.recent(10);

        assertThat(result).isEmpty();
        verify(repository).findTop50ByOrderByEventTimeDesc();
    }

    @Test
    @DisplayName("recent returns empty list when repository returns empty list")
    void recent_returnsEmptyList_whenRepositoryReturnsEmpty() {
        when(repository.findTop50ByOrderByEventTimeDesc()).thenReturn(List.of());

        List<TelemetryEvent> result = telemetryService.recent(10);

        assertThat(result).isEmpty();
        verify(repository).findTop50ByOrderByEventTimeDesc();
    }

    @Test
    @DisplayName("recent returns all results when size is less than safe limit")
    void recent_returnsAllResults_whenSizeLessThanSafeLimit() {
        TelemetryEvent first = TelemetryEvent.builder().eventName("event-1").build();
        TelemetryEvent second = TelemetryEvent.builder().eventName("event-2").build();

        when(repository.findTop50ByOrderByEventTimeDesc()).thenReturn(List.of(first, second));

        List<TelemetryEvent> result = telemetryService.recent(10);

        assertThat(result).containsExactly(first, second);
        verify(repository).findTop50ByOrderByEventTimeDesc();
    }

    @Test
    @DisplayName("recent returns sublist when result size exceeds safe limit")
    void recent_returnsSublist_whenSizeExceedsSafeLimit() {
        TelemetryEvent first = TelemetryEvent.builder().eventName("event-1").build();
        TelemetryEvent second = TelemetryEvent.builder().eventName("event-2").build();
        TelemetryEvent third = TelemetryEvent.builder().eventName("event-3").build();

        when(repository.findTop50ByOrderByEventTimeDesc()).thenReturn(List.of(first, second, third));

        List<TelemetryEvent> result = telemetryService.recent(2);

        assertThat(result).containsExactly(first, second);
        verify(repository).findTop50ByOrderByEventTimeDesc();
    }

    @Test
    @DisplayName("recent clamps low limit to 1")
    void recent_clampsLowLimitToOne() {
        TelemetryEvent first = TelemetryEvent.builder().eventName("event-1").build();
        TelemetryEvent second = TelemetryEvent.builder().eventName("event-2").build();

        when(repository.findTop50ByOrderByEventTimeDesc()).thenReturn(List.of(first, second));

        List<TelemetryEvent> result = telemetryService.recent(0);

        assertThat(result).containsExactly(first);
        verify(repository).findTop50ByOrderByEventTimeDesc();
    }

    @Test
    @DisplayName("recent clamps high limit to 200")
    void recent_clampsHighLimitToTwoHundred() {
        TelemetryEvent first = TelemetryEvent.builder().eventName("event-1").build();
        TelemetryEvent second = TelemetryEvent.builder().eventName("event-2").build();

        when(repository.findTop50ByOrderByEventTimeDesc()).thenReturn(List.of(first, second));

        List<TelemetryEvent> result = telemetryService.recent(500);

        assertThat(result).containsExactly(first, second);
        verify(repository).findTop50ByOrderByEventTimeDesc();
    }

    @Test
    @DisplayName("recordAnonymous returns null and does not save when telemetry is disabled")
    void recordAnonymous_returnsNull_whenTelemetryDisabled() {
        when(toggle.isEnabled()).thenReturn(false);

        TelemetryEvent result = telemetryService.recordAnonymous(
                "screen_view",
                Map.of("page", "settings"),
                Map.of("platform", "web"),
                "trace-1",
                "span-1"
        );

        assertThat(result).isNull();
        verify(toggle).isEnabled();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("recordAnonymous builds event and saves when telemetry is enabled")
    void recordAnonymous_buildsAndSavesEvent_whenTelemetryEnabled() {
        when(toggle.isEnabled()).thenReturn(true);

        TelemetryEvent saved = TelemetryEvent.builder()
                .eventName("screen_view")
                .traceId("trace-1")
                .spanId("span-1")
                .details(Map.of("page", "settings"))
                .deviceInfo(Map.of("platform", "web"))
                .build();

        when(repository.save(any(TelemetryEvent.class))).thenReturn(saved);

        TelemetryEvent result = telemetryService.recordAnonymous(
                "screen_view",
                Map.of("page", "settings"),
                Map.of("platform", "web"),
                "trace-1",
                "span-1"
        );

        assertThat(result).isSameAs(saved);

        ArgumentCaptor<TelemetryEvent> captor = ArgumentCaptor.forClass(TelemetryEvent.class);
        verify(repository).save(captor.capture());

        TelemetryEvent captured = captor.getValue();
        assertThat(captured.getEventName()).isEqualTo("screen_view");
        assertThat(captured.getTraceId()).isEqualTo("trace-1");
        assertThat(captured.getSpanId()).isEqualTo("span-1");
        assertThat(captured.getDetails()).containsEntry("page", "settings");
        assertThat(captured.getDeviceInfo()).containsEntry("platform", "web");
    }
}