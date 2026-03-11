import 'dart:async';
import 'dart:convert';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'auth_token_manager.dart';
import 'chat_outbox_service.dart';
import '../config/env_constant.dart';

/// Model for incoming/outgoing chat messages
class ChatMessage {
  final String? messageId;
  final String? clientMessageId;
  final String senderId;
  final String recipientId;
  final String content;
  final DateTime timestamp;
  final Map<String, dynamic>? attachment;
  bool isDelivered;
  bool isRead;

  ChatMessage({
    this.messageId,
    this.clientMessageId,
    required this.senderId,
    required this.recipientId,
    required this.content,
    required this.timestamp,
    this.attachment,
    this.isDelivered = false,
    this.isRead = false,
  });

  Map<String, dynamic> toJson() {
    return {
      'messageId': messageId,
      'clientMessageId': clientMessageId,
      'senderId': senderId,
      'recipientId': recipientId,
      'content': content,
      'timestamp': timestamp.toIso8601String(),
      if (attachment != null) 'attachment': attachment,
      'isDelivered': isDelivered,
      'isRead': isRead,
    };
  }

  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    final rawContent =
        json['content'] ?? json['message'] ?? json['text'] ?? json['body'];

    return ChatMessage(
      messageId: json['messageId']?.toString(),
      clientMessageId: json['clientMessageId']?.toString(),
      senderId: json['senderId']?.toString() ?? '',
      recipientId: json['recipientId']?.toString() ?? '',
      content: rawContent?.toString() ?? '',
      timestamp:
          DateTime.tryParse(json['timestamp']?.toString() ?? '') ??
          DateTime.now(),
      attachment: json['attachment'] is Map<String, dynamic>
          ? Map<String, dynamic>.from(json['attachment'])
          : null,
      isDelivered:
          json['delivered'] as bool? ?? json['isDelivered'] as bool? ?? false,
      isRead: json['isRead'] as bool? ?? json['read'] as bool? ?? false,
    );
  }
}

/// Service for managing real-time P2P chat via WebSocket
class ChatWebSocketService {
  static WebSocketChannel? _channel;
  static bool _isConnected = false;
  static bool _isAuthenticated = false;
  static String? _currentUserId;

  /// Stream controllers for different message types
  static final StreamController<ChatMessage> _messageReceived =
      StreamController<ChatMessage>.broadcast();
  static final StreamController<Map<String, dynamic>> _typingIndicators =
      StreamController<Map<String, dynamic>>.broadcast();
  static final StreamController<Map<String, dynamic>> _readReceipts =
      StreamController<Map<String, dynamic>>.broadcast();
  static final StreamController<String> _connectionStatus =
      StreamController<String>.broadcast();
  static final StreamController<Map<String, dynamic>> _errors =
      StreamController<Map<String, dynamic>>.broadcast();

  /// Expose streams for listeners
  static Stream<ChatMessage> get onMessageReceived => _messageReceived.stream;
  static Stream<Map<String, dynamic>> get onTypingIndicator =>
      _typingIndicators.stream;
  static Stream<Map<String, dynamic>> get onReadReceipt => _readReceipts.stream;
  static Stream<String> get connectionStatusUpdates => _connectionStatus.stream;
  static Stream<Map<String, dynamic>> get onError => _errors.stream;

