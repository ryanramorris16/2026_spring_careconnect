class ConversationPreviewDto {
  final int peerId;
  final String peerName;
  final String content; // last message
  final DateTime timestamp;

  ConversationPreviewDto({
    required this.peerId,
    required this.peerName,
    required this.content,
    required this.timestamp,
  });

  factory ConversationPreviewDto.fromJson(Map<String, dynamic> json) {
    final rawPeerName = json['peerName']?.toString().trim() ?? '';
    final rawPeerEmail = json['peerEmail']?.toString().trim() ?? '';
    final rawContent =
        json['content'] ?? json['message'] ?? json['text'] ?? json['body'];

    final resolvedPeerName = rawPeerName.isNotEmpty
        ? rawPeerName
        : (rawPeerEmail.isNotEmpty ? rawPeerEmail.split('@').first : 'Unknown');

    return ConversationPreviewDto(
      peerId: json['peerId'] is int
          ? json['peerId'] as int
          : int.tryParse(json['peerId']?.toString() ?? '') ?? 0,
      peerName: resolvedPeerName,
      content: rawContent?.toString() ?? '',
      timestamp:
          DateTime.tryParse(json['timestamp']?.toString() ?? '') ??
          DateTime.now(),
    );
  }
}
