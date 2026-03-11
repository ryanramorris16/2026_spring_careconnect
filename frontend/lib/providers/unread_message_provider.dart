import 'dart:async';

import 'package:flutter/foundation.dart';

import '../services/api_service.dart';
import '../services/chat_websocket_service.dart';

class UnreadMessageProvider extends ChangeNotifier {
  int _unreadCount = 0;
  String? _currentUserId;
  StreamSubscription<ChatMessage>? _messageSubscription;

  int get unreadCount => _unreadCount;
  bool get hasUnread => _unreadCount > 0;

  Future<void> initializeForUser(String userId) async {
    if (_currentUserId == userId && _messageSubscription != null) {
      return;
    }

    await _messageSubscription?.cancel();
    _messageSubscription = null;
    _currentUserId = userId;

    await ChatWebSocketService.initialize(userId: userId);
    await refreshUnreadCount();

    _messageSubscription = ChatWebSocketService.onMessageReceived.listen((msg) {
      if (msg.recipientId == _currentUserId) {
        _unreadCount += 1;
        notifyListeners();
      }
    });
  }

  Future<void> refreshUnreadCount() async {
    if (_currentUserId == null) return;

    try {
      final userId = int.tryParse(_currentUserId!);
      if (userId == null) return;
      final count = await ApiService.getUnreadMessageCount(userId);
      _unreadCount = count;
      notifyListeners();
    } catch (_) {
      // Keep existing badge count if refresh fails.
    }
  }

  void resetUnreadCount() {
    _unreadCount = 0;
    notifyListeners();
  }

  @override
  void dispose() {
    _messageSubscription?.cancel();
    super.dispose();
  }
}
