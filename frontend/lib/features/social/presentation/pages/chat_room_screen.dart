import 'dart:async';
import 'package:care_connect_app/services/api_service.dart';
import 'package:care_connect_app/services/chat_websocket_service.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';
import 'package:universal_html/html.dart' as html;
import 'package:url_launcher/url_launcher.dart';

import '../../../../providers/unread_message_provider.dart';
import '../../../../providers/user_provider.dart';
import '../model/message_dto.dart';

class ChatRoomScreen extends StatefulWidget {
  final int peerUserId;
  final String peerName;

  const ChatRoomScreen({
    super.key,
    required this.peerUserId,
    required this.peerName,
  });

  @override
  State<ChatRoomScreen> createState() => _ChatRoomScreenState();
}

class _ChatRoomScreenState extends State<ChatRoomScreen> {
  final TextEditingController _controller = TextEditingController();

  int? _currentUserId;
  List<MessageDto> messages = [];
  bool isLoading = true;
  bool _initialLoading = true;
  bool _webSocketConnected = false;
  StreamSubscription? _messageSubscription;
  StreamSubscription? _typingSubscription;
  StreamSubscription? _readReceiptSubscription;
  StreamSubscription? _connectionSubscription;
  Map<String, bool> _messageDeliveryStatus = {}; // clientMessageId -> delivered
  bool _initialized = false;
  PlatformFile? _selectedAttachment;
  bool _isUploadingAttachment = false;

