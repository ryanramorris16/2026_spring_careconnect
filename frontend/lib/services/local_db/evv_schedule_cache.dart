import 'app_database_stub.dart' if (dart.library.io) 'app_database.dart';

/// Lightweight singleton that caches EVV schedule API responses in the same
/// encrypted SQLite database used by [OfflineSyncService].  When the device
/// has no connectivity the [SchedulePage] falls back to this cached data so
/// caregivers can still view their assigned visits.
class EvvScheduleCache {
  EvvScheduleCache._();

  static final EvvScheduleCache instance = EvvScheduleCache._();

  /// Schedules cached within the last [_maxAge] are considered "fresh enough"
  /// to serve without a network refresh.  After this window they are still
  /// served when offline, but a banner warns the caregiver that the data may
  /// be outdated.
  static const Duration _maxAge = Duration(hours: 8);

  final AppDatabase _db = AppDatabase();

  /// Persists [dataJson] (raw JSON from the API) for [caregiverId].
  Future<void> save(int caregiverId, String dataJson) async {
    await _db.saveEvvSchedules(caregiverId: caregiverId, dataJson: dataJson);
  }

  /// Returns the cached JSON for [caregiverId] together with a [isStale] flag.
  ///
  /// * `data == null` → nothing cached yet.
  /// * `isStale == true` → cached data is older than [_maxAge].
  Future<({String? data, bool isStale})> load(int caregiverId) async {
    final result = await _db.loadEvvSchedules(caregiverId);
    if (result.data == null) return (data: null, isStale: false);

    final age = result.cachedAt != null
        ? DateTime.now().toUtc().difference(result.cachedAt!)
        : _maxAge + const Duration(seconds: 1); // treat unknown age as stale

    return (data: result.data, isStale: age > _maxAge);
  }
}
