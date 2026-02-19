import 'package:flutter/foundation.dart';

import 'telemetry_settings.dart';
import 'telemetry_guardrails.dart';

class Telemetry {
  static Future<bool> _enabled() async {
    final optedOut = await TelemetrySettings.isOptedOut();
    return !optedOut;
  }

  static Future<void> event(String name, Map<String, Object?> props) async {
    final enabled = await _enabled();

    if (!enabled) {
      if (kDebugMode) {
        debugPrint('[telemetry] blocked (opted out): $name');
      }
      return;
    }

    final sanitized = TelemetryGuardrails.sanitize(name, props);
    if (sanitized == null) {
      if (kDebugMode) {
        debugPrint('[telemetry] dropped (guardrails): $name');
      }
      return;
    }

    // No exporter yet.
    if (kDebugMode) {
      debugPrint('[telemetry] would send: $name props=$sanitized');
    }

    // Later: send to OTel exporter / backend endpoint.
  }
}
