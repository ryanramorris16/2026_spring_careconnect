// Tests for EnvironmentConfig (lib/config/environment_config.dart).
//
// EnvironmentConfig.baseUrl returns a URL based on kIsWeb / platform.
// In the test environment (non-web, non-Android), it returns the _android
// default (http://10.0.2.2:8080) or the _other default depending on build vars.

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/config/environment_config.dart';

void main() {
  group('EnvironmentConfig', () {
    test('baseUrl returns a non-empty string', () {
      // The URL may vary by environment, but should always be non-empty.
      expect(EnvironmentConfig.baseUrl, isNotEmpty);
    });

    test('baseUrl is a valid http/https URL', () {
      final url = EnvironmentConfig.baseUrl;
      expect(url.startsWith('http://') || url.startsWith('https://'), isTrue,
          reason: 'baseUrl should start with http:// or https://');
    });

    test('baseUrl does not contain whitespace', () {
      expect(EnvironmentConfig.baseUrl.trim(), equals(EnvironmentConfig.baseUrl));
    });
  });
}
