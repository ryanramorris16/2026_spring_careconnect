import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import 'api_service.dart';

class ChatOutboxService {
  static const String _storageKey = 'chat_pending_outbox_v1';
  static const int _maxQueueSize = 200;

  static Future<void> enqueueMessage({
    required int senderId,
    required int receiverId,
    required String content,
    Map<String, dynamic>? attachment,
    String? receiverName,
  }) async {
    final queue = await _loadQueue();
    final payload = <String, dynamic>{
      'id': '${DateTime.now().millisecondsSinceEpoch}_${queue.length}',
      'senderId': senderId,
      'receiverId': receiverId,
      'content': content,
      if (attachment != null) 'attachment': attachment,
      if (receiverName != null && receiverName.trim().isNotEmpty)
        'receiverName': receiverName.trim(),
      'queuedAt': DateTime.now().toIso8601String(),
    };

    queue.add(payload);

    if (queue.length > _maxQueueSize) {
      queue.removeRange(0, queue.length - _maxQueueSize);
    }

    await _saveQueue(queue);
  }

  static Future<int> retryQueuedMessages({required int senderId}) async {
    final queue = await _loadQueue();
    if (queue.isEmpty) return 0;

    final remaining = <Map<String, dynamic>>[];
    var sentCount = 0;

    for (final queued in queue) {
      final queuedSender = queued['senderId'];
      if (queuedSender is! int || queuedSender != senderId) {
        remaining.add(queued);
        continue;
      }

      try {
        final attachmentRaw = queued['attachment'];
        final attachment = attachmentRaw is Map
            ? Map<String, dynamic>.from(attachmentRaw)
            : null;

        final response = await ApiService.sendMessage(
          senderId: queued['senderId'] as int,
          receiverId: queued['receiverId'] as int,
          content: (queued['content'] ?? '').toString(),
          attachment: attachment,
        );

        if (response.statusCode >= 200 && response.statusCode < 300) {
          sentCount += 1;
        } else {
          remaining.add(queued);
        }
      } catch (_) {
        remaining.add(queued);
      }
    }

    await _saveQueue(remaining);
    return sentCount;
  }

  static Future<int> getPendingCountForSender(int senderId) async {
    final queue = await _loadQueue();
    return queue.where((m) => m['senderId'] == senderId).length;
  }

  static Future<List<Map<String, dynamic>>> getQueuedMessagesForSender(
    int senderId,
  ) async {
    final queue = await _loadQueue();
    return queue
        .where((m) => m['senderId'] == senderId)
        .map((m) => Map<String, dynamic>.from(m))
        .toList()
      ..sort((a, b) {
        final aTime = DateTime.tryParse(a['queuedAt']?.toString() ?? '');
        final bTime = DateTime.tryParse(b['queuedAt']?.toString() ?? '');
        return (aTime ?? DateTime.fromMillisecondsSinceEpoch(0)).compareTo(
          bTime ?? DateTime.fromMillisecondsSinceEpoch(0),
        );
      });
  }

  static Future<List<Map<String, dynamic>>> getQueuedMessagesBetween({
    required int senderId,
    required int receiverId,
  }) async {
    final queue = await _loadQueue();
    return queue
        .where(
          (m) => m['senderId'] == senderId && m['receiverId'] == receiverId,
        )
        .map((m) => Map<String, dynamic>.from(m))
        .toList()
      ..sort((a, b) {
        final aTime = DateTime.tryParse(a['queuedAt']?.toString() ?? '');
        final bTime = DateTime.tryParse(b['queuedAt']?.toString() ?? '');
        return (aTime ?? DateTime.fromMillisecondsSinceEpoch(0)).compareTo(
          bTime ?? DateTime.fromMillisecondsSinceEpoch(0),
        );
      });
  }

  static Future<List<Map<String, dynamic>>> _loadQueue() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_storageKey);
      if (raw == null || raw.isEmpty) return [];

      final decoded = jsonDecode(raw);
      if (decoded is! List) return [];

      return decoded
          .whereType<Map>()
          .map((item) => Map<String, dynamic>.from(item))
          .toList();
    } catch (_) {
      return [];
    }
  }

  static Future<void> _saveQueue(List<Map<String, dynamic>> queue) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_storageKey, jsonEncode(queue));
  }
}
