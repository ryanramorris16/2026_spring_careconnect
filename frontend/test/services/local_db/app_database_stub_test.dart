// Tests for AppDatabase stub (lib/services/local_db/app_database_stub.dart).
// Web-safe in-memory implementation — pure async Dart, no native plugins.
// Tests cover Mood model, insertMood, getMoodsForUser, getMoodsForUserOldestFirst,
// getMoodByIdForUser, deleteMoodById, and deleteMoodsByIds.

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/services/local_db/app_database_stub.dart';

void main() {
  late AppDatabase db;

  setUp(() {
    db = AppDatabase();
  });

  group('Mood constructor', () {
    test('stores all fields', () {
      final dt = DateTime(2025, 1, 1);
      final mood = Mood(id: 1, userId: 10, score: 7, label: 'Good', createdAt: dt);
      expect(mood.id, 1);
      expect(mood.userId, 10);
      expect(mood.score, 7);
      expect(mood.label, 'Good');
      expect(mood.createdAt, dt);
    });
  });

  group('AppDatabase.isEncrypted', () {
    test('always returns false', () async {
      expect(await db.isEncrypted(), isFalse);
    });
  });

  group('AppDatabase.insertMood', () {
    test('returns incremental IDs starting at 1', () async {
      final id1 = await db.insertMood(userId: 1, score: 5, label: 'OK');
      final id2 = await db.insertMood(userId: 1, score: 8, label: 'Good');
      expect(id1, 1);
      expect(id2, 2);
    });

    test('stores mood with provided createdAt', () async {
      final dt = DateTime(2025, 3, 10);
      await db.insertMood(userId: 5, score: 6, label: 'Fine', createdAt: dt);
      final moods = await db.getMoodsForUser(5);
      expect(moods.first.createdAt, dt);
    });

    test('uses current time when createdAt is null', () async {
      final before = DateTime.now().subtract(const Duration(seconds: 1));
      await db.insertMood(userId: 2, score: 4, label: 'Meh');
      final moods = await db.getMoodsForUser(2);
      expect(moods.first.createdAt.isAfter(before), isTrue);
    });
  });

  group('AppDatabase.getMoodsForUser', () {
    test('returns empty list for user with no moods', () async {
      final moods = await db.getMoodsForUser(99);
      expect(moods, isEmpty);
    });

    test('returns only moods belonging to given userId', () async {
      await db.insertMood(userId: 1, score: 7, label: 'A');
      await db.insertMood(userId: 2, score: 5, label: 'B');
      await db.insertMood(userId: 1, score: 8, label: 'C');

      final user1Moods = await db.getMoodsForUser(1);
      expect(user1Moods.length, 2);
      expect(user1Moods.every((m) => m.userId == 1), isTrue);
    });
  });

  group('AppDatabase.getMoodsForUserOldestFirst', () {
    test('returns moods in ascending createdAt order', () async {
      final dt1 = DateTime(2025, 1, 3);
      final dt2 = DateTime(2025, 1, 1);
      final dt3 = DateTime(2025, 1, 2);
      await db.insertMood(userId: 3, score: 5, label: 'C', createdAt: dt1);
      await db.insertMood(userId: 3, score: 7, label: 'A', createdAt: dt2);
      await db.insertMood(userId: 3, score: 6, label: 'B', createdAt: dt3);

      final moods = await db.getMoodsForUserOldestFirst(3);
      expect(moods[0].createdAt, dt2);
      expect(moods[1].createdAt, dt3);
      expect(moods[2].createdAt, dt1);
    });

    test('returns empty list when no moods for user', () async {
      final moods = await db.getMoodsForUserOldestFirst(777);
      expect(moods, isEmpty);
    });
  });

  group('AppDatabase.getMoodByIdForUser', () {
    test('returns the correct mood when found', () async {
      await db.insertMood(userId: 4, score: 9, label: 'Great');
      final id = await db.insertMood(userId: 4, score: 3, label: 'Bad');

      final mood = await db.getMoodByIdForUser(moodId: id, userIdValue: 4);
      expect(mood, isNotNull);
      expect(mood!.score, 3);
      expect(mood.label, 'Bad');
    });

    test('returns null for non-existent moodId', () async {
      final mood = await db.getMoodByIdForUser(moodId: 999, userIdValue: 1);
      expect(mood, isNull);
    });

    test('returns null when userId does not match', () async {
      final id = await db.insertMood(userId: 5, score: 6, label: 'X');
      final mood = await db.getMoodByIdForUser(moodId: id, userIdValue: 999);
      expect(mood, isNull);
    });
  });

  group('AppDatabase.deleteMoodById', () {
    test('removes the mood and returns 1', () async {
      final id = await db.insertMood(userId: 6, score: 5, label: 'Y');
      final count = await db.deleteMoodById(id);
      expect(count, 1);
      final moods = await db.getMoodsForUser(6);
      expect(moods, isEmpty);
    });

    test('returns 0 when mood does not exist', () async {
      final count = await db.deleteMoodById(9999);
      expect(count, 0);
    });
  });

  group('AppDatabase.deleteMoodsByIds', () {
    test('removes multiple moods by id', () async {
      final id1 = await db.insertMood(userId: 7, score: 5, label: 'P');
      final id2 = await db.insertMood(userId: 7, score: 7, label: 'Q');
      await db.insertMood(userId: 7, score: 9, label: 'R');

      await db.deleteMoodsByIds([id1, id2]);
      final moods = await db.getMoodsForUser(7);
      expect(moods.length, 1);
      expect(moods.first.label, 'R');
    });

    test('no-op for empty ids set', () async {
      await db.insertMood(userId: 8, score: 6, label: 'S');
      await db.deleteMoodsByIds([]);
      final moods = await db.getMoodsForUser(8);
      expect(moods.length, 1);
    });
  });

  group('AppDatabase.close', () {
    test('completes without error', () async {
      await expectLater(db.close(), completes);
    });
  });
}
