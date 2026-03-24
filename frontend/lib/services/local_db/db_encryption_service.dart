import 'dart:convert';
import 'dart:math';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Handles encryption key lifecycle for the local SQLCipher database.
///
/// Responsibilities:
/// - Retrieve existing encryption key from secure storage
/// - Generate and persist a new key if none exists
/// - Provide utilities for safe key usage in SQLCipher
/// - Support secure key deletion for reset scenarios
///
/// Security Notes:
/// - Encryption keys are stored in platform secure storage
/// - Keys are never logged or exposed
/// - All operations avoid leaking sensitive values
class DbEncryptionService {
  DbEncryptionService({FlutterSecureStorage? storage})
      : _storage = storage ??
            const FlutterSecureStorage(webOptions: WebOptions.defaultOptions);

  static const String _encryptionKeyStorageKey = 'careconnect_db_key_v1';
  final FlutterSecureStorage _storage;

  /// Returns an existing encryption key or creates and stores a new one.
  Future<String> getOrCreateEncryptionKey() async {
    final existing = await _storage.read(key: _encryptionKeyStorageKey);

    if (existing != null && existing.isNotEmpty) {
      assert(() {
        print('[DbEncryption] Existing encryption key found');
        return true;
      }());
      return existing;
    }

    assert(() {
      print('[DbEncryption] No key found, generating new encryption key');
      return true;
    }());

    final random = Random.secure();
    final bytes = List<int>.generate(32, (_) => random.nextInt(256));
    final generated = base64UrlEncode(bytes);

    await _storage.write(key: _encryptionKeyStorageKey, value: generated);

    assert(() {
      print('[DbEncryption] New encryption key generated and stored');
      return true;
    }());

    return generated;
  }

  /// Returns true when an encryption key has already been stored.
  Future<bool> hasEncryptionKey() async {
    final existing = await _storage.read(key: _encryptionKeyStorageKey);

    final hasKey = existing != null && existing.isNotEmpty;

    assert(() {
      print('[DbEncryption] Key exists: $hasKey');
      return true;
    }());

    return hasKey;
  }

  /// Escapes single quotes for safe use in PRAGMA statements.
  ///
  /// NOTE:
  /// This method does not log or expose key contents.
  String escapeForPragma(String rawKey) {
    return rawKey.replaceAll("'", "''");
  }

  /// Deletes the stored encryption key.
  ///
  /// Useful for factory-reset flows when the encrypted database is unreadable
  /// and needs to be recreated from scratch.
  Future<void> deleteKey() async {
    assert(() {
      print('[DbEncryption] Deleting stored encryption key');
      return true;
    }());

    await _storage.delete(key: _encryptionKeyStorageKey);
  }
}
