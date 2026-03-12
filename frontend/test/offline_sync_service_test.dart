import 'dart:async';
import 'dart:convert';

import 'package:care_connect_app/services/local_db/offline_sync_service.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;

import 'test_support/local_db_test_bindings.dart';

void main() {
  group('offline_sync_service.dart', () {
    late OfflineSyncService service;

    setUpAll(() async {
      await LocalDbTestBindings.install();
      service = OfflineSyncService.instance();
      await service.initialize();
    });

    setUp(() async {
      await _clearQueue(service);
    });

    tearDownAll(LocalDbTestBindings.uninstall);

    test('identifies queueable HTTP methods', () {
      expect(service.isQueueableMethod('POST'), isTrue);
      expect(service.isQueueableMethod('put'), isTrue);
      expect(service.isQueueableMethod('PATCH'), isTrue);
      expect(service.isQueueableMethod('DELETE'), isTrue);
      expect(service.isQueueableMethod('GET'), isFalse);
    });

    test('detects queue-worthy network errors', () {
      expect(service.shouldQueueForError(TimeoutException('timeout')), isTrue);
      expect(
        service.shouldQueueForError(
          http.ClientException('connection refused'),
        ),
        isTrue,
      );
      expect(service.shouldQueueForError(Exception('bad request')), isFalse);
    });

    test('buildQueuedStreamedResponse returns expected queued payload', () async {
      final request = http.Request(
        'POST',
        Uri.parse('https://example.org/v1/api/tasks'),
      );

      final streamed = service.buildQueuedStreamedResponse(
        request,
        queuedId: 'queued-123',
      );
      final response = await http.Response.fromStream(streamed);
      final decoded = jsonDecode(response.body) as Map<String, dynamic>;

      expect(response.statusCode, equals(200));
      expect(response.headers['x-offline-queued'], equals('true'));
      expect(response.headers['x-offline-request-id'], equals('queued-123'));
      expect(decoded['queued'], isTrue);
      expect(decoded['requestId'], equals('queued-123'));
    });

    test('enqueueRequest deduplicates equivalent payloads', () async {
      final first = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('https://example.org/v1/api/patient/1/mood'),
        headers: <String, String>{'Content-Type': 'application/json'},
        body: '{"score":8,"label":"Good"}',
      );

      final second = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('https://example.org/v1/api/patient/1/mood'),
        headers: <String, String>{'Content-Type': 'application/json'},
        body: '{"score":8,"label":"Good"}',
      );

      expect(first, equals(second));
      expect(await service.getPendingCount(), equals(1));
    });

    test('pending queue display is human-readable and does not expose endpoint info', () async {
      final moodId = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('https://example.org/v1/api/patient/1/mood'),
        headers: <String, String>{'Content-Type': 'application/json'},
        body: '{"score":8,"label":"Good"}',
      );
      final taskId = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('https://example.org/v1/api/tasks/patient/1'),
        headers: <String, String>{'Content-Type': 'application/json'},
        body:
            '{"title":"Blood Pressure Check","note":"Use home cuff","taskDate":"2026-03-12","time":"09:30"}',
      );
      final genericId = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('https://example.org/v1/api/custom'),
        headers: <String, String>{'Content-Type': 'application/json'},
        body: '{"patientName":"Mary Johnson","token":"secret-value"}',
      );

      final queue = await service.getPendingQueue(limit: 20);
      final mood = queue.firstWhere((item) => item.id == moodId);
      final task = queue.firstWhere((item) => item.id == taskId);
      final generic = queue.firstWhere((item) => item.id == genericId);

      expect(mood.displayTitle, equals('Mood Entry'));
      expect(mood.displayDetails.join(' '), contains('Mood rating'));

      expect(task.displayTitle, equals('Task'));
      expect(task.displayDetails.join(' '), contains('Title: Blood Pressure Check'));

      final genericDetails = generic.displayDetails.join(' ');
      expect(generic.displayTitle, equals('Queued Update'));
      expect(genericDetails, contains('Patient Name: Mary Johnson'));
      expect(genericDetails.toLowerCase(), isNot(contains('endpoint')));
      expect(genericDetails.toLowerCase(), isNot(contains('secret-value')));
      expect(genericDetails.toLowerCase(), isNot(contains('token')));
    });

    test('syncPendingQueue reports zero attempts when queue is empty', () async {
      final summary = await service.syncPendingQueue(limit: 50);
      expect(summary.attempted, equals(0));
      expect(summary.succeeded, equals(0));
      expect(summary.failed, equals(0));
    });

    test('deleteQueuedRequestById returns false for unknown id', () async {
      expect(await service.deleteQueuedRequestById('does-not-exist'), isFalse);
    });

    test('OfflineQueueHttpClient queues offline write requests', () async {
      final client = OfflineQueueHttpClient(
        inner: _ThrowingClient(TimeoutException('offline')),
        offlineSyncService: service,
        canQueueWrites: () => true,
      );

      final request = http.Request(
        'POST',
        Uri.parse('https://example.org/v1/api/tasks/patient/1'),
      )..body = '{"title":"Queued"}';

      final streamed = await client.send(request);
      final response = await http.Response.fromStream(streamed);

      expect(response.statusCode, equals(200));
      expect(response.headers['x-offline-queued'], equals('true'));
      expect(await service.getPendingCount(), equals(1));
    });

    test('OfflineQueueHttpClient does not queue GET requests', () async {
      final client = OfflineQueueHttpClient(
        inner: _ThrowingClient(TimeoutException('offline')),
        offlineSyncService: service,
        canQueueWrites: () => true,
      );

      final request = http.Request(
        'GET',
        Uri.parse('https://example.org/v1/api/tasks/patient/1'),
      );

      expect(
        () => client.send(request),
        throwsA(isA<TimeoutException>()),
      );
      expect(await service.getPendingCount(), equals(0));
    });

    test('OfflineQueueHttpClient does not queue auth endpoint writes', () async {
      final client = OfflineQueueHttpClient(
        inner: _ThrowingClient(TimeoutException('offline')),
        offlineSyncService: service,
        canQueueWrites: () => true,
      );

      final request = http.Request(
        'POST',
        Uri.parse('https://example.org/v1/api/auth/login'),
      )..body = '{"email":"patient@careconnect.com"}';

      expect(
        () => client.send(request),
        throwsA(isA<TimeoutException>()),
      );
      expect(await service.getPendingCount(), equals(0));
    });

    test('OfflineQueueHttpClient respects canQueueWrites=false', () async {
      final client = OfflineQueueHttpClient(
        inner: _ThrowingClient(TimeoutException('offline')),
        offlineSyncService: service,
        canQueueWrites: () => false,
      );

      final request = http.Request(
        'POST',
        Uri.parse('https://example.org/v1/api/tasks/patient/1'),
      )..body = '{"title":"Should fail"}';

      expect(
        () => client.send(request),
        throwsA(isA<TimeoutException>()),
      );
      expect(await service.getPendingCount(), equals(0));
    });

    test('OfflineQueueHttpClient passes through successful responses', () async {
      final client = OfflineQueueHttpClient(
        inner: _StaticClient(statusCode: 201, body: '{"created":true}'),
        offlineSyncService: service,
        canQueueWrites: () => true,
      );

      final request = http.Request(
        'POST',
        Uri.parse('https://example.org/v1/api/tasks/patient/1'),
      )..body = '{"title":"New"}';

      final streamed = await client.send(request);
      final response = await http.Response.fromStream(streamed);

      expect(response.statusCode, equals(201));
      expect(response.headers.containsKey('x-offline-queued'), isFalse);
      expect(await service.getPendingCount(), equals(0));
    });
  });
}

Future<void> _clearQueue(OfflineSyncService service) async {
  final pending = await service.getPendingQueue(limit: 2000);
  for (final item in pending) {
    await service.deleteQueuedRequestById(item.id);
  }
}

class _ThrowingClient extends http.BaseClient {
  _ThrowingClient(this.error);

  final Object error;

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    throw error;
  }
}

class _StaticClient extends http.BaseClient {
  _StaticClient({
    required this.statusCode,
    required this.body,
  });

  final int statusCode;
  final String body;

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    return http.StreamedResponse(
      Stream<List<int>>.value(utf8.encode(body)),
      statusCode,
      request: request,
      headers: <String, String>{'content-type': 'application/json'},
    );
  }
}