  int? _toNullableInt(dynamic value) {
    if (value is int) return value;
    if (value is num) return value.toInt();
    return int.tryParse(value?.toString() ?? '');
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    if (!_initialized) {
      final user = Provider.of<UserProvider>(context, listen: false).user;
      if (user == null) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('User not logged in')));
        return;
      }

      _currentUserId = user.id;

      // Load conversation history via REST
      fetchConversation();

      // Initialize WebSocket for real-time messages
      _initializeWebSocket();

      _initialized = true;
    }
  }

  Future<void> _initializeWebSocket() async {
    try {
      if (_currentUserId == null) return;

      // Initialize WebSocket service
      await ChatWebSocketService.initialize(userId: _currentUserId.toString());

      if (!mounted) return;

      setState(() => _webSocketConnected = true);
      print('✅ WebSocket initialized for user $_currentUserId');

      // Listen for incoming messages
      _messageSubscription = ChatWebSocketService.onMessageReceived.listen((
        message,
      ) {
        if (mounted &&
            message.senderId == widget.peerUserId.toString() &&
            message.recipientId == _currentUserId.toString()) {
          _addMessageToUI(message);
        }
      });

      // Listen for connection status
      _connectionSubscription = ChatWebSocketService.connectionStatusUpdates
          .listen((status) {
            if (mounted) {
              setState(() => _webSocketConnected = status == 'authenticated');
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text('Chat connection: $status'),
                  duration: const Duration(seconds: 1),
                ),
              );
            }
          });

      // Listen for typing indicators (optional)
      _typingSubscription = ChatWebSocketService.onTypingIndicator.listen((
        data,
      ) {
        if (mounted && data['senderId'] == widget.peerUserId.toString()) {
          print('✍️  ${widget.peerName} is typing...');
        }
      });

      // Listen for read receipts so sender sees live "Seen" updates.
      _readReceiptSubscription = ChatWebSocketService.onReadReceipt.listen((
        data,
      ) {
        _handleReadReceipt(data);
      });
    } catch (e) {
      print('❌ Failed to initialize WebSocket: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to connect to chat: $e')),
        );
      }
    }
  }

  void _addMessageToUI(ChatMessage message) {
    final newMsg = MessageDto(
      id:
          int.tryParse(message.messageId ?? '') ??
          DateTime.now().millisecondsSinceEpoch,
      senderId: int.parse(message.senderId),
      receiverId: int.parse(message.recipientId),
      content: message.content,
      timestamp: message.timestamp,
      isRead: false,
      attachmentFileId: _toNullableInt(message.attachment?['fileId']),
      attachmentUrl: message.attachment?['url']?.toString(),
      attachmentName: message.attachment?['name']?.toString(),
      attachmentContentType: message.attachment?['contentType']?.toString(),
      attachmentSize: _toNullableInt(message.attachment?['size']),
    );

    if (mounted) {
      setState(() {
        // Check if message already exists
        if (!messages.any((m) => m.id == newMsg.id)) {
          messages.add(newMsg);
        }
      });

      // Scroll to bottom
      Future.delayed(const Duration(milliseconds: 100), () {
        // Auto-scroll if needed
      });

      // Mark as read
      ChatWebSocketService.markMessageAsRead(
        messageId: message.messageId ?? newMsg.id.toString(),
        senderId: message.senderId,
      );

      // In the currently open room, new incoming messages are effectively read.
      setState(() {
        final idx = messages.indexWhere((m) => m.id == newMsg.id);
        if (idx != -1) {
          messages[idx] = MessageDto(
            id: messages[idx].id,
            senderId: messages[idx].senderId,
            receiverId: messages[idx].receiverId,
            content: messages[idx].content,
            timestamp: messages[idx].timestamp,
            isRead: true,
            attachmentFileId: messages[idx].attachmentFileId,
            attachmentUrl: messages[idx].attachmentUrl,
            attachmentName: messages[idx].attachmentName,
            attachmentContentType: messages[idx].attachmentContentType,
            attachmentSize: messages[idx].attachmentSize,
          );
        }
      });
    }
  }

  void _handleReadReceipt(Map<String, dynamic> data) {
    final messageId = int.tryParse(data['messageId']?.toString() ?? '');
    if (messageId == null || _currentUserId == null || !mounted) return;

    setState(() {
      messages = messages.map((m) {
        final isOutgoingToPeer =
            m.senderId == _currentUserId && m.receiverId == widget.peerUserId;
        if (isOutgoingToPeer && m.id == messageId) {
          return MessageDto(
            id: m.id,
            senderId: m.senderId,
            receiverId: m.receiverId,
            content: m.content,
            timestamp: m.timestamp,
            isRead: true,
            attachmentFileId: m.attachmentFileId,
            attachmentUrl: m.attachmentUrl,
            attachmentName: m.attachmentName,
            attachmentContentType: m.attachmentContentType,
            attachmentSize: m.attachmentSize,
          );
        }
        return m;
      }).toList();
    });
  }

  Future<void> _markUnreadPeerMessagesAsRead() async {
    if (_currentUserId == null) return;

    final unreadIncoming = messages
        .where(
          (m) =>
              m.senderId == widget.peerUserId &&
              m.receiverId == _currentUserId &&
              !m.isRead,
        )
        .toList();

    for (final m in unreadIncoming) {
      await ChatWebSocketService.markMessageAsRead(
        messageId: m.id.toString(),
        senderId: m.senderId.toString(),
      );
    }

    if (mounted) {
      await Provider.of<UnreadMessageProvider>(
        context,
        listen: false,
      ).refreshUnreadCount();
    }
  }

  @override
  void dispose() {
    if (mounted) {
      Provider.of<UnreadMessageProvider>(
        context,
        listen: false,
      ).refreshUnreadCount();
    }
    _messageSubscription?.cancel();
    _typingSubscription?.cancel();
    _readReceiptSubscription?.cancel();
    _connectionSubscription?.cancel();
    _controller.dispose();
    super.dispose();
  }

  Future<void> fetchConversation({bool silent = false}) async {
    if (_currentUserId == null) return;

    if (!silent && _initialLoading) {
      setState(() => isLoading = true);
    }

    try {
      final data = await ApiService.getConversation(
        user1: _currentUserId!,
        user2: widget.peerUserId,
      );

      final updatedMessages = (data)
          .map((json) => MessageDto.fromJson(json))
          .toList();

      //Only update UI if messages actually changed
      if (!listEquals(messages, updatedMessages)) {
        if (mounted) {
          setState(() {
            messages = updatedMessages;
            isLoading = false;
            _initialLoading = false;
          });

          await _markUnreadPeerMessagesAsRead();
        }
      } else if (_initialLoading) {
        setState(() {
          isLoading = false;
          _initialLoading = false;
        });

        await _markUnreadPeerMessagesAsRead();
      }
    } catch (e) {
      if (!silent) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to load conversation: $e')),
        );
      }
      setState(() {
        isLoading = false;
        _initialLoading = false;
      });
    }
  }

  Future<void> sendMessage() async {
    final content = _controller.text.trim();
    final hasAttachment = _selectedAttachment != null;
    if ((content.isEmpty && !hasAttachment) || _currentUserId == null) return;

    Map<String, dynamic>? attachmentPayload;

    try {
      if (hasAttachment) {
        setState(() => _isUploadingAttachment = true);
        attachmentPayload = await _uploadSelectedAttachment(
          _selectedAttachment!,
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to upload attachment: $e')),
        );
      }
      setState(() => _isUploadingAttachment = false);
      return;
    }

    setState(() => _isUploadingAttachment = false);

    if (!_webSocketConnected) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Chat not connected. Trying REST API...')),
      );
      // Fallback to REST if WebSocket not connected
      await _sendMessageViaRest(content, attachment: attachmentPayload);
      return;
    }

    try {
      // Generate client message ID for tracking
      final clientMsgId = '${DateTime.now().millisecondsSinceEpoch}_msg';
      _messageDeliveryStatus[clientMsgId] = false;

      // Add optimistic message to UI with temporary negative ID
      final tempId =
          -DateTime.now().millisecondsSinceEpoch; // Negative to avoid conflicts
      final optimisticMsg = MessageDto(
        id: tempId,
        senderId: _currentUserId!,
        receiverId: widget.peerUserId,
        content: content.isNotEmpty
            ? content
            : 'Attachment: ${attachmentPayload?['name'] ?? 'file'}',
        timestamp: DateTime.now(),
        isRead: false,
        attachmentFileId: _toNullableInt(attachmentPayload?['fileId']),
        attachmentUrl: attachmentPayload?['url']?.toString(),
        attachmentName: attachmentPayload?['name']?.toString(),
        attachmentContentType: attachmentPayload?['contentType']?.toString(),
        attachmentSize: _toNullableInt(attachmentPayload?['size']),
      );

      _controller.clear();
      _selectedAttachment = null;

      if (mounted) {
        setState(() {
          messages.add(optimisticMsg);
        });
      }

      // Send via WebSocket
      final success = await ChatWebSocketService.sendMessage(
        recipientId: widget.peerUserId.toString(),
        content: content,
        attachment: attachmentPayload,
      );

      if (!success && mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Failed to send message')));
      } else {
        // Refresh to replace optimistic IDs with DB IDs and read state.
        await fetchConversation(silent: true);
      }
    } catch (e) {
      print('❌ Error sending message: $e');
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Failed to send message: $e')));
    }
  }

  Future<void> _sendMessageViaRest(
    String content, {
    Map<String, dynamic>? attachment,
  }) async {
    try {
      await ApiService.sendMessage(
        senderId: _currentUserId!,
        receiverId: widget.peerUserId,
        content: content,
        attachment: attachment,
      );

      _controller.clear();
      _selectedAttachment = null;
      await fetchConversation();
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Failed to send message: $e')));
    }
  }

  Widget buildMessageBubble(MessageDto msg) {
    final isMe = msg.senderId == _currentUserId;
    final isDelivered = _messageDeliveryStatus[msg.id.toString()] ?? true;
    final isSeen = msg.isRead;
    final hasAttachment =
        msg.attachmentUrl != null && msg.attachmentUrl!.trim().isNotEmpty;
    final messageText = msg.content.trim();

    return Align(
      alignment: isMe ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 12),
        margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 10),
        decoration: BoxDecoration(
          color: isMe ? Colors.blue.shade100 : Colors.grey.shade300,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: isMe
              ? CrossAxisAlignment.end
              : CrossAxisAlignment.start,
          children: [
            if (hasAttachment) ...[
              InkWell(
                onTap: () => _openAttachment(
                  msg.attachmentUrl,
                  fileName: msg.attachmentName,
                  contentType: msg.attachmentContentType,
                ),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 8,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.6),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        _attachmentIcon(msg.attachmentContentType),
                        size: 16,
                        color: Colors.blueGrey.shade700,
                      ),
                      const SizedBox(width: 8),
                      Flexible(
                        child: Text(
                          msg.attachmentName ?? 'Attachment',
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            color: Colors.blueGrey.shade900,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              if (messageText.isNotEmpty) const SizedBox(height: 6),
            ],
            if (messageText.isNotEmpty)
              Text(
                messageText,
                style: TextStyle(
                  color: isMe ? Colors.blueGrey.shade900 : Colors.grey.shade900,
                ),
              ),
            const SizedBox(height: 4),
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  _formatTime(msg.timestamp),
                  style: TextStyle(fontSize: 10, color: Colors.grey.shade600),
                ),
                if (isMe) ...[
                  const SizedBox(width: 4),
                  Icon(
                    isSeen
                        ? Icons.done_all
                        : (isDelivered ? Icons.done : Icons.schedule),
                    size: 12,
                    color: isSeen
                        ? Colors.blue
                        : (isDelivered ? Colors.grey : Colors.orange),
                  ),
                  const SizedBox(width: 3),
                  Text(
                    isSeen ? 'Seen' : (isDelivered ? 'Delivered' : 'Sending'),
                    style: TextStyle(fontSize: 10, color: Colors.grey.shade600),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _pickAttachment() async {
    final result = await FilePicker.platform.pickFiles(
      allowMultiple: false,
      withData: kIsWeb,
      type: FileType.custom,
      allowedExtensions: const [
        'jpg',
        'jpeg',
        'png',
        'gif',
        'webp',
        'bmp',
        'mp4',
        'mov',
        'avi',
        'mkv',
        'webm',
        'm4a',
        'mp3',
        'wav',
        'aac',
        'ogg',
        'opus',
        'pdf',
        'doc',
        'docx',
        'txt',
        'rtf',
        'xls',
        'xlsx',
        'ppt',
        'pptx',
      ],
    );

    if (result == null || result.files.isEmpty) return;
    setState(() {
      _selectedAttachment = result.files.first;
    });
  }

  Future<Map<String, dynamic>> _uploadSelectedAttachment(
    PlatformFile file,
  ) async {
    final String? path = file.path;
    final Uint8List? bytes = file.bytes;

    return ApiService.uploadChatAttachment(
      userId: _currentUserId!,
      fileName: file.name,
      filePath: path,
      fileBytes: bytes,
      category: _resolveAttachmentCategory(file),
    );
  }

  String _resolveAttachmentCategory(PlatformFile file) {
    final extension = file.extension?.toLowerCase() ?? '';
    const imageExtensions = {'jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'};
    const videoExtensions = {'mp4', 'mov', 'avi', 'mkv', 'webm'};
    const audioExtensions = {'m4a', 'mp3', 'wav', 'aac', 'ogg', 'opus'};

    if (imageExtensions.contains(extension)) return 'chat_image';
    if (videoExtensions.contains(extension)) return 'chat_video';
    if (audioExtensions.contains(extension)) return 'chat_voice_note';
    return 'chat_document';
  }

  IconData _attachmentIcon(String? contentType) {
    final lower = (contentType ?? '').toLowerCase();
    if (lower.startsWith('image/')) return Icons.image;
    if (lower.startsWith('video/')) return Icons.videocam;
    if (lower.startsWith('audio/')) return Icons.mic;
    if (lower.contains('pdf')) return Icons.picture_as_pdf;
    if (lower.contains('word') || lower.contains('document')) {
      return Icons.description;
    }
    return Icons.attach_file;
  }

  IconData _attachmentIconFromExtension(String? extension) {
    final ext = (extension ?? '').toLowerCase();
    const imageExtensions = {'jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'};
    const videoExtensions = {'mp4', 'mov', 'avi', 'mkv', 'webm'};
    const audioExtensions = {'m4a', 'mp3', 'wav', 'aac', 'ogg', 'opus'};
    const pdfExtensions = {'pdf'};
    const docExtensions = {
      'doc',
      'docx',
      'txt',
      'rtf',
      'xls',
      'xlsx',
      'ppt',
      'pptx',
    };

    if (imageExtensions.contains(ext)) return Icons.image;
    if (videoExtensions.contains(ext)) return Icons.videocam;
    if (audioExtensions.contains(ext)) return Icons.mic;
    if (pdfExtensions.contains(ext)) return Icons.picture_as_pdf;
    if (docExtensions.contains(ext)) return Icons.description;
    return Icons.attach_file;
  }

  Future<void> _openAttachment(
    String? url, {
    String? fileName,
    String? contentType,
  }) async {
    if (url == null || url.trim().isEmpty) return;
    final normalized = url.trim();
    final uri = Uri.tryParse(normalized);
    if (uri == null) return;

    final resolvedUri = uri.hasScheme
        ? uri
        : Uri.parse(ApiConstants.baseUrl).resolve(normalized);

    final inferredContentType =
        (contentType != null && contentType.trim().isNotEmpty)
        ? contentType
        : _inferContentTypeFromFileName(fileName);

    try {
      if (kIsWeb) {
        await _downloadAttachmentWithAuth(
          resolvedUri,
          suggestedFileName: fileName,
          contentTypeHint: inferredContentType,
          openInline: _isMediaContentType(inferredContentType),
        );
        return;
      }

      final authHeaders = await ApiService.getAuthHeaders();

      final openedInApp = await launchUrl(
        resolvedUri,
        mode: LaunchMode.inAppWebView,
        webViewConfiguration: WebViewConfiguration(headers: authHeaders),
      );

      if (openedInApp) {
        return;
      }

      final opened = await launchUrl(
        resolvedUri,
        mode: LaunchMode.externalApplication,
      );
      if (!opened && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Unable to open attachment')),
        );
      }
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Unable to open attachment')),
      );
    }
  }

  Future<void> _downloadAttachmentWithAuth(
    Uri uri, {
    String? suggestedFileName,
    String? contentTypeHint,
    bool openInline = false,
  }) async {
    final authHeaders = await ApiService.getAuthHeaders();
    final response = await http.get(uri, headers: authHeaders);

    if (response.statusCode != 200) {
      throw Exception('Download failed (${response.statusCode})');
    }

    final disposition = response.headers['content-disposition'];
    final contentType =
        response.headers['content-type'] ??
        contentTypeHint ??
        'application/octet-stream';
    final resolvedFileName =
        _extractFileNameFromDisposition(disposition) ??
        suggestedFileName ??
        'attachment';

    final blob = html.Blob([response.bodyBytes], contentType);
    final objectUrl = html.Url.createObjectUrlFromBlob(blob);

    if (openInline) {
      html.window.open(objectUrl, '_blank');
      // Delay revocation to let the browser finish opening the Blob URL.
      Future<void>.delayed(const Duration(seconds: 10), () {
        html.Url.revokeObjectUrl(objectUrl);
      });
      return;
    }

    final anchor = html.AnchorElement(href: objectUrl)
      ..download = resolvedFileName
      ..style.display = 'none';

    html.document.body?.append(anchor);
    anchor.click();
    anchor.remove();
    html.Url.revokeObjectUrl(objectUrl);
  }

  String? _extractFileNameFromDisposition(String? disposition) {
    if (disposition == null || disposition.isEmpty) return null;

    final utf8 = RegExp(
      r"filename\*=UTF-8''([^;]+)",
      caseSensitive: false,
    ).firstMatch(disposition);
    if (utf8 != null && utf8.groupCount >= 1) {
      return Uri.decodeComponent(utf8.group(1)!.trim());
    }

    final plain = RegExp(
      r'filename="?([^";]+)"?',
      caseSensitive: false,
    ).firstMatch(disposition);
    if (plain != null && plain.groupCount >= 1) {
      return plain.group(1)!.trim();
    }

    return null;
  }

  bool _isMediaContentType(String? contentType) {
    final lower = (contentType ?? '').toLowerCase();
    return lower.startsWith('image/') || lower.startsWith('video/');
  }

  String _inferContentTypeFromFileName(String? fileName) {
    final lower = (fileName ?? '').toLowerCase();
    if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) return 'image/jpeg';
    if (lower.endsWith('.png')) return 'image/png';
    if (lower.endsWith('.gif')) return 'image/gif';
    if (lower.endsWith('.webp')) return 'image/webp';
    if (lower.endsWith('.bmp')) return 'image/bmp';
    if (lower.endsWith('.mp4')) return 'video/mp4';
    if (lower.endsWith('.mov')) return 'video/quicktime';
    if (lower.endsWith('.webm')) return 'video/webm';
    return 'application/octet-stream';
  }

  String _formatTime(DateTime time) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final msgDate = DateTime(time.year, time.month, time.day);

    if (msgDate == today) {
      return '${time.hour}:${time.minute.toString().padLeft(2, '0')}';
    } else {
      return '${time.day}/${time.month} ${time.hour}:${time.minute.toString().padLeft(2, '0')}';
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_currentUserId == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Chat')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.peerName),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.videocam),
            tooltip: 'Start video call',
            onPressed: () async {
              final currentUser = Provider.of<UserProvider>(
                context,
                listen: false,
              ).user;

              if (currentUser == null || _currentUserId == null) {
                if (!mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('User not logged in')),
                );
                return;
              }

              final canCall = await ApiService.canInitiateVideoCall(
                currentUserId: _currentUserId!,
                currentUserRole: currentUser.role,
                caregiverId: currentUser.caregiverId,
                targetUserId: widget.peerUserId,
              );

              if (!mounted) return;

              if (!canCall) {
                final message = currentUser.role == 'PATIENT'
                    ? 'You can only call your assigned caregivers.'
                    : 'You can only call assigned patients or caregivers in your assigned patients\' circles.';
                ScaffoldMessenger.of(
                  context,
                ).showSnackBar(SnackBar(content: Text(message)));
                return;
              }

              final callId =
                  'chime_call_${DateTime.now().millisecondsSinceEpoch}';
              context.push(
                '/video-call-chime'
                '?userId=$_currentUserId'
                '&recipientId=${widget.peerUserId}'
                '&userRole=${Uri.encodeComponent(currentUser.role)}'
                '&userName=${Uri.encodeComponent(currentUser.name ?? 'User')}'
                '&recipientName=${Uri.encodeComponent(widget.peerName)}'
                '&initiator=true'
                '&video=true'
                '&audio=true'
                '&callId=$callId',
              );
            },
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: isLoading
                ? const Center(child: CircularProgressIndicator())
                : ListView.builder(
                    reverse: true,
                    itemCount: messages.length,
                    itemBuilder: (context, index) {
                      final reversedIndex = messages.length - 1 - index;
                      return buildMessageBubble(messages[reversedIndex]);
                    },
                  ),
          ),
          const Divider(height: 1),
          if (_selectedAttachment != null)
            Container(
              width: double.infinity,
              margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.grey.shade100,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Row(
                children: [
                  Icon(
                    _attachmentIconFromExtension(
                      _selectedAttachment!.extension,
                    ),
                    size: 18,
                    color: Colors.blueGrey.shade700,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      _selectedAttachment!.name,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close),
                    tooltip: 'Remove attachment',
                    onPressed: _isUploadingAttachment
                        ? null
                        : () => setState(() => _selectedAttachment = null),
                  ),
                ],
              ),
            ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.attach_file),
                  tooltip: 'Attach video, voice note, or document',
                  onPressed: _isUploadingAttachment ? null : _pickAttachment,
                ),
                Expanded(
                  child: TextField(
                    controller: _controller,
                    decoration: const InputDecoration(
                      hintText: 'Type a message...',
                      border: OutlineInputBorder(),
                      contentPadding: EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 8,
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                _isUploadingAttachment
                    ? const SizedBox(
                        width: 24,
                        height: 24,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : IconButton(
                        icon: const Icon(Icons.send),
                        onPressed: sendMessage,
                      ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
