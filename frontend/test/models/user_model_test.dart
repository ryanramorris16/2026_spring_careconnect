// Tests for UserModel (lib/models/user_model.dart).
// Pure Dart class with constructor, toJson, and fromJson.

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/models/user_model.dart';

void main() {
  group('UserModel constructor', () {
    test('stores all fields', () {
      final user = UserModel(
        name: 'Alice Smith',
        email: 'alice@example.com',
        userId: 'u-123',
        role: 'CAREGIVER',
      );
      expect(user.name, 'Alice Smith');
      expect(user.email, 'alice@example.com');
      expect(user.userId, 'u-123');
      expect(user.role, 'CAREGIVER');
    });
  });

  group('UserModel.toJson', () {
    test('returns correct map', () {
      final user = UserModel(
        name: 'Bob',
        email: 'bob@test.com',
        userId: 'u-456',
        role: 'PATIENT',
      );
      final json = user.toJson();
      expect(json['name'], 'Bob');
      expect(json['email'], 'bob@test.com');
      expect(json['userId'], 'u-456');
      expect(json['role'], 'PATIENT');
    });
  });

  group('UserModel.fromJson', () {
    test('parses complete JSON', () {
      final user = UserModel.fromJson({
        'name': 'Carol',
        'email': 'carol@test.com',
        'userId': 'u-789',
        'role': 'ADMIN',
      });
      expect(user.name, 'Carol');
      expect(user.email, 'carol@test.com');
      expect(user.userId, 'u-789');
      expect(user.role, 'ADMIN');
    });

    test('uses empty strings for missing fields', () {
      final user = UserModel.fromJson({});
      expect(user.name, '');
      expect(user.email, '');
      expect(user.userId, '');
      expect(user.role, '');
    });

    test('round-trips through toJson', () {
      final original = UserModel(
        name: 'Dave',
        email: 'dave@test.com',
        userId: 'u-999',
        role: 'FAMILY_MEMBER',
      );
      final copy = UserModel.fromJson(original.toJson());
      expect(copy.name, original.name);
      expect(copy.email, original.email);
      expect(copy.userId, original.userId);
      expect(copy.role, original.role);
    });
  });
}
