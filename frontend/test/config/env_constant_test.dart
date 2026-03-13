// Tests for env_constant.dart configuration helpers
// (lib/config/env_constant.dart).
//
// These are pure functions that read from --dart-define constants.
// In the test environment no --dart-define values are set, so the functions
// either return their hard-coded defaults or throw expected exceptions.
// All tests run without any platform channels or network I/O.

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/config/env_constant.dart';

void main() {
  group('getBackendBaseUrl – default (no --dart-define)', () {
    test('returns a non-empty string', () {
      // When BACKEND_URL is not set the function must still return a URL.
      expect(getBackendBaseUrl(), isNotEmpty);
    });

    test('returns a string starting with http', () {
      // The fallback URL must use the http scheme.
      expect(getBackendBaseUrl(), startsWith('http'));
    });

    test('does not throw', () {
      // getBackendBaseUrl has no throw path for an empty value.
      expect(() => getBackendBaseUrl(), returnsNormally);
    });
  });

  group('getAppDomain – default', () {
    test('returns "localhost" when APP_DOMAIN is not set', () {
      // The const declares defaultValue: "localhost".
      expect(getAppDomain(), 'localhost');
    });
  });

  group('getAppPort – default', () {
    test('returns "50030" when APP_PORT is not set', () {
      // The const declares defaultValue: "50030".
      expect(getAppPort(), '50030');
    });
  });

  group('getOAuthRedirectUri', () {
    test('returns a non-empty string', () {
      // Composed from getAppDomain() + getAppPort().
      expect(getOAuthRedirectUri(), isNotEmpty);
    });

    test('contains "/oauth2/callback/google"', () {
      // The OAuth redirect path must always be present.
      expect(getOAuthRedirectUri(), contains('/oauth2/callback/google'));
    });

    test('uses http for localhost domain', () {
      // localhost should use plain http, not https.
      expect(getOAuthRedirectUri(), startsWith('http://localhost'));
    });
  });

  group('getWebBaseUrl', () {
    test('returns a non-empty string', () {
      // Composed from getAppDomain() + getAppPort().
      expect(getWebBaseUrl(), isNotEmpty);
    });

    test('uses http for localhost domain', () {
      // localhost should use plain http.
      expect(getWebBaseUrl(), startsWith('http://localhost'));
    });

    test('contains the app port', () {
      // The port 50030 must be embedded in the base URL for localhost.
      expect(getWebBaseUrl(), contains(getAppPort()));
    });
  });

  group('getWebSocketNotificationUrl', () {
    test('returns a non-empty string', () {
      // Must always produce a URL.
      expect(getWebSocketNotificationUrl(), isNotEmpty);
    });

    test('ends with /ws/notifications', () {
      // The notification WebSocket path is always /ws/notifications.
      expect(getWebSocketNotificationUrl(), endsWith('/ws/notifications'));
    });
  });

  group('getWebRTCSignalingServerUrl', () {
    test('returns a non-empty string', () {
      // Must always produce a URL.
      expect(getWebRTCSignalingServerUrl(), isNotEmpty);
    });

    test('ends with /ws/notifications', () {
      // The WebRTC signaling path currently maps to /ws/notifications.
      expect(getWebRTCSignalingServerUrl(), endsWith('/ws/notifications'));
    });
  });

  group('getAgoraAppCertificate', () {
    test('returns empty string when AGORA_APP_CERTIFICATE is not set', () {
      // Certificate is optional — no throw, just empty string.
      expect(getAgoraAppCertificate(), isEmpty);
    });
  });

  group('getEnableUSPSDigest', () {
    test('returns "false" by default', () {
      // Default is "false" when ENABLE_USPS_DIGEST is not set.
      expect(getEnableUSPSDigest(), 'false');
    });
  });

  group('getEnableMockUSPSDigest', () {
    test('returns "false" by default', () {
      // Default is "false" when ENABLE_MOCK_USPS_DIGEST is not set.
      expect(getEnableMockUSPSDigest(), 'false');
    });
  });

  group('getAgoraAppId – throws when not set', () {
    test('throws Exception when AGORA_APP_ID is not defined', () {
      // An empty AGORA_APP_ID must throw to signal misconfiguration.
      expect(() => getAgoraAppId(), throwsException);
    });
  });

  group('getFitbitClientId – throws when not set', () {
    test('throws Exception when FITBIT_CLIENT_ID is not defined', () {
      // An empty FITBIT_CLIENT_ID must throw to signal misconfiguration.
      expect(() => getFitbitClientId(), throwsException);
    });
  });

  group('getFitbitClientSecret – throws when not set', () {
    test('throws Exception when FITBIT_CLIENT_SECRET is not defined', () {
      // An empty FITBIT_CLIENT_SECRET must throw to signal misconfiguration.
      expect(() => getFitbitClientSecret(), throwsException);
    });
  });

  group('getDeepSeekUri – throws when not set', () {
    test('throws Exception when DEEPSEEK_URI is not defined', () {
      // An empty DEEPSEEK_URI must throw to signal misconfiguration.
      expect(() => getDeepSeekUri(), throwsException);
    });
  });

  group('getGoogleClientId – throws when not set', () {
    test('throws Exception when GOOGLE_CLIENT_ID is not defined', () {
      // An empty GOOGLE_CLIENT_ID must throw to signal misconfiguration.
      expect(() => getGoogleClientId(), throwsException);
    });
  });
}
