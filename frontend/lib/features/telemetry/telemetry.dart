import 'package:flutter/foundation.dart';

import 'telemetry_settings.dart';
import 'telemetry_guardrails.dart';
/// Created 2/19/2026: Team B
/// The [Telemetry] class acts as a secure gateway between the application 
/// and external analytics collectors. It ensures user privacy and data 
/// integrity by enforcing three distinct layers of validation:

class Telemetry {
  static Future<bool> _enabled() async {
    final optedOut = await TelemetrySettings.isOptedOut();
    return !optedOut;
  }
  /// Records an analytical event with associated properties.
  ///
  /// This method is asynchronous and fail-safe. If the event is blocked by 
  /// privacy settings or fails the [TelemetryGuardrails] validation, it 
  /// will be silently dropped (though it will log to the console in debug mode).
  ///
  /// * [name]: The identifier for the event (must be whitelisted).
  /// * [props]: A map of metadata associated with the event.
  ///
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
}// end class Telemetry
