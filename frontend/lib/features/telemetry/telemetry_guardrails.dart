class TelemetryGuardrails {
  static const Set<String> allowedEvents = {
    'privacy_telemetry_toggle',
    'screen_view',
    'button_tap',
    'error_network',
    'error_timeout',
  };

  static const Set<String> blockedKeys = {
    'name',
    'firstName',
    'lastName',
    'email',
    'phone',
    'address',
    'dob',
    'dateOfBirth',
    'ssn',
    'mrn',
    'patientId',
    'providerId',
    'notes',
    'message',
    'symptom',
    'symptoms',
    'diagnosis',
    'medication',
    'freeText',
  };

  static Map<String, Object?>? sanitize(
    String eventName,
    Map<String, Object?> props,
  ) {
    if (!allowedEvents.contains(eventName)) return null;

    final out = <String, Object?>{};
    for (final entry in props.entries) {
      final k = entry.key;
      if (blockedKeys.contains(k)) continue;

      final v = entry.value;
      if (v == null) continue;

      if (v is String || v is num || v is bool) {
        if (v is String && v.length > 64) continue; // blocks most free-text
        out[k] = v;
      }
    }
    return out;
  }
}
