import 'dart:convert';

import 'package:care_connect_app/features/telemetry/telemetry.dart';
import 'package:care_connect_app/features/telemetry/telemetry_settings.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Telemetry Orchestrator', () {
    setUp(() {
      SharedPreferences.setMockInitialValues({});
      Telemetry.resetForTest();
    });

    test('event() drops if user is opted out locally', () async {
      await TelemetrySettings.setOptedOut(true);

      var postCalled = false;
      var putCalled = false;

      Telemetry.client = MockClient((request) async {
        if (request.method == 'PUT' &&
            request.url.path.endsWith('/v1/api/dev/telemetry/enabled')) {
          putCalled = true;
          return http.Response('{"enabled":false}', 200);
        }

        if (request.method == 'POST' &&
            request.url.path.endsWith('/v1/api/dev/telemetry')) {
          postCalled = true;
          return http.Response('', 200);
        }

        return http.Response('{"enabled":true}', 200);
      });

      await Telemetry.event('button_tap', {'id': '123'});

      expect(postCalled, isFalse);
      expect(putCalled, isTrue);
    });

    test('getBackendEnabled() returns true on successful enabled response',
        () async {
      Telemetry.client = MockClient((request) async {
        expect(request.url.path, contains('/v1/api/dev/telemetry/enabled'));
        return http.Response('{"enabled":true}', 200);
      });

      final result = await Telemetry.getBackendEnabled();
      expect(result, isTrue);
    });

    test('getBackendEnabled() fails open when backend request fails', () async {
      Telemetry.client = MockClient((request) async {
        throw Exception('network failure');
      });

      final result = await Telemetry.getBackendEnabled();
      expect(result, isTrue);
    });

    test('setBackendEnabled() updates cache from successful backend response',
        () async {
      Telemetry.client = MockClient((request) async {
        expect(request.method, equals('PUT'));
        expect(request.url.toString(), contains('enabled=false'));
        return http.Response('{"enabled":false}', 200);
      });

      final result = await Telemetry.setBackendEnabled(false);
      expect(result, isFalse);
    });

    test('setBackendEnabled() falls back to requested value on backend failure',
        () async {
      Telemetry.client = MockClient((request) async {
        throw Exception('put failed');
      });

      final result = await Telemetry.setBackendEnabled(false);
      expect(result, isFalse);
    });

    test('isEnabled() returns false when user is opted out locally', () async {
      await TelemetrySettings.setOptedOut(true);

      var putCalled = false;
      Telemetry.client = MockClient((request) async {
        if (request.method == 'PUT') {
          putCalled = true;
          return http.Response('{"enabled":false}', 200);
        }
        return http.Response('{"enabled":true}', 200);
      });

      final result = await Telemetry.isEnabled();

      expect(result, isFalse);
      expect(putCalled, isTrue);
    });

    test(
        'isEnabled() returns backend value when local setting allows telemetry',
        () async {
      await TelemetrySettings.setOptedOut(false);

      Telemetry.client = MockClient((request) async {
        if (request.method == 'GET') {
          return http.Response('{"enabled":true}', 200);
        }
        return http.Response('{"enabled":true}', 200);
      });

      final result = await Telemetry.isEnabled();
      expect(result, isTrue);
    });

    test('event() sends correct payload when all gates pass', () async {
      await TelemetrySettings.setOptedOut(false);

      http.Request? capturedRequest;

      Telemetry.client = MockClient((request) async {
        capturedRequest = request;
        if (request.method == 'GET') {
          return http.Response('{"enabled":true}', 200);
        }
        if (request.method == 'POST') {
          return http.Response('', 200);
        }
        return http.Response('', 200);
      });

      await Telemetry.event('button_tap', {'element': 'login_btn'});

      expect(capturedRequest, isNotNull);
      expect(capturedRequest!.method, equals('POST'));
      expect(capturedRequest!.url.path, contains('/v1/api/dev/telemetry'));

      final body = jsonDecode(capturedRequest!.body) as Map<String, dynamic>;
      expect(body['eventName'], equals('button_tap'));
      expect(body['sessionId'], isNotNull);
      expect(body['traceId'], isNotNull);
      expect(body['spanId'], isNotNull);
      expect(body['details']['element'], equals('login_btn'));
      expect(body['deviceInfo'], isNotNull);
    });

    test('sanitize() rejection stops the flow', () async {
      await TelemetrySettings.setOptedOut(false);

      var postCalled = false;

      Telemetry.client = MockClient((request) async {
        if (request.method == 'GET') {
          return http.Response('{"enabled":true}', 200);
        }
        if (request.method == 'POST') {
          postCalled = true;
          return http.Response('', 200);
        }
        return http.Response('', 200);
      });

      await Telemetry.event('illegal_event', {'data': 'bad'});

      expect(postCalled, isFalse);
    });

    test('backend enabled value is cached across calls', () async {
      await TelemetrySettings.setOptedOut(false);

      var getCount = 0;

      Telemetry.client = MockClient((request) async {
        if (request.method == 'GET') {
          getCount++;
          return http.Response('{"enabled":true}', 200);
        }
        return http.Response('', 200);
      });

      final first = await Telemetry.isEnabled();
      final second = await Telemetry.isEnabled();

      expect(first, isTrue);
      expect(second, isTrue);
      expect(getCount, equals(1));
    });
  });
}
