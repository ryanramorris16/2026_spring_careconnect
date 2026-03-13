// Tests for CaregiverModel and ProfessionalInfo (lib/models/caregiver_model.dart).
// Pure Dart classes with constructor, toJson, fromJson.
// Note: CaregiverModel uses Address from lib/features/dashboard/models/patient_model.dart.

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/models/caregiver_model.dart';
import 'package:care_connect_app/features/dashboard/models/patient_model.dart';

Address _addr() => Address(
  line1: '1 Main St',
  line2: '',
  city: 'Chicago',
  state: 'IL',
  zip: '60601',
);

CaregiverModel _caregiver({ProfessionalInfo? professionalInfo}) =>
    CaregiverModel(
      name: 'Jane Doe',
      email: 'jane@care.com',
      userId: 'cg-001',
      role: 'CAREGIVER',
      firstName: 'Jane',
      lastName: 'Doe',
      phone: '555-1234',
      dob: '1985-03-15',
      gender: 'Female',
      caregiverType: 'Professional',
      address: _addr(),
      professionalInfo: professionalInfo,
    );

void main() {
  group('ProfessionalInfo constructor', () {
    test('stores all fields', () {
      final info = ProfessionalInfo(
        licenseNumber: 'LIC-999',
        issuingState: 'IL',
        yearsExperience: 10,
      );
      expect(info.licenseNumber, 'LIC-999');
      expect(info.issuingState, 'IL');
      expect(info.yearsExperience, 10);
    });
  });

  group('ProfessionalInfo.toJson', () {
    test('returns correct map', () {
      final info = ProfessionalInfo(
        licenseNumber: 'LIC-123',
        issuingState: 'CA',
        yearsExperience: 5,
      );
      final json = info.toJson();
      expect(json['licenseNumber'], 'LIC-123');
      expect(json['issuingState'], 'CA');
      expect(json['yearsExperience'], 5);
    });
  });

  group('ProfessionalInfo.fromJson', () {
    test('parses complete JSON', () {
      final info = ProfessionalInfo.fromJson({
        'licenseNumber': 'L-42',
        'issuingState': 'NY',
        'yearsExperience': 8,
      });
      expect(info.licenseNumber, 'L-42');
      expect(info.issuingState, 'NY');
      expect(info.yearsExperience, 8);
    });

    test('uses defaults for missing fields', () {
      final info = ProfessionalInfo.fromJson({});
      expect(info.licenseNumber, '');
      expect(info.issuingState, '');
      expect(info.yearsExperience, 0);
    });
  });

  group('CaregiverModel constructor', () {
    test('stores all fields', () {
      final cg = _caregiver();
      expect(cg.name, 'Jane Doe');
      expect(cg.email, 'jane@care.com');
      expect(cg.role, 'CAREGIVER');
      expect(cg.firstName, 'Jane');
      expect(cg.caregiverType, 'Professional');
      expect(cg.professionalInfo, isNull);
    });

    test('stores optional professional info', () {
      final info = ProfessionalInfo(
        licenseNumber: 'L-1',
        issuingState: 'TX',
        yearsExperience: 3,
      );
      final cg = _caregiver(professionalInfo: info);
      expect(cg.professionalInfo, isNotNull);
      expect(cg.professionalInfo!.licenseNumber, 'L-1');
    });
  });

  group('CaregiverModel.toJson', () {
    test('includes base UserModel fields', () {
      final json = _caregiver().toJson();
      expect(json['name'], 'Jane Doe');
      expect(json['email'], 'jane@care.com');
      expect(json['userId'], 'cg-001');
      expect(json['role'], 'CAREGIVER');
    });

    test('includes caregiver-specific fields', () {
      final json = _caregiver().toJson();
      expect(json['firstName'], 'Jane');
      expect(json['lastName'], 'Doe');
      expect(json['phone'], '555-1234');
      expect(json['dob'], '1985-03-15');
      expect(json['gender'], 'Female');
      expect(json['caregiverType'], 'Professional');
      expect(json['address'], isA<Map>());
    });

    test('omits professional key when no professional info', () {
      final json = _caregiver().toJson();
      expect(json.containsKey('professional'), isFalse);
    });

    test('includes professional key when professional info present', () {
      final info = ProfessionalInfo(
        licenseNumber: 'L-2',
        issuingState: 'FL',
        yearsExperience: 7,
      );
      final json = _caregiver(professionalInfo: info).toJson();
      expect(json.containsKey('professional'), isTrue);
      expect(json['professional']['licenseNumber'], 'L-2');
    });
  });

  group('CaregiverModel.fromJson', () {
    test('parses minimal JSON (no professional info)', () {
      final cg = CaregiverModel.fromJson({
        'name': 'Bob',
        'email': 'bob@test.com',
        'userId': 'cg-002',
        'firstName': 'Bob',
        'lastName': 'Smith',
        'phone': '555-9999',
        'dob': '1970-01-01',
        'gender': 'Male',
        'caregiverType': 'Family',
      });
      expect(cg.firstName, 'Bob');
      expect(cg.role, 'CAREGIVER');
      expect(cg.professionalInfo, isNull);
    });

    test('uses empty defaults for missing fields', () {
      final cg = CaregiverModel.fromJson({});
      expect(cg.name, '');
      expect(cg.email, '');
      expect(cg.firstName, '');
    });

    test('parses professional info when present', () {
      final cg = CaregiverModel.fromJson({
        'professional': {
          'licenseNumber': 'L-99',
          'issuingState': 'WA',
          'yearsExperience': 12,
        },
      });
      expect(cg.professionalInfo, isNotNull);
      expect(cg.professionalInfo!.licenseNumber, 'L-99');
    });
  });
}
