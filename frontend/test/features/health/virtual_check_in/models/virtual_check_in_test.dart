// Tests for VirtualCheckIn, CheckInType, CheckInStatus
// (lib/features/health/virtual_check_in/models/virtual_check_in.dart).

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/health/virtual_check_in/models/virtual_check_in.dart';

VirtualCheckIn _basic() => VirtualCheckIn(
      id: 'vc-1',
      type: CheckInType.routine,
      clinicianName: 'Dr. Smith',
      startedAt: DateTime(2025, 12, 4, 10, 30),
      durationMinutes: 15,
      status: CheckInStatus.completed,
      moodLabel: 'Good',
      nextCheckIn: DateTime(2025, 12, 11),
      summary: 'All vitals normal.',
    );

void main() {
  group('CheckInType enum', () {
    test('has routine, followUp, urgent', () {
      expect(CheckInType.values,
          containsAll([CheckInType.routine, CheckInType.followUp, CheckInType.urgent]));
    });
  });

  group('CheckInStatus enum', () {
    test('has completed, missed, cancelled', () {
      expect(CheckInStatus.values,
          containsAll([CheckInStatus.completed, CheckInStatus.missed, CheckInStatus.cancelled]));
    });
  });

  group('VirtualCheckIn constructor', () {
    test('stores all required fields', () {
      final vc = _basic();
      expect(vc.id, 'vc-1');
      expect(vc.type, CheckInType.routine);
      expect(vc.clinicianName, 'Dr. Smith');
      expect(vc.durationMinutes, 15);
      expect(vc.status, CheckInStatus.completed);
      expect(vc.moodLabel, 'Good');
      expect(vc.summary, 'All vitals normal.');
    });

    test('stores followUp type', () {
      final vc = VirtualCheckIn(
        id: 'vc-2',
        type: CheckInType.followUp,
        clinicianName: 'Dr. Jones',
        startedAt: DateTime(2025, 12, 5),
        durationMinutes: 10,
        status: CheckInStatus.missed,
        moodLabel: 'Fair',
        nextCheckIn: DateTime(2025, 12, 12),
        summary: 'Follow-up missed.',
      );
      expect(vc.type, CheckInType.followUp);
      expect(vc.status, CheckInStatus.missed);
    });
  });
}
