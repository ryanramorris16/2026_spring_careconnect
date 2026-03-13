// Tests for PatientUserModel (lib/models/patient_model.dart).
// Pure Dart class with constructor, toJson, fromJson, toString.
// Note: uses Address from lib/features/dashboard/models/patient_model.dart.

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/models/patient_model.dart';
import 'package:care_connect_app/features/dashboard/models/patient_model.dart';

Address _addr() => Address(
  line1: '50 Oak St',
  line2: 'Floor 2',
  city: 'Naperville',
  state: 'IL',
  zip: '60540',
);

PatientUserModel _patient() => PatientUserModel(
  name: 'Pat Tient',
  email: 'pat@health.com',
  userId: 'pt-001',
  role: 'PATIENT',
  firstName: 'Pat',
  lastName: 'Tient',
  phone: '630-555-0001',
  dob: '1990-07-04',
  gender: 'Non-binary',
  address: _addr(),
);

void main() {
  group('PatientUserModel constructor', () {
    test('stores all fields', () {
      final p = _patient();
      expect(p.name, 'Pat Tient');
      expect(p.email, 'pat@health.com');
      expect(p.userId, 'pt-001');
      expect(p.role, 'PATIENT');
      expect(p.firstName, 'Pat');
      expect(p.lastName, 'Tient');
      expect(p.phone, '630-555-0001');
      expect(p.dob, '1990-07-04');
      expect(p.gender, 'Non-binary');
    });
  });

  group('PatientUserModel.toJson', () {
    test('includes base UserModel fields', () {
      final json = _patient().toJson();
      expect(json['name'], 'Pat Tient');
      expect(json['email'], 'pat@health.com');
      expect(json['userId'], 'pt-001');
    });

    test('role is always PATIENT', () {
      // toJson explicitly sets role: PATIENT
      final json = _patient().toJson();
      expect(json['role'], 'PATIENT');
    });

    test('includes patient-specific fields', () {
      final json = _patient().toJson();
      expect(json['firstName'], 'Pat');
      expect(json['lastName'], 'Tient');
      expect(json['phone'], '630-555-0001');
      expect(json['dob'], '1990-07-04');
      expect(json['gender'], 'Non-binary');
      expect(json['address'], isA<Map>());
    });
  });

  group('PatientUserModel.fromJson', () {
    test('parses complete JSON', () {
      final p = PatientUserModel.fromJson({
        'name': 'Sam Patient',
        'email': 'sam@care.com',
        'userId': 'pt-002',
        'role': 'PATIENT',
        'firstName': 'Sam',
        'lastName': 'Patient',
        'phone': '312-555-0002',
        'dob': '1985-11-20',
        'gender': 'Male',
        'address': {
          'line1': '1 River Rd',
          'line2': '',
          'city': 'Joliet',
          'state': 'IL',
          'zip': '60432',
        },
      });
      expect(p.firstName, 'Sam');
      expect(p.lastName, 'Patient');
      expect(p.dob, '1985-11-20');
      expect(p.gender, 'Male');
    });

    test('uses empty defaults for missing fields', () {
      final p = PatientUserModel.fromJson({});
      expect(p.name, '');
      expect(p.email, '');
      expect(p.firstName, '');
      expect(p.role, 'PATIENT');
    });

    test('round-trips through toJson', () {
      final original = _patient();
      final copy = PatientUserModel.fromJson(original.toJson());
      expect(copy.firstName, original.firstName);
      expect(copy.lastName, original.lastName);
      expect(copy.email, original.email);
      expect(copy.role, 'PATIENT');
    });
  });

  group('PatientUserModel.toString', () {
    test('contains firstName and lastName', () {
      final s = _patient().toString();
      expect(s.contains('Pat'), isTrue);
      expect(s.contains('Tient'), isTrue);
    });
  });
}
