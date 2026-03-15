import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:sqlite3/sqlite3.dart' as sqlite;
import 'package:care_connect_app/services/local_db/app_database.dart';
import 'package:care_connect_app/services/local_db/offline_sync_service.dart';

import '../test_support/local_db_test_bindings.dart';

class FakeInnerClient extends http.BaseClient {
  FakeInnerClient(this._handler);

  final Future<http.StreamedResponse> Function(http.BaseRequest request)
      _handler;
  bool closed = false;

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) {
    return _handler(request);
  }

  @override
  void close() {
    closed = true;
  }
}

Future<void> _clearQueue(AppDatabase db) async {
  final rows = await db.getPendingOfflineSyncQueue(limit: 5000);
  for (final row in rows) {
    await db.deleteOfflineSyncById(row.id);
  }

  final raw = sqlite.sqlite3.open(LocalDbTestBindings.dbPath);
  raw.execute('''
    CREATE TABLE IF NOT EXISTS offline_sync (
      id TEXT PRIMARY KEY,
      method TEXT NOT NULL,
      url TEXT NOT NULL,
      headers_json TEXT NOT NULL,
      body_json TEXT,
      created_at TEXT NOT NULL,
      fingerprint TEXT NOT NULL UNIQUE,
      status TEXT NOT NULL DEFAULT 'pending',
      retry_count INTEGER NOT NULL DEFAULT 0,
      last_error TEXT
    )
  ''');
  raw.execute('DELETE FROM offline_sync');
  raw.dispose();
}

Future<void> _insertRawRow({
  required String id,
  required String method,
  required String url,
  required String headersJson,
  String? bodyJson,
  required String createdAt,
  required String fingerprint,
  required String status,
  dynamic retryCount = 0,
  String? lastError,
}) async {
  final raw = sqlite.sqlite3.open(LocalDbTestBindings.dbPath);
  raw.execute('''
    CREATE TABLE IF NOT EXISTS offline_sync (
      id TEXT PRIMARY KEY,
      method TEXT NOT NULL,
      url TEXT NOT NULL,
      headers_json TEXT NOT NULL,
      body_json TEXT,
      created_at TEXT NOT NULL,
      fingerprint TEXT NOT NULL UNIQUE,
      status TEXT NOT NULL DEFAULT 'pending',
      retry_count INTEGER NOT NULL DEFAULT 0,
      last_error TEXT
    )
  ''');
  raw.execute(
    '''
    INSERT INTO offline_sync (
      id, method, url, headers_json, body_json, created_at, fingerprint, status, retry_count, last_error
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ''',
    <Object?>[
      id,
      method,
      url,
      headersJson,
      bodyJson,
      createdAt,
      fingerprint,
      status,
      retryCount,
      lastError,
    ],
  );
  raw.dispose();
}

