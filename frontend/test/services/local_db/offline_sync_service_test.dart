import 'dart:async';
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:care_connect_app/services/local_db/offline_sync_service.dart';

void main() {
  // This must be first
  TestWidgetsFlutterBinding.ensureInitialized();

  const MethodChannel pathChannel = MethodChannel('plugins.flutter.io/path_provider');
  const MethodChannel secureStorageChannel = MethodChannel('plugins.it_nomads.com/flutter_secure_storage');

  // We define the service variable here but DON'T initialize it yet
  late OfflineSyncService service;

  setUpAll(() {
    // Mock PathProvider for the AppDatabase
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(pathChannel, (methodCall) async => '.');

    // Mock SecureStorage for AuthTokenManager
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(secureStorageChannel, (methodCall) async {
      if (methodCall.method == 'read') return 'mock_token';
      return null;
    });
  });

  setUp(() {
    // Accessing the instance INSIDE setUp prevents the "No current invoker" error
    service = OfflineSyncService.instance();
  });

  group('OfflineSyncService - Essential Coverage', () {
    test('isQueueableMethod correctly identifies methods', () {
      expect(service.isQueueableMethod('POST'), isTrue);
      expect(service.isQueueableMethod('GET'), isFalse);
    });

    test('shouldQueueForError identifies network issues', () {
      expect(service.shouldQueueForError(http.ClientException('')), isTrue);
      expect(service.shouldQueueForError(TimeoutException('')), isTrue);
    });

    test('Display Logic: Mood Check-In formatting', () async {
      final id = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('https://api.careconnect.com/v1/api/mood-pain-log'),
        body: jsonEncode({'moodValue': 5}),
      );

      final queue = await service.getPendingQueue();
      final item = queue.firstWhere((e) => e.id == id);
      
      expect(item.displayTitle, 'Mood Check-In');
      expect(item.displayDetails.join(), contains('5'));
    });

    test('Display Logic: Task formatting', () async {
      final id = await service.enqueueRequest(
        method: 'POST',
        uri: Uri.parse('https://api.careconnect.com/v1/api/tasks'),
        body: jsonEncode({'title': 'Morning Meds'}),
      );

      final queue = await service.getPendingQueue();
      final item = queue.firstWhere((e) => e.id == id);
      
      expect(item.displayTitle, 'Task');
      expect(item.displayDetails.join(), contains('Morning Meds'));
    });
  });

  group('OfflineQueueHttpClient - Interception Coverage', () {
    test('Queues failing POST requests', () async {
      // Mock client that simulates a network failure
      final mockInner = MockClient((request) async {
        throw http.ClientException('No Internet');
      });

      final client = OfflineQueueHttpClient(
        inner: mockInner,
        offlineSyncService: service,
      );

      final response = await client.post(
        Uri.parse('https://api.careconnect.com/v1/api/mood'),
        body: jsonEncode({'moodValue': 4}),
      );

      expect(response.statusCode, 200);
      expect(jsonDecode(response.body)['queued'], isTrue);
    });
  });
}
