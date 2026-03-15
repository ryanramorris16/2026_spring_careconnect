import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:path_provider_platform_interface/path_provider_platform_interface.dart';

class LocalDbTestBindings {
  static bool _installed = false;
  static PathProviderPlatform? _previousPathProvider;
  static late Directory _documentsDir;

  static final Map<String, String> _secureStore = <String, String>{};

  static const MethodChannel _secureStorageChannel =
      MethodChannel('plugins.it_nomads.com/flutter_secure_storage');

  static Future<void> install() async {
    if (_installed) return;

    TestWidgetsFlutterBinding.ensureInitialized();

    _documentsDir =
        await Directory.systemTemp.createTemp('careconnect_local_db_test_');

    _previousPathProvider = PathProviderPlatform.instance;
    PathProviderPlatform.instance =
        _FakePathProviderPlatform(_documentsDir.path);

    final messenger =
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

    // IMPORTANT: In many Flutter versions, this returns void (do NOT await).
    messenger.setMockMethodCallHandler(
      _secureStorageChannel,
      (MethodCall call) async {
        final args = (call.arguments is Map)
            ? Map<dynamic, dynamic>.from(call.arguments as Map)
            : <dynamic, dynamic>{};

        switch (call.method) {
          case 'read':
            final key = args['key']?.toString();
            return key == null ? null : _secureStore[key];

          case 'write':
            final key = args['key']?.toString();
            final value = args['value']?.toString() ?? '';
            if (key != null) {
              _secureStore[key] = value;
            }
            return null;

          case 'delete':
            final key = args['key']?.toString();
            if (key != null) {
              _secureStore.remove(key);
            }
            return null;

          case 'deleteAll':
            _secureStore.clear();
            return null;

          case 'containsKey':
            final key = args['key']?.toString();
            return key != null && _secureStore.containsKey(key);

          case 'readAll':
            return Map<String, String>.from(_secureStore);

          default:
            return null;
        }
      },
    );

    _installed = true;
  }

  static Future<void> reset() async {
    _secureStore.clear();

    final dbFile = File(dbPath);
    if (await dbFile.exists()) {
      await dbFile.delete();
    }
  }

  static Future<void> uninstall() async {
    if (!_installed) return;

    final messenger =
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

    // IMPORTANT: returns void in many versions (do NOT await).
    messenger.setMockMethodCallHandler(_secureStorageChannel, null);

    if (_previousPathProvider != null) {
      PathProviderPlatform.instance = _previousPathProvider!;
    }

    if (await _documentsDir.exists()) {
      await _documentsDir.delete(recursive: true);
    }

    _secureStore.clear();
    _installed = false;
  }

  static String get dbPath =>
      '${_documentsDir.path}${Platform.pathSeparator}careconnect_mobile.sqlite';
}

class _FakePathProviderPlatform extends PathProviderPlatform {
  _FakePathProviderPlatform(this._path);

  final String _path;

  @override
  Future<String?> getApplicationDocumentsPath() async => _path;

  @override
  Future<String?> getTemporaryPath() async => _path;
}
