package com.careconnect.service;

import com.careconnect.model.TelemetryEvent;
import com.careconnect.repository.TelemetryEventRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Service that records and queries application telemetry events. */
@Service
@RequiredArgsConstructor
public class TelemetryService {

  /** Repository used to persist telemetry events. */
  private final TelemetryEventRepository repository;

  /** Feature toggle used to enable or disable telemetry collection. */
  private final TelemetryToggleService toggle;

  /**
   * Records a telemetry event when telemetry is enabled.
   *
   * @param event telemetry event to store
   * @return stored event, or the original event when telemetry is disabled
   */
  public TelemetryEvent record(final TelemetryEvent event) {
    final TelemetryEvent recordedEvent;
    if (toggle.isEnabled()) {
      recordedEvent = repository.save(event);
    } else {
      recordedEvent = event;
    }
    return recordedEvent;
  }

  /**
   * Returns the most recent telemetry events up to the requested limit.
   *
   * @param limit requested number of events
   * @return recent telemetry events
   */
  public List<TelemetryEvent> recent(final int limit) {
    final int safe = Math.max(1, Math.min(limit, 200));
    final List<TelemetryEvent> results = repository.findTop50ByOrderByEventTimeDesc();
    final List<TelemetryEvent> recentEvents;

    if (results == null || results.isEmpty()) {
      recentEvents = Collections.emptyList();
    } else if (results.size() <= safe) {
      recentEvents = results;
    } else {
      recentEvents = results.subList(0, safe);
    }
    return recentEvents;
  }

  /**
   * Records anonymous feature telemetry without user identifiers.
   *
   * @param eventName event name to store
   * @param details optional event details
   * @param deviceInfo optional device metadata
   * @param traceId distributed trace identifier
   * @param spanId distributed span identifier
   * @return stored event, or {@code null} when telemetry is disabled
   */
  public TelemetryEvent recordAnonymous(
      final String eventName,
      final Map<String, Object> details,
      final Map<String, Object> deviceInfo,
      final String traceId,
      final String spanId) {
    final TelemetryEvent recordedEvent;
    if (toggle.isEnabled()) {
      final TelemetryEvent event = new TelemetryEvent();
      event.setEventName(eventName);
      event.setTraceId(traceId);
      event.setSpanId(spanId);
      event.setDetails(details);
      event.setDeviceInfo(deviceInfo);
      recordedEvent = repository.save(event);
    } else {
      recordedEvent = null;
    }
    return recordedEvent;
  }
}