void main() {
  group('OfflineSyncService + OfflineQueueHttpClient', () {
    late OfflineSyncService service;
    late AppDatabase db;

    setUpAll(() async {
      await LocalDbTestBindings.install();
      service = OfflineSyncService.instance();
      db = AppDatabase();
      await service.initialize();
    });

    tearDownAll(() async {
      await service.close();
      await db.closeDb();
      await LocalDbTestBindings.uninstall();
    });

    setUp(() async {
      await _clearQueue(db);
    });

    test(
        'queueable methods, pending count, enqueue dedupe, delete, and queued response work',
        () async {
      expect(service.isQueueableMethod('post'), isTrue);
      expect(service.isQueueableMethod('PUT'), isTrue);
      expect(service.isQueueableMethod('patch'), isTrue);
      expect(service.isQueueableMethod('delete'), isTrue);
      expect(service.isQueueableMethod('get'), isFalse);

      final firstId = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('https://example.com/v1/api/tasks'),
        headers: <String, String>{
          'Authorization': 'Bearer one',
          'Cookie': 'x=1',
          OfflineSyncService.replayHeader: 'true',
          'Content-Type': 'application/json',
        },
        body: '{"title":"Task A"}',
      );

      final duplicate = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('https://example.com/v1/api/tasks'),
        headers: <String, String>{
          'Authorization': 'Bearer two',
          'Cookie': 'x=2',
          'Content-Type': 'application/json',
        },
        body: '{"title":"Task A"}',
      );

      expect(duplicate, equals(firstId));
      expect(await service.getPendingCount(), 1);

      final queuedRows = await db.getPendingOfflineSyncQueue(limit: 10);
      expect(queuedRows.single.bodyJson, '{"title":"Task A"}');
      expect(
          queuedRows.single.headersJson
              .contains(OfflineSyncService.replayHeader),
          isFalse);

      final request = http.Request('POST', Uri.parse('https://example.com/x'));
      final streamed =
          service.buildQueuedStreamedResponse(request, queuedId: 'queued-123');
      final response = await http.Response.fromStream(streamed);

      expect(response.statusCode, 200);
      expect(response.headers['x-offline-queued'], 'true');
      expect(response.headers['x-offline-request-id'], 'queued-123');
      expect(jsonDecode(response.body)['queued'], isTrue);

      expect(await service.deleteQueuedRequestById('missing'), isFalse);
      expect(await service.deleteQueuedRequestById(firstId), isTrue);
      expect(await service.getPendingCount(), 0);
    });

    test(
        'shouldQueueForError covers timeout, client, socket-like, and non-network errors',
        () {
      expect(service.shouldQueueForError(TimeoutException('timeout')), isTrue);
      expect(service.shouldQueueForError(http.ClientException('bad network')),
          isTrue);
      expect(service.shouldQueueForError(Exception('SocketException: boom')),
          isTrue);
      expect(
          service.shouldQueueForError(Exception('failed host lookup')), isTrue);
      expect(service.shouldQueueForError(Exception('network is unreachable')),
          isTrue);
      expect(
          service.shouldQueueForError(Exception('connection refused')), isTrue);
      expect(
          service.shouldQueueForError(Exception('connection reset')), isTrue);
      expect(service.shouldQueueForError(Exception('timed out')), isTrue);
      expect(
          service.shouldQueueForError(Exception('some other error')), isFalse);
    });

    test(
        'getPendingQueue builds display models for mood, mood check-in, tasks, generic payload, and waiting state',
        () async {
      await _insertRawRow(
        id: 'mood-checkin-valid',
        method: 'POST',
        url: 'https://example.com/v1/api/mood-pain-log',
        headersJson: '{}',
        bodyJson: jsonEncode({
          'moodValue': 8,
          'timestamp': '2026-01-02T03:04:05.000Z',
        }),
        createdAt: '2026-01-02T03:04:05.000Z',
        fingerprint: 'fp1',
        status: 'pending',
      );

      await _insertRawRow(
        id: 'mood-checkin-invalid-time',
        method: 'POST',
        url: 'https://example.com/v1/api/mood-pain-log',
        headersJson: '{}',
        bodyJson: jsonEncode({
          'moodValue': 2,
          'timestamp': 'bad-time',
        }),
        createdAt: '2026-01-02T03:04:05.000Z',
        fingerprint: 'fp2',
        status: 'pending',
      );

      await _insertRawRow(
        id: 'mood-entry',
        method: 'POST',
        url: 'https://example.com/v1/api/mood',
        headersJson: '{}',
        bodyJson: jsonEncode({
          'score': 6,
          'label': 'Good',
        }),
        createdAt: '2026-01-03T03:04:05.000Z',
        fingerprint: 'fp3',
        status: 'failed',
      );

      await _insertRawRow(
        id: 'task-nested-map',
        method: 'POST',
        url: 'https://example.com/v1/api/tasks',
        headersJson: '{}',
        bodyJson: jsonEncode({
          'task': {
            'title': 'Refill Rx',
            'note': 'Take after food',
            'taskDate': '2026-01-10',
            'time': '08:00',
          }
        }),
        createdAt: '2026-01-04T03:04:05.000Z',
        fingerprint: 'fp4',
        status: 'syncing',
      );

      await _insertRawRow(
        id: 'task-nested-json-string',
        method: 'POST',
        url: 'https://example.com/v1/api/tasks',
        headersJson: '{}',
        bodyJson: jsonEncode({
          'task': jsonEncode({
            'taskTitle': 'Walk',
            'description': '15 minutes',
            'dueDate': '2026-01-11',
            'dueTime': '09:30',
          }),
        }),
        createdAt: '2026-01-05T03:04:05.000Z',
        fingerprint: 'fp5',
        status: 'pending',
      );

      await _insertRawRow(
        id: 'generic',
        method: 'POST',
        url: 'https://example.com/v1/api/something',
        headersJson: '{}',
        bodyJson: jsonEncode({
          '': 'fallback field',
          'camelCaseField': true,
          'long_text': 'x' * 100,
          'count': 3,
          'items': [1, 2, 3],
          'meta': {'a': 1},
          'password': 'secret',
          'emptyList': [],
          'emptyMap': {},
          'blank': '   ',
          'nullValue': null,
        }),
        createdAt: '2026-01-06T03:04:05.000Z',
        fingerprint: 'fp6',
        status: 'pending',
      );

      await _insertRawRow(
        id: 'waiting',
        method: 'POST',
        url: 'https://example.com/v1/api/other',
        headersJson: '[]',
        bodyJson: 'not-json',
        createdAt: '2026-01-07T03:04:05.000Z',
        fingerprint: 'fp7',
        status: 'pending',
      );

      final items = await service.getPendingQueue(limit: 20);

      final byId = <String, OfflineSyncQueueItem>{
        for (final item in items) item.id: item,
      };

      expect(byId['mood-checkin-valid']!.displayTitle, 'Mood Check-In');
      expect(byId['mood-checkin-valid']!.displayDetails.first,
          contains('Mood rating: 8'));

      expect(byId['mood-checkin-invalid-time']!.displayDetails.last,
          contains('Date: bad-time'));

      expect(byId['mood-entry']!.displayTitle, 'Mood Entry');
      expect(byId['mood-entry']!.displayDetails.first,
          contains('Mood rating: 6 (Good)'));

      expect(byId['task-nested-map']!.displayTitle, 'Task');
      expect(byId['task-nested-map']!.displayDetails[0], 'Title: Refill Rx');
      expect(
          byId['task-nested-map']!.displayDetails[1], 'Note: Take after food');

      expect(byId['task-nested-json-string']!.displayDetails[0], 'Title: Walk');
      expect(byId['task-nested-json-string']!.displayDetails[1],
          'Note: 15 minutes');

      expect(byId['generic']!.displayTitle, 'Queued Update');
      expect(byId['generic']!.displayDetails.join(' | '),
          contains('Field: fallback field'));
      expect(byId['generic']!.displayDetails.join(' | '),
          contains('Camel Case Field: true'));
      expect(byId['generic']!.displayDetails.join(' | '),
          isNot(contains('password')));
      expect(byId['generic']!.displayDetails.length, lessThanOrEqualTo(5));

      expect(byId['waiting']!.displayTitle, 'Queued Update');
      expect(byId['waiting']!.displayDetails, ['Waiting to sync']);
    });

    test(
        'syncQueuedRequestById handles missing id, unsupported status, invalid URL, 200 success, 409 success, 500 failure, and network failure',
        () async {
      expect(await service.syncQueuedRequestById('missing-id'), isTrue);

      await _insertRawRow(
        id: 'completed-row',
        method: 'POST',
        url: 'https://example.com/v1/api/tasks',
        headersJson: '{}',
        bodyJson: '{}',
        createdAt: '2026-01-01T00:00:00.000Z',
        fingerprint: 'completed-fp',
        status: 'completed',
      );
      expect(await service.syncQueuedRequestById('completed-row'), isFalse);

      await _insertRawRow(
        id: 'invalid-url-row',
        method: 'POST',
        url: '::bad-uri::',
        headersJson: '{}',
        bodyJson: '{}',
        createdAt: '2026-01-01T00:00:00.000Z',
        fingerprint: 'invalid-url-fp',
        status: 'pending',
      );
      expect(await service.syncQueuedRequestById('invalid-url-row'), isFalse);
      final invalidUrlRow = await db.getOfflineSyncById('invalid-url-row');
      expect(invalidUrlRow!.status, 'failed');
      expect(invalidUrlRow.lastError, 'Invalid queued URL');

      final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      server.listen((HttpRequest request) async {
        if (request.uri.path == '/ok') {
          request.response.statusCode = 200;
          await request.response.close();
          return;
        }
        if (request.uri.path == '/conflict') {
          request.response.statusCode = 409;
          await request.response.close();
          return;
        }
        if (request.uri.path == '/fail') {
          request.response.statusCode = 500;
          request.response.write('server failed badly');
          await request.response.close();
          return;
        }
      });

      final base = 'http://${server.address.host}:${server.port}';

      final okId = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('$base/ok'),
        headers: const {'content-type': 'application/json'},
        body: '{"ok":true}',
      );
      final conflictId = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('$base/conflict'),
        headers: const {'content-type': 'application/json'},
        body: '{"ok":true}',
      );
      final failId = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('$base/fail'),
        headers: const {'content-type': 'application/json'},
        body: '{"ok":true}',
      );
      final networkFailId = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('http://127.0.0.1:1/will-fail'),
        headers: const {'content-type': 'application/json'},
        body: '{"ok":true}',
      );

      expect(await service.syncQueuedRequestById(okId), isTrue);
      expect(await db.getOfflineSyncById(okId), isNull);

      expect(await service.syncQueuedRequestById(conflictId), isTrue);
      expect(await db.getOfflineSyncById(conflictId), isNull);

      expect(await service.syncQueuedRequestById(failId), isFalse);
      final failedHttp = await db.getOfflineSyncById(failId);
      expect(failedHttp!.status, 'failed');
      expect(failedHttp.retryCount, 1);
      expect(failedHttp.lastError, contains('HTTP 500'));

      expect(await service.syncQueuedRequestById(networkFailId), isFalse);
      final failedNetwork = await db.getOfflineSyncById(networkFailId);
      expect(failedNetwork!.status, 'failed');
      expect(failedNetwork.retryCount, 1);
      expect(failedNetwork.lastError, isNotEmpty);

      await server.close(force: true);
    });

    test('syncPendingQueue returns correct empty and mixed summaries',
        () async {
      final emptySummary = await service.syncPendingQueue(limit: 50);
      expect(emptySummary.attempted, 0);
      expect(emptySummary.succeeded, 0);
      expect(emptySummary.failed, 0);

      final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      server.listen((HttpRequest request) async {
        if (request.uri.path == '/good') {
          request.response.statusCode = 200;
        } else {
          request.response.statusCode = 500;
          request.response.write('nope');
        }
        await request.response.close();
      });

      final base = 'http://${server.address.host}:${server.port}';

      await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('$base/good'),
        headers: const {'content-type': 'application/json'},
        body: '{"x":1}',
      );
      await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('$base/bad'),
        headers: const {'content-type': 'application/json'},
        body: '{"x":2}',
      );

      final summary = await service.syncPendingQueue(limit: 50);
      expect(summary.attempted, 2);
      expect(summary.succeeded, 1);
      expect(summary.failed, 1);

      await server.close(force: true);
    });

    test(
        'OfflineQueueHttpClient passes through and queues only supported failed writes',
        () async {
      final passThroughClient = FakeInnerClient((request) async {
        return http.StreamedResponse(Stream.value(utf8.encode('ok')), 200,
            request: request);
      });

      final wrapper = OfflineQueueHttpClient(
        inner: passThroughClient,
        offlineSyncService: service,
      );

      final getRequest =
          http.Request('GET', Uri.parse('https://example.com/items'));
      final getResponse = await wrapper.send(getRequest);
      expect(getResponse.statusCode, 200);

      final authRequest = http.Request(
          'POST', Uri.parse('https://example.com/v1/api/auth/login'));
      final authResponse = await wrapper.send(authRequest);
      expect(authResponse.statusCode, 200);

      final replayRequest =
          http.Request('POST', Uri.parse('https://example.com/tasks'));
      replayRequest.headers[OfflineSyncService.replayHeader] = 'true';
      final replayResponse = await wrapper.send(replayRequest);
      expect(replayResponse.statusCode, 200);

      // Also cover the two pass-through branches in OfflineQueueHttpClient:
      final mp = http.MultipartRequest(
          'POST', Uri.parse('https://example.com/v1/api/tasks'));
      final mpResp = await wrapper.send(mp);
      expect(mpResp.statusCode, 200);

      final streamedReq = http.StreamedRequest(
          'POST', Uri.parse('https://example.com/v1/api/tasks'));
      final streamedResp = await wrapper.send(streamedReq);
      expect(streamedResp.statusCode, 200);

      wrapper.close();
      expect(passThroughClient.closed, isTrue);

      final timeoutClient = FakeInnerClient((request) async {
        throw TimeoutException('offline');
      });

      final queueingWrapper = OfflineQueueHttpClient(
        inner: timeoutClient,
        offlineSyncService: service,
        canQueueWrites: () => true,
      );

      final writeRequest = http.Request(
        'POST',
        Uri.parse('https://example.com/v1/api/tasks'),
      )..body = '{"title":"Queued"}';

      final queued = await queueingWrapper.send(writeRequest);
      final queuedResponse = await http.Response.fromStream(queued);
      expect(queuedResponse.statusCode, 200);
      expect(jsonDecode(queuedResponse.body)['offline'], isTrue);

      final noQueueClient = FakeInnerClient((request) async {
        throw TimeoutException('offline');
      });

      final noQueueWrapper = OfflineQueueHttpClient(
        inner: noQueueClient,
        offlineSyncService: service,
        canQueueWrites: () => false,
      );

      expect(
        () => noQueueWrapper.send(
          http.Request('POST', Uri.parse('https://example.com/v1/api/tasks')),
        ),
        throwsA(isA<TimeoutException>()),
      );
    });
  });
}
