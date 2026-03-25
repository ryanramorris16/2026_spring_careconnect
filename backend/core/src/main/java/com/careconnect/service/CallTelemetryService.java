package com.careconnect.service;

import com.careconnect.model.CallTelemetryEvent;
import com.careconnect.repository.CallTelemetryEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service that records and summarizes call telemetry events. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallTelemetryService {

  private static final Set<String> LARGE_OR_SENSITIVE_KEYS =
      Set.of(
          "audioBase64",
          "imageBase64",
          "token",
          "joinToken",
          "text",
          "transcript",
          "notes",
          "message",
          "email",
          "phone",
          "address",
          "name");

  private static final Set<String> ALLOWED_TELEMETRY_KEYS =
      Set.of(
          "callId",
          "captureMode",
          "audioFormat",
          "imageFormat",
          "meetingActive",
          "notifiedOtherParty",
          "status",
          "type",
          "textLength",
          "overallScore",
          "overallLabel",
          "timestamp",
          "dbgTs",
          "dbgVs",
          "dbgIs",
          "dbgTw",
          "dbgVw",
          "dbgIw",
          "dbgTc",
          "dbgVc",
          "dbgIc",
          "dbgCf");

  private final CallTelemetryEventRepository callTelemetryEventRepository;
  private final ObjectMapper objectMapper;

  /**
   * Records a general call telemetry event.
   *
   * @param callId call identifier
   * @param eventType event type name
   * @param actorUserId acting user identifier
   * @param targetUserId target user identifier
   * @param status event status
   * @param metadata optional event metadata
   * @param errorMessage optional error message
   */
  public void recordCallEvent(
      final String callId,
      final String eventType,
      final Long actorUserId,
      final Long targetUserId,
      final String status,
      final Map<String, Object> metadata,
      final String errorMessage) {
    final CallTelemetryEvent event =
        buildBaseEvent(callId, eventType, "REST", actorUserId, targetUserId, status, errorMessage);
    event.setMetadataJson(toJsonSafe(metadata));
    callTelemetryEventRepository.save(event);
  }

  /**
   * Records a sentiment-related telemetry event for a call.
   *
   * @param callId call identifier
   * @param eventType event type name
   * @param channel sentiment channel
   * @param actorUserId acting user identifier
   * @param targetUserId target user identifier
   * @param captureMode capture mode used for the event
   * @param result sentiment analysis result
   * @param payload optional event payload
   * @param status event status
   * @param errorMessage optional error message
   */
  public void recordSentimentEvent(
      final String callId,
      final String eventType,
      final String channel,
      final Long actorUserId,
      final Long targetUserId,
      final String captureMode,
      final BedrockSentimentService.SentimentResult result,
      final Map<String, Object> payload,
      final String status,
      final String errorMessage) {
    final CallTelemetryEvent event =
        buildBaseEvent(callId, eventType, "REST", actorUserId, targetUserId, status, errorMessage);
    event.setChannel(trim(channel));
    event.setCaptureMode(trim(captureMode));
    applySentimentResult(event, result);
    event.setPayloadJson(toJsonSafe(sanitizePayload(payload)));
    callTelemetryEventRepository.save(event);
  }

  /**
   * Records a WebSocket-originated telemetry event.
   *
   * @param callId call identifier
   * @param eventType event type name
   * @param actorUserId acting user identifier
   * @param targetUserId target user identifier
   * @param payload optional payload details
   * @param status event status
   * @param errorMessage optional error message
   */
  public void recordWebSocketEvent(
      final String callId,
      final String eventType,
      final Long actorUserId,
      final Long targetUserId,
      final Map<String, Object> payload,
      final String status,
      final String errorMessage) {
    final CallTelemetryEvent event =
        buildBaseEvent(
            callId,
            eventType,
            "WEBSOCKET",
            actorUserId,
            targetUserId,
            status,
            errorMessage);
    event.setPayloadJson(toJsonSafe(sanitizePayload(payload)));
    callTelemetryEventRepository.save(event);
  }

  /**
   * Returns telemetry events for a call in reverse chronological order.
   *
   * @param callId call identifier
   * @return telemetry events for the call
   */
  public List<CallTelemetryEvent> getTelemetryForCall(final String callId) {
    return callTelemetryEventRepository.findByCallIdOrderByOccurredAtDesc(callId);
  }

  /**
   * Returns the latest sentiment event for each channel on a call.
   *
   * @param callId call identifier
   * @return latest sentiment event by channel
   */
  public Map<String, CallTelemetryEvent> getLatestSentimentByChannel(final String callId) {
    final List<CallTelemetryEvent> events =
        callTelemetryEventRepository.findByCallIdOrderByOccurredAtDesc(callId);
    final Map<String, CallTelemetryEvent> latestByChannel = new LinkedHashMap<>();
    for (final CallTelemetryEvent event : events) {
      final String channel = normalizedChannel(event);
      if (channel == null) {
        continue;
      }
      if (!latestByChannel.containsKey(channel)) {
        latestByChannel.put(channel, event);
      }
      if (hasCoreChannels(latestByChannel)) {
        break;
      }
    }
    return latestByChannel;
  }

  public List<CallTelemetryEvent> getTelemetryForUser(final Long userId) {
    return callTelemetryEventRepository.findTop500ByActorUserIdOrTargetUserIdOrderByOccurredAtDesc(
        userId, userId);
  }

  /**
   * Builds summarized sentiment history entries for a user across calls.
   *
   * @param userId user identifier
   * @return summarized sentiment history entries
   */
  public List<Map<String, Object>> getSentimentHistoryForUser(final Long userId) {
    if (userId == null) {
      return List.of();
    }

    final List<CallTelemetryEvent> events =
        callTelemetryEventRepository.findByActorUserIdOrTargetUserIdOrderByOccurredAtAsc(
            userId, userId);
    if (events.isEmpty()) {
      return List.of();
    }

    final Map<String, List<CallTelemetryEvent>> byCall =
        events.stream()
            .filter(event -> event.getCallId() != null && !event.getCallId().isBlank())
            .collect(
                Collectors.groupingBy(
                    CallTelemetryEvent::getCallId, LinkedHashMap::new, Collectors.toList()));

    final List<Map<String, Object>> summaries = new ArrayList<>();
    for (final Map.Entry<String, List<CallTelemetryEvent>> entry : byCall.entrySet()) {
      final Map<String, Object> summary = summarizeCall(entry.getKey(), entry.getValue());
      if (!summary.isEmpty()) {
        summaries.add(summary);
      }
    }

    summaries.sort(
        (a, b) -> {
          LocalDateTime left = (LocalDateTime) a.getOrDefault("_sortDate", LocalDateTime.MIN);
          LocalDateTime right = (LocalDateTime) b.getOrDefault("_sortDate", LocalDateTime.MIN);
          return right.compareTo(left);
        });
    summaries.forEach(item -> item.remove("_sortDate"));
    return summaries;
  }

  /**
   * Deletes all telemetry events for a call.
   *
   * @param callId call identifier
   * @return number of deleted events
   */
  @Transactional
  public long deleteTelemetryForCall(final String callId) {
    final String normalizedCallId = trim(callId);
    final long deletedCount;
    if (normalizedCallId == null) {
      deletedCount = 0;
    } else {
      deletedCount = callTelemetryEventRepository.deleteByCallId(normalizedCallId);
    }
    return deletedCount;
  }

  /**
   * Finds telemetry history associated with a patient across matched calls.
   *
   * @param patientUserId patient user identifier
   * @return matched events and call identifiers
   */
  public PatientCallHistoryMatch findCallHistoryForPatient(final Long patientUserId) {
    if (patientUserId == null) {
      return new PatientCallHistoryMatch(List.of(), Set.of());
    }

    final List<CallTelemetryEvent> matchedEvents =
        callTelemetryEventRepository.findAll().stream()
            .filter(event -> eventMatchesPatientHistory(event, patientUserId))
            .toList();

    final Set<String> callIds =
        matchedEvents.stream()
            .map(CallTelemetryEvent::getCallId)
            .map(this::trim)
            .filter(value -> value != null)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    return new PatientCallHistoryMatch(matchedEvents, callIds);
  }

  /**
   * Deletes a collection of telemetry events by their identifiers.
   *
   * @param events telemetry events to delete
   * @return number of deleted events
   */
  @Transactional
  public long deleteTelemetryEvents(final Collection<CallTelemetryEvent> events) {
    final long deletedCount;
    if (events == null || events.isEmpty()) {
      deletedCount = 0;
    } else {
      final List<Long> ids =
          events.stream()
              .map(CallTelemetryEvent::getId)
              .filter(id -> id != null)
              .distinct()
              .toList();
      if (ids.isEmpty()) {
        deletedCount = 0;
      } else {
        callTelemetryEventRepository.deleteAllByIdInBatch(ids);
        deletedCount = ids.size();
      }
    }
    return deletedCount;
  }


  private CallTelemetryEvent buildBaseEvent(
      final String callId,
      final String eventType,
      final String eventSource,
      final Long actorUserId,
      final Long targetUserId,
      final String status,
      final String errorMessage) {
    final CallTelemetryEvent event = new CallTelemetryEvent();
    event.setCallId(trim(callId));
    event.setEventType(eventType);
    event.setEventSource(eventSource);
    event.setActorUserId(actorUserId);
    event.setTargetUserId(targetUserId);
    event.setStatus(defaultStatus(status));
    event.setErrorMessage(trim(errorMessage));
    event.setOccurredAt(LocalDateTime.now());
    return event;
  }

  private void applySentimentResult(
      final CallTelemetryEvent event,
      final BedrockSentimentService.SentimentResult result) {
    if (result != null) {
      event.setSentimentScore(result.score());
      event.setSentimentLabel(trim(result.label()));
      event.setSentimentNotes(trim(result.notes()));
      event.setAnalysisTimestamp(result.timestamp());
    }
  }

  private static boolean hasCoreChannels(
      final Map<String, CallTelemetryEvent> latestByChannel) {
    return latestByChannel.containsKey("TEXT")
        && latestByChannel.containsKey("VOICE")
        && latestByChannel.containsKey("VIDEO");
  }

  private static String normalizedChannel(final CallTelemetryEvent event) {
    final String channel;
    if (event == null || event.getChannel() == null || event.getSentimentScore() == null) {
      channel = null;
    } else {
      final String trimmedChannel = event.getChannel().trim().toUpperCase(Locale.ROOT);
      channel = trimmedChannel.isEmpty() ? null : trimmedChannel;
    }
    return channel;
  }

  private Map<String, Object> sanitizePayload(Map<String, Object> payload) {
    if (payload == null || payload.isEmpty()) {
      return Map.of();
    }

    Map<String, Object> sanitized = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : payload.entrySet()) {
      String key = entry.getKey();
      String normalizedKey = key == null ? "" : key.trim();
      String lowerKey = normalizedKey.toLowerCase(Locale.ROOT);

      if (lowerKey.isEmpty()) {
        continue;
      }

      if (LARGE_OR_SENSITIVE_KEYS.contains(normalizedKey)
          || lowerKey.contains("token")
          || lowerKey.contains("secret")
          || lowerKey.contains("password")
          || lowerKey.contains("audio")
          || lowerKey.contains("image")
          || lowerKey.contains("transcript")
          || lowerKey.contains("email")
          || lowerKey.contains("phone")
          || lowerKey.contains("address")
          || lowerKey.contains("name")) {
        Object value = entry.getValue();
        if (value == null) {
          sanitized.put(normalizedKey, null);
        } else {
          int length = value.toString().length();
          sanitized.put(normalizedKey, "[REDACTED:" + length + " chars]");
        }
        continue;
      }

      if (!ALLOWED_TELEMETRY_KEYS.contains(normalizedKey)) {
        sanitized.put(normalizedKey, "[OMITTED]");
        continue;
      }

      Object value = entry.getValue();
      if (value instanceof String textValue && textValue.length() > 180) {
        sanitized.put(normalizedKey, "[TRUNCATED:" + textValue.length() + " chars]");
        continue;
      }

      sanitized.put(normalizedKey, value);
    }
    return sanitized;
  }

  private String toJsonSafe(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.warn("Failed to serialize telemetry payload", ex);
      return "{}";
    }
  }

  private String trim(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String defaultStatus(String status) {
    String normalized = trim(status);
    return normalized == null ? "SUCCESS" : normalized;
  }

  private boolean eventMatchesPatientHistory(CallTelemetryEvent event, Long patientUserId) {
    if (event == null || patientUserId == null) {
      return false;
    }
    if (patientUserId.equals(event.getActorUserId())
        || patientUserId.equals(event.getTargetUserId())) {
      return true;
    }

    Map<String, Object> metadata = parseJsonMap(event.getMetadataJson());
    if (containsPatientContext(metadata.get("contextPatientUserIds"), patientUserId)) {
      return true;
    }
    Long singleContextId = toLong(metadata.get("contextPatientUserId"));
    return patientUserId.equals(singleContextId);
  }

  private boolean containsPatientContext(Object rawValue, Long patientUserId) {
    if (!(rawValue instanceof List<?> values) || patientUserId == null) {
      return false;
    }
    for (Object value : values) {
      if (patientUserId.equals(toLong(value))) {
        return true;
      }
    }
    return false;
  }

  private Map<String, Object> parseJsonMap(String rawJson) {
    String normalized = trim(rawJson);
    if (normalized == null) {
      return Map.of();
    }
    try {
      Object decoded = objectMapper.readValue(normalized, Object.class);
      if (decoded instanceof Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          String key = entry.getKey() == null ? "" : entry.getKey().toString();
          if (!key.isBlank()) {
            result.put(key, entry.getValue());
          }
        }
        return result;
      }
    } catch (Exception ex) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to parse telemetry metadata JSON: {}", ex.getMessage());
      }
    }
    return Map.of();
  }

  private Long toLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    String normalized = value == null ? null : trim(value.toString());
    if (normalized == null) {
      return null;
    }
    try {
      return Long.parseLong(normalized);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private Map<String, Object> summarizeCall(String callId, List<CallTelemetryEvent> allEvents) {
    if (allEvents == null || allEvents.isEmpty()) {
      return Map.of();
    }

    List<CallTelemetryEvent> sorted =
        allEvents.stream()
            .filter(e -> e.getOccurredAt() != null)
            .sorted(Comparator.comparing(CallTelemetryEvent::getOccurredAt))
            .toList();
    if (sorted.isEmpty()) {
      return Map.of();
    }

    LocalDateTime callStart =
        sorted.stream()
            .filter(e -> "CALL_JOIN".equalsIgnoreCase(e.getEventType()))
            .map(CallTelemetryEvent::getOccurredAt)
            .findFirst()
            .orElse(sorted.get(0).getOccurredAt());

    LocalDateTime callEnd =
        sorted.stream()
            .filter(e -> "CALL_END".equalsIgnoreCase(e.getEventType()))
            .map(CallTelemetryEvent::getOccurredAt)
            .reduce((first, second) -> second)
            .orElse(sorted.get(sorted.size() - 1).getOccurredAt());

    final long totalSeconds = Math.max(1L, Duration.between(callStart, callEnd).getSeconds());

    final List<CallTelemetryEvent> timelineSamples =
        sorted.stream()
            .filter(e -> e.getSentimentScore() != null && e.getSentimentLabel() != null)
            .filter(
                e -> {
                  String channel =
                      e.getChannel() == null ? "" : e.getChannel().trim().toUpperCase(Locale.ROOT);
                  if (channel.isEmpty() || "COMBINED".equals(channel)) {
                    return false;
                  }
                  String eventType =
                      e.getEventType() == null
                          ? ""
                          : e.getEventType().trim().toUpperCase(Locale.ROOT);
                  return eventType.startsWith("SENTIMENT_") && !"SENTIMENT_FINAL".equals(eventType);
                })
            .toList();

    Map<String, Long> durationByBucket = new HashMap<>();
    durationByBucket.put("CALM", 0L);
    durationByBucket.put("ANXIOUS", 0L);
    durationByBucket.put("DISTRESSED", 0L);

    if (!timelineSamples.isEmpty()) {
      for (int i = 0; i < timelineSamples.size(); i++) {
        CallTelemetryEvent current = timelineSamples.get(i);
        LocalDateTime from = current.getOccurredAt();
        LocalDateTime to =
            i < timelineSamples.size() - 1 ? timelineSamples.get(i + 1).getOccurredAt() : callEnd;
        long segmentSeconds = Math.max(1L, Duration.between(from, to).getSeconds());
        String bucket = normalizeLabel(current.getSentimentLabel());
        durationByBucket.put(bucket, durationByBucket.getOrDefault(bucket, 0L) + segmentSeconds);
      }
    }

    CallTelemetryEvent finalEvent =
        sorted.stream()
            .filter(e -> "SENTIMENT_FINAL".equalsIgnoreCase(e.getEventType()))
            .filter(e -> e.getSentimentScore() != null)
            .reduce((first, second) -> second)
            .orElse(null);

    double overallScore;
    String overallLabel;
    if (finalEvent != null) {
      overallScore = clamp(finalEvent.getSentimentScore());
      overallLabel = normalizeLabel(finalEvent.getSentimentLabel());
    } else if (!timelineSamples.isEmpty()) {
      overallScore =
          clamp(
              timelineSamples.stream()
                  .map(CallTelemetryEvent::getSentimentScore)
                  .filter(v -> v != null)
                  .mapToDouble(Double::doubleValue)
                  .average()
                  .orElse(0.5));
      overallLabel = labelFromScore(overallScore);
    } else {
      return Map.of();
    }

    List<Double> sampleScores =
        timelineSamples.stream()
            .map(CallTelemetryEvent::getSentimentScore)
            .filter(v -> v != null)
            .map(this::clamp)
            .toList();
    if (sampleScores.isEmpty()) {
      sampleScores = List.of(overallScore);
    }

    final double stabilityScore = computeStability(sampleScores);
    double calmPct = percent(durationByBucket.get("CALM"), totalSeconds);
    double anxiousPct = percent(durationByBucket.get("ANXIOUS"), totalSeconds);
    double distressedPct = percent(durationByBucket.get("DISTRESSED"), totalSeconds);

    if (timelineSamples.isEmpty()) {
      if ("CALM".equals(overallLabel)) {
        calmPct = 1.0;
      } else if ("DISTRESSED".equals(overallLabel)) {
        distressedPct = 1.0;
      } else {
        anxiousPct = 1.0;
      }
    }

    Map<String, Object> output = new LinkedHashMap<>();
    output.put("callId", callId);
    output.put("callDate", callStart);
    output.put("durationMinutes", round(totalSeconds / 60.0));
    output.put("overallScore", round(overallScore));
    output.put("overallLabel", overallLabel);
    output.put("positiveTimePct", round(calmPct));
    output.put("neutralTimePct", round(anxiousPct));
    output.put("negativeTimePct", round(distressedPct));
    output.put("stabilityScore", round(stabilityScore));
    output.put("_sortDate", callStart);
    return output;
  }

  private String normalizeLabel(String label) {
    String normalized = label == null ? "" : label.trim().toUpperCase(Locale.ROOT);
    if (normalized.contains("CALM") || normalized.contains("POSITIVE")) {
      return "CALM";
    }
    if (normalized.contains("DISTRESS") || normalized.contains("NEGATIVE")) {
      return "DISTRESSED";
    }
    return "ANXIOUS";
  }

  private String labelFromScore(double score) {
    if (score >= 0.67) {
      return "CALM";
    }
    if (score < 0.34) {
      return "DISTRESSED";
    }
    return "ANXIOUS";
  }

  private double computeStability(List<Double> values) {
    if (values == null || values.isEmpty()) {
      return 0.0;
    }
    if (values.size() == 1) {
      return 1.0;
    }
    double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
    double stdDev = Math.sqrt(variance);
    return clamp(1.0 - stdDev);
  }

  private double percent(long part, long total) {
    if (total <= 0) {
      return 0.0;
    }
    return clamp((double) part / (double) total);
  }

  private double round(double value) {
    return Math.round(value * 1000.0) / 1000.0;
  }

  private double clamp(double value) {
    if (value < 0.0) {
      return 0.0;
    }
    if (value > 1.0) {
      return 1.0;
    }
    return value;
  }

  /** Matched telemetry history for a patient across related calls. */
  public record PatientCallHistoryMatch(List<CallTelemetryEvent> events, Set<String> callIds) {
    public PatientCallHistoryMatch {
      events = events == null ? List.of() : List.copyOf(events);
      callIds = callIds == null ? Set.of() : Set.copyOf(callIds);
    }
  }
}
