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
    final rawId = json['id'];
    final rawSenderId = json['senderId'];
    final rawReceiverId = json['receiverId'];
    final rawContent = json['content'] ?? json['message'] ?? json['text'];
    final rawAttachment = json['attachment'];
    final attachment = rawAttachment is Map<String, dynamic>
        ? rawAttachment
        : <String, dynamic>{};
    final rawAttachmentFileId =
        json['attachmentFileId'] ?? attachment['fileId'];
    final rawAttachmentSize = json['attachmentSize'] ?? attachment['size'];

    return MessageDto(
      id: rawId is int ? rawId : int.tryParse(rawId?.toString() ?? '') ?? 0,
      senderId: rawSenderId is int
          ? rawSenderId
          : int.tryParse(rawSenderId?.toString() ?? '') ?? 0,
      receiverId: rawReceiverId is int
          ? rawReceiverId
          : int.tryParse(rawReceiverId?.toString() ?? '') ?? 0,
      content: rawContent?.toString() ?? '',
      timestamp:
          DateTime.tryParse(json['timestamp']?.toString() ?? '') ??
          DateTime.now(),
      isRead: json['isRead'] as bool? ?? json['read'] as bool? ?? false,
      attachmentFileId: rawAttachmentFileId is int
          ? rawAttachmentFileId
          : int.tryParse(rawAttachmentFileId?.toString() ?? ''),
      attachmentUrl:
          json['attachmentUrl']?.toString() ?? attachment['url']?.toString(),
      attachmentName:
          json['attachmentName']?.toString() ?? attachment['name']?.toString(),
      attachmentContentType:
          json['attachmentContentType']?.toString() ??
          attachment['contentType']?.toString(),
      attachmentSize: rawAttachmentSize is int
          ? rawAttachmentSize
          : int.tryParse(rawAttachmentSize?.toString() ?? ''),
    );
  }
}
