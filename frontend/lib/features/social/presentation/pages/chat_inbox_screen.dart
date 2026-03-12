import 'package:care_connect_app/services/api_service.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../../providers/user_provider.dart';
import '../model/conversation_preview_dto.dart';
import 'chat_room_screen.dart';
import 'my_friend_screen.dart';

class ChatInboxScreen extends StatefulWidget {
  const ChatInboxScreen({super.key});

  @override
  State<ChatInboxScreen> createState() => _ChatInboxScreenState();
}

class _ChatInboxScreenState extends State<ChatInboxScreen> {
  int? _userId;
  List<ConversationPreviewDto> inbox = [];
  bool isLoading = true;
  bool _initialized = false;

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

      _userId = user.id;
      fetchInbox();
      _initialized = true;
    }
  }

  Future<void> fetchInbox() async {
    setState(() => isLoading = true);
    try {
      final data = await ApiService.getInbox(_userId!);
      setState(() {
        inbox = (data)
            .map((json) => ConversationPreviewDto.fromJson(json))
            .toList();
        isLoading = false;
      });
    } catch (e) {
      setState(() => isLoading = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Failed to load inbox: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_userId == null) {
      return Scaffold(
        // appBar: AppBar(title: const Text('Messages')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Messages'),
        actions: [
          IconButton(
            icon: const Icon(Icons.group),
            tooltip: 'My Friends',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => MyFriendsScreen()),
              );
            },
          ),
        ],
      ),
      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : inbox.isEmpty
          ? const Center(child: Text('No messages yet'))
          : ListView.builder(
              itemCount: inbox.length,
              itemBuilder: (context, index) {
                final convo = inbox[index];
                final previewText = convo.content.trim().isEmpty
                    ? '(no message text)'
                    : convo.content;
                return ListTile(
                  title: Text(convo.peerName),
                  subtitle: Row(
                    children: [
                      if (convo.isPending)
                        Container(
                          margin: const EdgeInsets.only(right: 8),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 6,
                            vertical: 2,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.orange.shade100,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: const Text(
                            'Pending',
                            style: TextStyle(
                              fontSize: 11,
                              color: Colors.orange,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      Expanded(
                        child: Text(
                          previewText,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  trailing: Text(
                    convo.timestamp.toIso8601String().split('T').join(' • '),
                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => ChatRoomScreen(
                          peerUserId: convo.peerId,
                          peerName: convo.peerName,
                        ),
                      ),
                    ).then((_) async {
                      if (!mounted) return;
                      await fetchInbox();
                    });
                  },
                );
              },
            ),
    );
  }
}
