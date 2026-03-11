// Tests for NotetakerConfigService data models and HTTP service methods.
//
// Coverage strategy:
//   Model classes tested directly (no platform channels needed).
//   HTTP service methods tested via http.runWithClient + MockClient.
//   AuthTokenManager.getAuthHeaders() intercepted via FlutterSecureStorage stub
//   (jwt_token + token_expiry seeded so a Bearer token is returned).
//
//   Branches tested:
//     PatientNotetakerKeyword.toJson — produces correct map.
//     PatientNotetakerConfigDTO.fromJson — parses all fields including triggerKeywords list.
//     PatientNotetakerConfigDTO.fromJson — missing optional fields use defaults.
//     PatientNotetakerConfigDTO.toJson — serialises fields, omits null id.
//     PatientNotetakerConfigDTO.toJson — includes id when present.
//     PatientNotetakerConfigDTO.copyWith — overrides specified fields, keeps rest.
//     saveUserNotetakerConfig — 200 → DTO, non-200 → null.
//     getUserNotetakerConfig — 200 → DTO, 404 → default config, non-200 → null.
//     getPatientNotes — 200 list → list, 404 → [], non-200 → [].
//     createPatientNote — 200 → note, non-200 → throws.
//     updatePatientNote — 200 → note, non-200 → throws.
//     deletePatientNote — 200 → completes.

import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:care_connect_app/features/notetaker/models/patient_note_model.dart';
import 'package:care_connect_app/services/notetaker_config_service.dart';

// ─── Secure storage stub ──────────────────────────────────────────────────────

const MethodChannel _secureStorageChannel =
    MethodChannel('plugins.it_nomads.com/flutter_secure_storage');

final Map<String, String?> _secureStore = {};

void _setupSecureStorageStub() {
  TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockMethodCallHandler(_secureStorageChannel, (call) async {
    switch (call.method) {
      case 'write':
        _secureStore[call.arguments['key'] as String] =
            call.arguments['value'] as String?;
        return null;
      case 'read':
        return _secureStore[call.arguments['key'] as String];
      case 'delete':
        _secureStore.remove(call.arguments['key'] as String);
        return null;
      case 'deleteAll':
        _secureStore.clear();
        return null;
      default:
        return null;
    }
  });
}

/// Seeds a valid JWT token into secure storage so AuthTokenManager returns
/// an Authorization header without hitting the backend for validation.
void _seedAuthToken() {
  _secureStore['jwt_token'] = 'test-jwt-token';
  // Unix epoch far in the future (year 2033) so the token is always valid.
  _secureStore['token_expiry'] = '2000000000';
}

/// Minimal valid config JSON as returned by the backend.
Map<String, dynamic> _configJson({int patientId = 1}) => {
  'id': 10,
  'patientId': patientId,
  'isEnabled': true,
  'permitCaregiverAccess': false,
  'triggerKeywords': [
    {'keyword': 'PII_SSN', 'event_type': 'ALERT'},
  ],
};

