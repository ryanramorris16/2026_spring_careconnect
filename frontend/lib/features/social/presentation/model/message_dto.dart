class MessageDto {
  final int id;
  final int senderId;
  final int receiverId;
  final String content;
  final DateTime timestamp;
  final bool isRead;
  final int? attachmentFileId;
  final String? attachmentUrl;
  final String? attachmentName;
  final String? attachmentContentType;
  final int? attachmentSize;

  MessageDto({
    required this.id,
    required this.senderId,
    required this.receiverId,
    required this.content,
    required this.timestamp,
    this.isRead = false,
    this.attachmentFileId,
    this.attachmentUrl,
    this.attachmentName,
    this.attachmentContentType,
    this.attachmentSize,
  });

  factory MessageDto.fromJson(Map<String, dynamic> json) {
    final attachmentRaw = json['attachment'];
    final attachment = attachmentRaw is Map<String, dynamic>
        ? attachmentRaw
        : (attachmentRaw is Map ? Map<String, dynamic>.from(attachmentRaw) : null);

    int? toNullableInt(dynamic value) {
      if (value is int) return value;
      if (value is num) return value.toInt();
      return int.tryParse(value?.toString() ?? '');
    }

    return MessageDto(
      id: toNullableInt(json['id']) ?? 0,
      senderId: toNullableInt(json['senderId']) ?? 0,
      receiverId: toNullableInt(json['receiverId']) ?? 0,
      content: json['content']?.toString() ?? '',
      timestamp:
          DateTime.tryParse(json['timestamp']?.toString() ?? '') ??
          DateTime.now(),
      isRead: json['isRead'] == true || json['read'] == true,
      attachmentFileId: toNullableInt(
        json['attachmentFileId'] ?? attachment?['fileId'] ?? attachment?['id'],
      ),
      attachmentUrl:
          json['attachmentUrl']?.toString() ??
          attachment?['url']?.toString() ??
          attachment?['fileUrl']?.toString(),
      attachmentName:
          json['attachmentName']?.toString() ??
          attachment?['name']?.toString() ??
          attachment?['fileName']?.toString(),
      attachmentContentType:
          json['attachmentContentType']?.toString() ??
          attachment?['contentType']?.toString(),
      attachmentSize: toNullableInt(
        json['attachmentSize'] ?? attachment?['size'] ?? attachment?['fileSize'],
      ),
    );
  }
}