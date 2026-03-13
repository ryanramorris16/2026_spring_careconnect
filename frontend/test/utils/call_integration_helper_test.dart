// Tests for CallIntegrationHelper (lib/utils/call_integration_helper.dart).
//
// The static methods startVideoCallToPatient/Caregiver and sendSOSEmergencyAlert
// all require network calls, geolocator, or Provider<UserProvider>. Only the
// pure-widget factory methods are tested here:
//   - createPatientActionButtons → Row with 3 IconButtons (video/audio/sms)
//   - createCaregiverActionButtons → Row with 3 IconButtons (video/audio/sms)
//   - createSOSButton → ElevatedButton.icon labeled "SOS EMERGENCY"

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/utils/call_integration_helper.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/providers/user_provider.dart';

Widget _wrapWithProvider(Widget child) {
  return ChangeNotifierProvider<UserProvider>(
    create: (_) => UserProvider(),
    child: MaterialApp(home: Scaffold(body: child)),
  );
}

void main() {
  group('CallIntegrationHelper.createPatientActionButtons', () {
    testWidgets('renders video, audio, and sms icon buttons', (tester) async {
      await tester.pumpWidget(
        _wrapWithProvider(Builder(
          builder: (ctx) => CallIntegrationHelper.createPatientActionButtons(
            context: ctx,
            currentCaregiver: {'id': 1, 'name': 'CG'},
            targetPatient: {'id': 2, 'name': 'PT'},
          ),
        )),
      );
      expect(find.byIcon(Icons.videocam), findsOneWidget);
      expect(find.byIcon(Icons.call), findsOneWidget);
      expect(find.byIcon(Icons.sms), findsOneWidget);
    });
  });

  group('CallIntegrationHelper.createCaregiverActionButtons', () {
    testWidgets('renders video, audio, and sms icon buttons', (tester) async {
      await tester.pumpWidget(
        _wrapWithProvider(Builder(
          builder: (ctx) => CallIntegrationHelper.createCaregiverActionButtons(
            context: ctx,
            currentPatient: {'id': 10, 'name': 'Patient A'},
            targetCaregiver: {'id': 20, 'name': 'Caregiver B'},
          ),
        )),
      );
      expect(find.byIcon(Icons.videocam), findsOneWidget);
      expect(find.byIcon(Icons.call), findsOneWidget);
      expect(find.byIcon(Icons.sms), findsOneWidget);
    });
  });

  group('CallIntegrationHelper.createSOSButton', () {
    testWidgets('renders SOS EMERGENCY button', (tester) async {
      await tester.pumpWidget(
        _wrapWithProvider(Builder(
          builder: (ctx) => CallIntegrationHelper.createSOSButton(
            context: ctx,
            currentPatient: {'id': 1, 'name': 'Patient'},
          ),
        )),
      );
      expect(find.text('SOS EMERGENCY'), findsOneWidget);
      expect(find.byIcon(Icons.emergency), findsOneWidget);
    });

    testWidgets('tapping SOS button shows dialog with emergency types',
        (tester) async {
      await tester.pumpWidget(
        _wrapWithProvider(Builder(
          builder: (ctx) => CallIntegrationHelper.createSOSButton(
            context: ctx,
            currentPatient: {'id': 1, 'name': 'Patient'},
          ),
        )),
      );
      await tester.tap(find.text('SOS EMERGENCY'));
      await tester.pumpAndSettle();
      expect(find.text('🚨 SOS Emergency'), findsOneWidget);
      expect(find.text('Fall Emergency'), findsOneWidget);
      expect(find.text('Medical Emergency'), findsOneWidget);
    });
  });
}
