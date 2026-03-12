import 'dart:convert';

import 'package:care_connect_app/features/dashboard/patient_dashboard/models/medication_reminder_item.dart';
import 'package:care_connect_app/services/api_service.dart';

class PatientMedicationReminderService {
  final Map<int, DateTime> _takenUntilByMedicationId = <int, DateTime>{};

  Future<List<MedicationReminderItem>> loadReminders({
    required int? patientId,
  }) async {
    if (patientId == null) {
      return const <MedicationReminderItem>[];
    }

    final response = await ApiService.getPatientMedicationsForPatient(patientId);
    if (response.statusCode != 200) {
      return const <MedicationReminderItem>[];
    }

    final decoded = jsonDecode(response.body);
    if (decoded is! List) {
      return const <MedicationReminderItem>[];
    }

    final now = DateTime.now();
    final List<MedicationReminderItem> reminders = <MedicationReminderItem>[];
    final activeMedicationIds = <int>{};
    var syntheticId = -1;

    for (final row in decoded) {
      if (row is! Map) {
        continue;
      }

      final isActive = row['isActive'] != false;
      if (!isActive) {
        continue;
      }

      final name = (row['medicationName'] ?? '').toString().trim();
      if (name.isEmpty) {
        continue;
      }

      final medicationId = _parseInt(row['id']) ?? syntheticId--;
      activeMedicationIds.add(medicationId);

      final dosage = (row['dosage'] ?? 'Dose not set').toString();
      final frequency = (row['frequency'] ?? 'Once daily').toString();
      final startDate = DateTime.tryParse((row['startDate'] ?? '').toString());
      final takenUntil = _takenUntilByMedicationId[medicationId];
      final isTakenForWindow = takenUntil != null && takenUntil.isAfter(now);
      final nextDueAt = isTakenForWindow
          ? takenUntil
          : (startDate != null && startDate.isAfter(now) ? startDate : now);

      reminders.add(
        MedicationReminderItem(
          medicationId: medicationId,
          medicationName: name,
          dosage: dosage,
          frequency: frequency,
          nextDueAt: nextDueAt,
          isTakenForCurrentWindow: isTakenForWindow,
        ),
      );
    }

    reminders.sort((a, b) {
      if (a.isTakenForCurrentWindow != b.isTakenForCurrentWindow) {
        return a.isTakenForCurrentWindow ? 1 : -1;
      }
      return a.nextDueAt.compareTo(b.nextDueAt);
    });

    _takenUntilByMedicationId.removeWhere(
      (medicationId, _) => !activeMedicationIds.contains(medicationId),
    );

    return reminders;
  }

  void markTaken({
    required int medicationId,
    required String frequency,
  }) {
    _takenUntilByMedicationId[medicationId] = DateTime.now().add(
      _frequencyInterval(frequency),
    );
  }

  void markMissed({required int medicationId}) {
    _takenUntilByMedicationId.remove(medicationId);
  }

  bool hasPendingUntaken(List<MedicationReminderItem> reminders) {
    return reminders.any((item) => !item.isTakenForCurrentWindow);
  }

  int? _parseInt(dynamic value) {
    if (value is int) {
      return value;
    }
    if (value is num) {
      return value.toInt();
    }
    if (value is String) {
      return int.tryParse(value);
    }
    return null;
  }

  Duration _frequencyInterval(String frequency) {
    final text = frequency.toLowerCase();

    if (text.contains('twice') || text.contains('2x') || text.contains('two')) {
      return const Duration(hours: 12);
    }
    if (text.contains('three') || text.contains('3x')) {
      return const Duration(hours: 8);
    }
    if (text.contains('every') && text.contains('hour')) {
      final match = RegExp(r'(\d+)').firstMatch(text);
      final hours = match == null ? 1 : int.tryParse(match.group(1)!) ?? 1;
      return Duration(hours: hours.clamp(1, 24));
    }
    if (text.contains('week')) {
      return const Duration(days: 7);
    }
    if (text.contains('month')) {
      return const Duration(days: 30);
    }
    if (text.contains('day') || text.contains('daily')) {
      return const Duration(days: 1);
    }

    return const Duration(days: 1);
  }
}