  /// Initialize WebSocket connection
  static Future<void> initialize({required String userId}) async {
    if (_isConnected) {
      // Reuse the socket only when it is already bound to the same user.
      if (_currentUserId == userId && _isAuthenticated) {
        print('✅ ChatWebSocketService already initialized for user $userId');
        return;
      }

      // If account changed (or auth is stale), reconnect as the requested user.
      print(
        '🔄 Reinitializing chat socket from user $_currentUserId to $userId',
      );
      await disconnect();
    }

    try {
      _currentUserId = userId;

      final wsUrl = _getChatWebSocketUrl();
      print('🔌 Connecting to chat WebSocket: $wsUrl');

      _channel = WebSocketChannel.connect(Uri.parse(wsUrl));
      _isConnected = true;
      _connectionStatus.add('connected');

      // Listen to messages from server
      _channel!.stream.listen(
        (message) => _handleMessage(message),
        onError: (error) {
          print('❌ ChatWebSocketService error: $error');
          _connectionStatus.add('error');
          _errors.add({'error': error.toString(), 'timestamp': DateTime.now()});
          _isConnected = false;
          _isAuthenticated = false;
        },
        onDone: () {
          print('🔌 ChatWebSocketService connection closed');
          _connectionStatus.add('disconnected');
          _isConnected = false;
          _isAuthenticated = false;
        },
        cancelOnError: false,
      );

      // Authenticate after connection
      await _authenticate(userId);
    } catch (e, stackTrace) {
      print('❌ Error initializing ChatWebSocketService: $e');
      print('Stack trace: $stackTrace');
      _isConnected = false;
      _connectionStatus.add('failed');
      _errors.add({'error': e.toString(), 'stackTrace': stackTrace.toString()});
      rethrow;
    }
  }

  /// Authenticate user after connecting
  static Future<void> _authenticate(String userId) async {
    if (_channel == null || !_isConnected) {
      throw Exception('WebSocket not connected');
    }

    try {
      final authHeaders = await AuthTokenManager.getAuthHeaders();
      final token =
          authHeaders['Authorization']?.replaceFirst('Bearer ', '') ?? '';

      final authMessage = {
        'type': 'authenticate',
        'userId': userId,
        'token': token,
      };

      _channel!.sink.add(jsonEncode(authMessage));
      _isAuthenticated = true;
      _connectionStatus.add('authenticated');
      print('✅ User $userId authenticated in chat');
    } catch (e) {
      print('❌ Authentication failed: $e');
      _connectionStatus.add('auth-failed');
      _errors.add({'error': 'Authentication failed: $e'});
      rethrow;
    }
  }

  /// Send a chat message
  static Future<bool> sendMessage({
    required String recipientId,
    required String content,
    Map<String, dynamic>? attachment,
  }) async {
    if (!_isAuthenticated || _channel == null || !_isConnected) {
      print('❌ WebSocket not authenticated or connected');
      _errors.add({'error': 'WebSocket not connected'});
      return false;
    }

    try {
      final clientMessageId = '${DateTime.now().millisecondsSinceEpoch}_msg';

      final message = {
        'type': 'message',
        'messageId': clientMessageId,
        'senderId': _currentUserId,
        'recipientId': recipientId,
        'content': content,
        if (attachment != null) 'attachment': attachment,
        'timestamp': DateTime.now().toIso8601String(),
      };

      _channel!.sink.add(jsonEncode(message));
      print('✉️  Message sent to $recipientId: "$content"');
      return true;
    } catch (e) {
      print('❌ Error sending message: $e');
      _errors.add({'error': 'Failed to send message: $e'});
      return false;
    }
  }

  /// Send typing indicator
  static Future<void> sendTypingIndicator({
    required String recipientId,
    required bool isTyping,
  }) async {
    if (!_isAuthenticated || _channel == null || !_isConnected) return;

    try {
      final indicator = {
        'type': 'typing',
        'senderId': _currentUserId,
        'recipientId': recipientId,
        'isTyping': isTyping,
      };

      _channel!.sink.add(jsonEncode(indicator));
    } catch (e) {
      print('❌ Error sending typing indicator: $e');
    }
  }

  /// Mark message as read
  static Future<void> markMessageAsRead({
    required String messageId,
    required String senderId,
  }) async {
    if (!_isAuthenticated || _channel == null || !_isConnected) return;

    try {
      final readReceipt = {
        'type': 'read-receipt',
        'messageId': messageId,
        'recipientId': _currentUserId,
      };

      _channel!.sink.add(jsonEncode(readReceipt));
      print('✅ Message $messageId marked as read');
    } catch (e) {
      print('❌ Error marking message as read: $e');
    }
  }

