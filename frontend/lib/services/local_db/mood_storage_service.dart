import 'dart:convert';

import 'package:http/http.dart' as http;

import '../api_service.dart';
import 'app_database.dart';
import 'connectivity_router_service.dart';

/// Routes mood reads/writes between backend APIs and local encrypted storage.
class MoodStorageService {
  MoodStorageService({
    required ConnectivityRouterService connectivityRouter,
    AppDatabase? appDatabase,
  })  : _connectivityRouter = connectivityRouter,
        _appDatabase = appDatabase ?? AppDatabase();

  final ConnectivityRouterService _connectivityRouter;
  final AppDatabase _appDatabase;

  /// Saves mood online when available, otherwise stores it locally.
  Future<void> saveMood({
    required int userId,
    required int score,
    required String label,
  }) async {
    await _connectivityRouter.route<void>(
      online: () async {
        final response = await ApiService.saveMoodScore(
          userId: userId,
          score: score,
          label: label,
        );
        _throwIfHttpError(response, 'save mood to backend');
      },
      offline: () async {
        await _appDatabase.insertMood(
          userId: userId,
          score: score,
          label: label,
          createdAt: DateTime.now(),
        );
      },
    );
  }

  /// Returns mood history from backend when online, local DB when offline.
  Future<List<Map<String, dynamic>>> getMoodHistory(int userId) async {
    return _connectivityRouter.route<List<Map<String, dynamic>>>(
      online: () async {
        final backendResponse = await ApiService.getMoodHistory(userId);
        return backendResponse
            .whereType<Map<String, dynamic>>()
            .map((entry) => Map<String, dynamic>.from(entry))
            .toList();
      },
      offline: () async {
        final localRows = await _appDatabase.getMoodsForUser(userId);
        return localRows
            .map(
              (row) => {
                'id': row.id,
                'userId': row.userId,
                'score': row.score,
                'label': row.label,
                'createdAt': row.createdAt.toIso8601String(),
                'source': 'offline',
              },
            )
            .toList();
      },
    );
  }

  /// Returns true when local encryption key exists.
  Future<bool> isLocalDbEncrypted() async {
    return _appDatabase.isEncrypted();
  }

  /// Closes the local database handle.
  Future<void> close() => _appDatabase.close();

  void _throwIfHttpError(http.Response response, String action) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return;
    }

    final message = _extractErrorMessage(response.body);
    throw Exception(
      'Failed to $action. Status: ${response.statusCode}. Error: $message',
    );
  }

  String _extractErrorMessage(String body) {
    if (body.isEmpty) {
      return 'No response body';
    }

    try {
      final decoded = jsonDecode(body);
      if (decoded is Map<String, dynamic>) {
        if (decoded['message'] is String) {
          return decoded['message'] as String;
        }
        if (decoded['error'] is String) {
          return decoded['error'] as String;
        }
      }
    } catch (_) {
      return body;
    }
    return body;
  }
}
