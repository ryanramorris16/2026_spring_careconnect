typedef IsOnlineFn = Future<bool> Function();

/// Routes storage operations to online or offline handlers.
///
/// Connectivity detection is injected from outside this class.
class ConnectivityRouterService {
  ConnectivityRouterService({required IsOnlineFn isOnline}) : _isOnline = isOnline;

  final IsOnlineFn _isOnline;

  /// Executes [online] when connected, otherwise executes [offline].
  Future<T> route<T>({
    required Future<T> Function() online,
    required Future<T> Function() offline,
  }) async {
    final isOnline = await _isOnline();
    if (isOnline) {
      return online();
    }
    return offline();
  }
}