/// Minimal valid note JSON as returned by the backend.
Map<String, dynamic> _noteJson({String id = 'n1', int patientId = 1}) => {
  'id': id,
  'patientId': patientId.toString(),
  'note': 'Some note text',
  'aiSummary': 'Summary',
  'createdAt': '2025-01-01T00:00:00.000Z',
  'updatedAt': '2025-01-01T00:00:00.000Z',
};

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    _secureStore.clear();
    SharedPreferences.setMockInitialValues({});
    _setupSecureStorageStub();
    _seedAuthToken();
  });

  tearDownAll(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_secureStorageChannel, null);
  });

  // ─── PatientNotetakerKeyword ──────────────────────────────────────────────

  group('PatientNotetakerKeyword.toJson', () {
    test('serialises keyword and event_type', () {
      final kw = PatientNotetakerKeyword(
        keyword: 'PII_SSN',
        event_type: 'ALERT',
      );
      final json = kw.toJson();
      expect(json['keyword'], 'PII_SSN');
      expect(json['event_type'], 'ALERT');
    });
  });

  // ─── PatientNotetakerConfigDTO.fromJson ───────────────────────────────────

  group('PatientNotetakerConfigDTO.fromJson', () {
    test('parses all fields from JSON', () {
      final json = {
        'id': 42,
        'patientId': 7,
        'isEnabled': true,
        'permitCaregiverAccess': false,
        'triggerKeywords': [
          {'keyword': 'PII_SSN', 'event_type': 'ALERT'},
          {'keyword': 'PII_CC', 'event_type': 'LOG'},
        ],
        'updatedAt': '2025-01-15T10:30:00.000Z',
      };
      final dto = PatientNotetakerConfigDTO.fromJson(json);
      expect(dto.id, 42);
      expect(dto.patientId, 7);
      expect(dto.isEnabled, isTrue);
      expect(dto.permitCaregiverAccess, isFalse);
      expect(dto.triggerKeywords.length, 2);
      expect(dto.triggerKeywords[0].keyword, 'PII_SSN');
      expect(dto.triggerKeywords[1].event_type, 'LOG');
      expect(dto.updatedAt, isNotNull);
    });

    test('missing optional fields (id, triggerKeywords, updatedAt) use defaults', () {
      // isEnabled must be a valid bool; passing null hits a source-level bug
      // (the ?? 'DEFAULT' fallback is a String but the field is typed bool).
      // We pass true here and verify that the truly-optional fields default correctly.
      final json = {
        'patientId': 5,
        'isEnabled': true,
        'permitCaregiverAccess': true,
      };
      final dto = PatientNotetakerConfigDTO.fromJson(json);
      expect(dto.id, isNull);
      expect(dto.triggerKeywords, isEmpty);
      expect(dto.updatedAt, isNull);
    });

    test('empty triggerKeywords list produces empty list', () {
      final json = {
        'patientId': 3,
        'isEnabled': false,
        'permitCaregiverAccess': false,
        'triggerKeywords': <dynamic>[],
      };
      final dto = PatientNotetakerConfigDTO.fromJson(json);
      expect(dto.triggerKeywords, isEmpty);
    });
  });

  // ─── PatientNotetakerConfigDTO.toJson ─────────────────────────────────────

  group('PatientNotetakerConfigDTO.toJson', () {
    test('omits id when null', () {
      final dto = PatientNotetakerConfigDTO(
        patientId: 5,
        isEnabled: true,
        permitCaregiverAccess: false,
        triggerKeywords: [],
      );
      final json = dto.toJson();
      expect(json.containsKey('id'), isFalse);
      expect(json['patientId'], 5);
      expect(json['isEnabled'], isTrue);
      expect(json['triggerKeywords'], isEmpty);
    });

    test('includes id when present', () {
      final dto = PatientNotetakerConfigDTO(
        id: 99,
        patientId: 5,
        isEnabled: false,
        permitCaregiverAccess: true,
        triggerKeywords: [
          PatientNotetakerKeyword(keyword: 'KEY', event_type: 'ALERT'),
        ],
      );
      final json = dto.toJson();
      expect(json['id'], 99);
      expect(json['permitCaregiverAccess'], isTrue);
      expect((json['triggerKeywords'] as List).length, 1);
      expect((json['triggerKeywords'] as List)[0]['keyword'], 'KEY');
    });
  });

  // ─── PatientNotetakerConfigDTO.copyWith ───────────────────────────────────

  group('PatientNotetakerConfigDTO.copyWith', () {
    test('overrides only specified fields', () {
      final original = PatientNotetakerConfigDTO(
        id: 1,
        patientId: 10,
        isEnabled: true,
        permitCaregiverAccess: false,
        triggerKeywords: [],
      );
      final copy = original.copyWith(isEnabled: false, permitCaregiverAccess: true);
      expect(copy.id, 1);
      expect(copy.patientId, 10);
      expect(copy.isEnabled, isFalse);
      expect(copy.permitCaregiverAccess, isTrue);
    });

    test('returns independent copy with new keyword list', () {
      final original = PatientNotetakerConfigDTO(
        patientId: 3,
        isEnabled: true,
        permitCaregiverAccess: false,
        triggerKeywords: [],
      );
      final newKeywords = [
        PatientNotetakerKeyword(keyword: 'NEW', event_type: 'LOG'),
      ];
      final copy = original.copyWith(triggerKeywords: newKeywords);
      expect(copy.triggerKeywords.length, 1);
      expect(original.triggerKeywords, isEmpty);
    });

    test('no arguments → returns identical values', () {
      final original = PatientNotetakerConfigDTO(
        id: 7,
        patientId: 2,
        isEnabled: false,
        permitCaregiverAccess: true,
        triggerKeywords: [],
      );
      final copy = original.copyWith();
      expect(copy.id, original.id);
      expect(copy.patientId, original.patientId);
      expect(copy.isEnabled, original.isEnabled);
      expect(copy.permitCaregiverAccess, original.permitCaregiverAccess);
    });
  });

  // ─── NotetakerConfigService.saveUserNotetakerConfig ───────────────────────

  group('NotetakerConfigService.saveUserNotetakerConfig', () {
    test('200 → returns DTO', () async {
      final config = PatientNotetakerConfigDTO(
        patientId: 1,
        isEnabled: true,
        permitCaregiverAccess: false,
        triggerKeywords: [],
      );
      final result = await http.runWithClient(
        () => NotetakerConfigService.saveUserNotetakerConfig(
          config,
          userId: 99,
        ),
        () => MockClient(
          (_) async => http.Response(jsonEncode(_configJson()), 200),
        ),
      );
      expect(result, isNotNull);
      expect(result?.patientId, 1);
      expect(result?.isEnabled, isTrue);
    });

    test('201 → returns DTO', () async {
      final config = PatientNotetakerConfigDTO(
        patientId: 2,
        isEnabled: false,
        permitCaregiverAccess: true,
        triggerKeywords: [],
      );
      final result = await http.runWithClient(
        () => NotetakerConfigService.saveUserNotetakerConfig(
          config,
          userId: 5,
        ),
        () => MockClient(
          (_) async =>
              http.Response(jsonEncode(_configJson(patientId: 2)), 201),
        ),
      );
      expect(result, isNotNull);
    });

    test('non-200 → returns null', () async {
      final config = PatientNotetakerConfigDTO(
        patientId: 3,
        isEnabled: true,
        permitCaregiverAccess: false,
        triggerKeywords: [],
      );
      final result = await http.runWithClient(
        () => NotetakerConfigService.saveUserNotetakerConfig(
          config,
          userId: 1,
        ),
        () => MockClient((_) async => http.Response('error', 400)),
      );
      expect(result, isNull);
    });
  });

  // ─── NotetakerConfigService.getUserNotetakerConfig ────────────────────────

  group('NotetakerConfigService.getUserNotetakerConfig', () {
    test('200 → returns DTO from response', () async {
      final result = await http.runWithClient(
        () => NotetakerConfigService.getUserNotetakerConfig(1, null),
        () => MockClient(
          (_) async => http.Response(jsonEncode(_configJson()), 200),
        ),
      );
      expect(result, isNotNull);
      expect(result?.isEnabled, isTrue);
    });

    test('404 → returns default config with correct patientId', () async {
      final result = await http.runWithClient(
        () => NotetakerConfigService.getUserNotetakerConfig(7, null),
        () => MockClient((_) async => http.Response('', 404)),
      );
      expect(result, isNotNull);
      expect(result?.patientId, 7);
      expect(result?.isEnabled, isTrue);
      expect(result?.triggerKeywords, isNotEmpty);
    });

    test('non-200 non-404 → returns null', () async {
      final result = await http.runWithClient(
        () => NotetakerConfigService.getUserNotetakerConfig(1, null),
        () => MockClient((_) async => http.Response('error', 500)),
      );
      expect(result, isNull);
    });
  });

  // ─── NotetakerConfigService.getPatientNotes ───────────────────────────────

  group('NotetakerConfigService.getPatientNotes', () {
    test('200 with JSON list → returns list of notes', () async {
      final notes = [_noteJson(), _noteJson(id: 'n2')];
      final result = await http.runWithClient(
        () => NotetakerConfigService.getPatientNotes(1),
        () => MockClient(
          (_) async => http.Response(jsonEncode(notes), 200),
        ),
      );
      expect(result.length, 2);
      expect(result[0].note, 'Some note text');
    });

    test('200 with data key → returns list', () async {
      final body = {
        'data': [_noteJson()],
      };
      final result = await http.runWithClient(
        () => NotetakerConfigService.getPatientNotes(1),
        () => MockClient(
          (_) async => http.Response(jsonEncode(body), 200),
        ),
      );
      expect(result.length, 1);
    });

    test('200 with notes key → returns list', () async {
      final body = {
        'notes': [_noteJson()],
      };
      final result = await http.runWithClient(
        () => NotetakerConfigService.getPatientNotes(1),
        () => MockClient(
          (_) async => http.Response(jsonEncode(body), 200),
        ),
      );
      expect(result.length, 1);
    });

    test('404 → returns empty list', () async {
      final result = await http.runWithClient(
        () => NotetakerConfigService.getPatientNotes(99),
        () => MockClient((_) async => http.Response('', 404)),
      );
      expect(result, isEmpty);
    });

    test('non-200 → returns empty list', () async {
      final result = await http.runWithClient(
        () => NotetakerConfigService.getPatientNotes(1),
        () => MockClient((_) async => http.Response('error', 500)),
      );
      expect(result, isEmpty);
    });
  });

  // ─── NotetakerConfigService.createPatientNote ─────────────────────────────

  group('NotetakerConfigService.createPatientNote', () {
    test('200 → returns created note', () async {
      final noteData = _noteJson();
      final note = _buildNote(patientId: 1);
      final result = await http.runWithClient(
        () => NotetakerConfigService.createPatientNote(note),
        () => MockClient(
          (_) async => http.Response(jsonEncode(noteData), 200),
        ),
      );
      expect(result.note, 'Some note text');
    });

    test('201 → returns created note', () async {
      final note = _buildNote(patientId: 2);
      final result = await http.runWithClient(
        () => NotetakerConfigService.createPatientNote(note),
        () => MockClient(
          (_) async => http.Response(jsonEncode(_noteJson(patientId: 2)), 201),
        ),
      );
      expect(result.patientId, '2');
    });

    test('non-200 → throws', () async {
      final note = _buildNote(patientId: 1);
      expect(
        () => http.runWithClient(
          () => NotetakerConfigService.createPatientNote(note),
          () => MockClient((_) async => http.Response('error', 400)),
        ),
        throwsA(isA<Exception>()),
      );
    });
  });

  // ─── NotetakerConfigService.updatePatientNote ─────────────────────────────

  group('NotetakerConfigService.updatePatientNote', () {
    test('200 → returns updated note', () async {
      final note = _buildNote(id: 'existing-id', patientId: 1);
      final result = await http.runWithClient(
        () => NotetakerConfigService.updatePatientNote(note),
        () => MockClient(
          (_) async =>
              http.Response(jsonEncode(_noteJson(id: 'existing-id')), 200),
        ),
      );
      expect(result.id, 'existing-id');
    });

    test('non-200 → throws', () async {
      final note = _buildNote(id: 'nid', patientId: 1);
      expect(
        () => http.runWithClient(
          () => NotetakerConfigService.updatePatientNote(note),
          () => MockClient((_) async => http.Response('error', 500)),
        ),
        throwsA(isA<Exception>()),
      );
    });
  });

  // ─── NotetakerConfigService.deletePatientNote ─────────────────────────────

  group('NotetakerConfigService.deletePatientNote', () {
    test('200 → completes without error', () async {
      await http.runWithClient(
        () => NotetakerConfigService.deletePatientNote('n1', 1),
        () => MockClient((_) async => http.Response('', 200)),
      );
      // No assertion needed — just must not throw.
    });

    test('204 → completes without error', () async {
      await http.runWithClient(
        () => NotetakerConfigService.deletePatientNote('n2', 1),
        () => MockClient((_) async => http.Response('', 204)),
      );
    });
  });
}

// ─── Helper ───────────────────────────────────────────────────────────────────

PatientNote _buildNote({String id = 'test-id', int patientId = 1}) {
  return PatientNote(
    id: id,
    patientId: patientId.toString(),
    note: 'Test note',
    aiSummary: 'Summary',
    createdAt: DateTime(2025),
    updatedAt: DateTime(2025),
  );
}
