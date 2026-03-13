// Tests for dashboard Patient and Address models
// (lib/features/dashboard/models/patient_model.dart).

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/dashboard/models/patient_model.dart';

void main() {
  group('Address', () {
    test('constructor stores all fields', () {
      final address = Address(
        line1: '123 Main St',
        line2: 'Apt 4B',
        city: 'Springfield',
        state: 'IL',
        zip: '62701',
        phone: '(555) 123-4567',
      );
      expect(address.line1, '123 Main St');
      expect(address.line2, 'Apt 4B');
      expect(address.city, 'Springfield');
      expect(address.state, 'IL');
      expect(address.zip, '62701');
      expect(address.phone, '(555) 123-4567');
    });

    test('fromJson parses all fields', () {
      final address = Address.fromJson({
        'line1': '456 Oak Ave',
        'line2': 'Suite 2',
        'city': 'Chicago',
        'state': 'IL',
        'zip': '60601',
        'phone': '(555) 987-6543',
      });
      expect(address.line1, '456 Oak Ave');
      expect(address.city, 'Chicago');
      expect(address.zip, '60601');
    });

    test('fromJson allows null fields', () {
      final address = Address.fromJson({});
      expect(address.line1, isNull);
      expect(address.city, isNull);
    });

    test('toJson includes all fields', () {
      final address = Address(
        line1: '789 Pine Rd',
        city: 'Naperville',
        state: 'IL',
      );
      final json = address.toJson();
      expect(json['line1'], '789 Pine Rd');
      expect(json['city'], 'Naperville');
      expect(json['state'], 'IL');
    });
  });

  group('Patient.fromJson', () {
    test('parses simple flat structure', () {
      final patient = Patient.fromJson({
        'id': 10,
        'firstName': 'Alice',
        'lastName': 'Smith',
        'email': 'alice@example.com',
        'phone': '555-1234',
        'dob': '1990-01-15',
        'relationship': 'Mother',
        'gender': 'Female',
      });

      expect(patient.id, 10);
      expect(patient.firstName, 'Alice');
      expect(patient.lastName, 'Smith');
      expect(patient.email, 'alice@example.com');
      expect(patient.phone, '555-1234');
      expect(patient.dob, '1990-01-15');
      expect(patient.relationship, 'Mother');
      expect(patient.gender, 'Female');
    });

    test('parses nested patient structure', () {
      final patient = Patient.fromJson({
        'patient': {
          'id': 20,
          'firstName': 'Bob',
          'lastName': 'Jones',
          'email': 'bob@example.com',
          'phone': '555-5678',
          'dob': '1985-06-01',
          'relationship': 'Father',
        },
      });
      expect(patient.id, 20);
      expect(patient.firstName, 'Bob');
    });

    test('id as string is parsed to int', () {
      final patient = Patient.fromJson({
        'id': '42',
        'firstName': 'Carol',
        'lastName': 'White',
        'email': '',
        'phone': '',
        'dob': '',
        'relationship': '',
      });
      expect(patient.id, 42);
    });

    test('uses patientId when id is absent', () {
      final patient = Patient.fromJson({
        'patientId': 99,
        'firstName': 'Dave',
        'lastName': 'Brown',
        'email': '',
        'phone': '',
        'dob': '',
        'relationship': '',
      });
      expect(patient.id, 99);
    });

    test('id defaults to 0 when missing', () {
      final patient = Patient.fromJson({
        'firstName': 'Eve',
        'lastName': 'Green',
        'email': '',
        'phone': '',
        'dob': '',
        'relationship': '',
      });
      expect(patient.id, 0);
    });

    test('linkId and linkStatus from direct fields', () {
      final patient = Patient.fromJson({
        'id': 1,
        'firstName': 'Frank',
        'lastName': 'Lee',
        'email': '',
        'phone': '',
        'dob': '',
        'relationship': 'Sibling',
        'linkId': 55,
        'linkStatus': 'PENDING',
      });
      expect(patient.linkId, 55);
      expect(patient.linkStatus, 'PENDING');
    });

    test('linkId and linkStatus from link object', () {
      final patient = Patient.fromJson({
        'id': 2,
        'firstName': 'Grace',
        'lastName': 'Kim',
        'email': '',
        'phone': '',
        'dob': '',
        'relationship': '',
        'link': {
          'id': 77,
          'status': 'ACTIVE',
          'linkType': 'Friend',
        },
      });
      expect(patient.linkId, 77);
      expect(patient.linkStatus, 'ACTIVE');
    });

    test('relationship from link.linkType when not present in patient data', () {
      final patient = Patient.fromJson({
        'id': 3,
        'firstName': 'Henry',
        'lastName': 'Park',
        'email': '',
        'phone': '',
        'dob': '',
        'link': {
          'id': 88,
          'linkType': 'Caregiver',
        },
      });
      expect(patient.relationship, 'Caregiver');
    });

    test('parses address when present', () {
      final patient = Patient.fromJson({
        'id': 4,
        'firstName': 'Iris',
        'lastName': 'Nguyen',
        'email': '',
        'phone': '',
        'dob': '',
        'relationship': '',
        'address': {
          'line1': '321 Elm St',
          'city': 'Decatur',
          'state': 'IL',
        },
      });
      expect(patient.address, isNotNull);
      expect(patient.address?.line1, '321 Elm St');
      expect(patient.address?.city, 'Decatur');
    });

    test('address is null when not in json', () {
      final patient = Patient.fromJson({
        'id': 5,
        'firstName': 'Jake',
        'lastName': 'Wilson',
        'email': '',
        'phone': '',
        'dob': '',
        'relationship': '',
      });
      expect(patient.address, isNull);
    });

    test('linkStatus defaults to ACTIVE', () {
      final patient = Patient.fromJson({
        'id': 6,
        'firstName': 'Kate',
        'lastName': 'Adams',
        'email': '',
        'phone': '',
        'dob': '',
        'relationship': '',
      });
      expect(patient.linkStatus, 'ACTIVE');
    });

    test('parses maNumber', () {
      final patient = Patient.fromJson({
        'id': 7,
        'firstName': 'Leo',
        'lastName': 'Martinez',
        'email': '',
        'phone': '',
        'dob': '',
        'relationship': '',
        'maNumber': 'MA-123456',
      });
      expect(patient.maNumber, 'MA-123456');
    });
  });
}
