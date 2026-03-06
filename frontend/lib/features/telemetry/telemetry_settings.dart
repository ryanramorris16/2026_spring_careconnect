import 'package:shared_preferences/shared_preferences.dart';


/// A persistent storage utility for managing user telemetry preferences.
///
/// [TelemetrySettings] handles the local storage of privacy-related flags
/// using [SharedPreferences]. This ensures that once a user opts out or 
/// acknowledges the privacy dialog, the state is preserved across app sessions.
///
/// These settings are the primary "Master Switch" used by the [Telemetry] 
/// class to determine if data collection should be permitted.
class TelemetrySettings {

  /// flag indicating if the user has disabled telemetry.
  static const _optOutKey = 'telemetry_opted_out';

  /// flag indicating if the user has been presented with
  /// the initial telemetry disclosure/opt-out dialog.
  static const _seenDialogKey = 'telemetry_seen_optout_dialog';

  /// Default: telemetry is ON, which means optedOut == false if unset.
  /// Returns whether the user has explicitly opted out of telemetry collection.
  /// 
  /// Defaults to `false` (meaning telemetry is **ON**) if no value has 
  /// been previously saved.
  static Future<bool> isOptedOut() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_optOutKey) ?? false;
  }

  /// Persists the user's telemetry preference.
  /// 
  /// Set [optedOut] to `true` to disable all telemetry collection immediately.
  static Future<void> setOptedOut(bool optedOut) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_optOutKey, optedOut);
  }

  /// Checks if the user has already viewed the telemetry consent/info dialog.
  /// 
  /// Use this to determine whether to trigger the initial setup UI. 
  /// Defaults to `false` if the user is new.
  static Future<bool> hasSeenDialog() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_seenDialogKey) ?? false;
  }

  /// Marks the telemetry disclosure dialog as having been displayed.
  /// 
  /// This should be called once the user dismisses the initial privacy 
  /// onboarding to prevent redundant popups.
  static Future<void> setHasSeenDialog(bool seen) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_seenDialogKey, seen);
  }
}// end class TelemetrySettings