  /// Handle incoming messages from server
  static void _handleMessage(dynamic message) {
    try {
      final payload = jsonDecode(message) as Map<String, dynamic>;
      final type = payload['type'] as String?;

      print('📨 Received message type: $type');

      switch (type) {
        case 'authenticated':
          print('✅ Authentication confirmed');
          final senderId = int.tryParse(_currentUserId ?? '');
          if (senderId != null) {
            ChatOutboxService.retryQueuedMessages(senderId: senderId).then((
              count,
            ) {
              if (count > 0) {
                print('📤 Retried and sent $count queued chat message(s)');
              }
            });
          }
          break;

        case 'message-received':
          final chatMsg = ChatMessage.fromJson(payload);
          _messageReceived.add(chatMsg);
          print(
            '📨 Message received from ${chatMsg.senderId}: "${chatMsg.content}"',
          );
          break;

        case 'message-sent':
          final clientMsgId = payload['clientMessageId'];
          final serverMsgId = payload['messageId'];
          final delivered = payload['delivered'] as bool?;
          print(
            '✅ Message sent (client: $clientMsgId, server: $serverMsgId, delivered: $delivered)',
          );
          break;

        case 'user-typing':
          _typingIndicators.add(payload);
          final sender = payload['senderId'];
          final isTyping = payload['isTyping'] as bool?;
          print(
            '✍️  User $sender is ${isTyping == true ? 'typing' : 'stopped typing'}',
          );
          break;

        case 'message-read':
          _readReceipts.add(payload);
          print('✅ Message ${payload['messageId']} was read');
          break;

        case 'error':
          _errors.add(payload);
          print('❌ Server error: ${payload['message']}');
          break;

        case 'connection-established':
          print('🔌 Connection established: ${payload['message']}');
          break;

        default:
          print('⚠️  Unknown message type: $type');
      }
    } catch (e) {
      print('❌ Error handling message: $e');
      _errors.add({'error': 'Failed to parse message', 'raw': message});
    }
  }

  /// Get WebSocket URL for chat
  static String _getChatWebSocketUrl() {
    // Use the same base as the backend but on /ws/chat endpoint
    final base = _getWebSocketBaseUrl();
    return '$base/ws/chat';
  }

  /// Get WebSocket base URL
  static String _getWebSocketBaseUrl() {
    try {
      // Check for environment override
      const wsOverride = String.fromEnvironment('WEBSOCKET_SERVER_URL');
      if (wsOverride.isNotEmpty) {
        return wsOverride;
      }

      // Use API base URL and convert to WebSocket
      final apiBase = getBackendBaseUrl();
      if (apiBase.startsWith('https://')) {
        return apiBase.replaceFirst('https://', 'wss://');
      } else if (apiBase.startsWith('http://')) {
        return apiBase.replaceFirst('http://', 'ws://');
      }
      return 'ws://localhost:8080';
    } catch (e) {
      print('⚠️  Error getting WebSocket base URL: $e, using default');
      return 'ws://localhost:8080';
    }
  }

  /// Check if connected and authenticated
  static bool get isConnected => _isConnected && _isAuthenticated;

  /// Check if currently connected
  static bool get isConnectionOpen => _isConnected;

  /// Get current user ID
  static String? get currentUserId => _currentUserId;

  /// Disconnect and cleanup
  static Future<void> disconnect() async {
    try {
      _isAuthenticated = false;
      _isConnected = false;
      if (_channel != null) {
        await _channel!.sink.close();
        _channel = null;
      }
      _connectionStatus.add('disconnected');
      print('✅ ChatWebSocketService disconnected');
    } catch (e) {
      print('❌ Error disconnecting: $e');
    }
  }

  /// Cleanup all resources
  static Future<void> dispose() async {
    await disconnect();
    await _messageReceived.close();
    await _typingIndicators.close();
    await _readReceipts.close();
    await _connectionStatus.close();
    await _errors.close();
  }
}
