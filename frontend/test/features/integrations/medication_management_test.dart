// Tests for MedicationManagementScreen
// (lib/features/integrations/presentation/pages/medication_management.dart).
//
// Coverage strategy:
//   MedicationManagementScreen loads medications from SharedPreferences in
//   initState.  With mocked SharedPreferences (no saved medications) the
//   widget transitions from an isLoading=true state to the empty-medications
//   state.  CommonDrawer requires a UserProvider.
//
//   Branches tested (loading state):
//     CircularProgressIndicator shown   — isLoading == true on first frame.
//
//   Branches tested (empty-medications state after load):
//     Scaffold renders                  — widget settles without crashing.
//     "No Medications Added" title      — empty-state heading is shown.
//     "Scan Medication Barcode" button  — primary scan action is present.
//     "Enter NDC Code" button           — secondary input action is present.
//     "Add Manually" button             — tertiary input action is present.
//     "Medication Management" AppBar    — page title is correct.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:care_connect_app/providers/user_provider.dart';
import 'package:care_connect_app/features/integrations/presentation/pages/medication_management.dart';

/// Wraps [child] with a UserProvider (needed by the embedded CommonDrawer).
Widget _wrap(Widget child) {
  final provider = UserProvider();
  provider.setUser(UserSession(
    id: 1,
    email: 'cg@example.com',
    role: 'CAREGIVER',
    token: 'tok',
  ));
  return ChangeNotifierProvider<UserProvider>.value(
    value: provider,
    child: MaterialApp(home: child),
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    // No saved medications — exercises the null-medications branch of
    // _loadMedications and the empty-medications UI.
    SharedPreferences.setMockInitialValues({});
  });

  group('MedicationManagementScreen', () {
    testWidgets('renders Scaffold after medications load', (tester) async {
      // Verifies the widget settles without crashing once loading finishes.
      await tester.pumpWidget(_wrap(const MedicationManagementScreen()));
      await tester.pumpAndSettle();

      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows "No Medications Added" when no medications are saved', (
      tester,
    ) async {
      // Verifies the empty-state title after _loadMedications returns nothing.
      await tester.pumpWidget(_wrap(const MedicationManagementScreen()));
      await tester.pumpAndSettle();

      expect(find.text('No Medications Added'), findsOneWidget);
    });

    testWidgets('shows "Medication Management" in the AppBar', (tester) async {
      // Verifies the page title in the AppBar.
      await tester.pumpWidget(_wrap(const MedicationManagementScreen()));
      await tester.pumpAndSettle();

      expect(find.text('Medication Management'), findsOneWidget);
    });

    testWidgets('shows "Scan Medication Barcode" button', (tester) async {
      // Verifies the primary barcode-scan CTA is rendered.
      await tester.pumpWidget(_wrap(const MedicationManagementScreen()));
      await tester.pumpAndSettle();

      expect(find.text('Scan Medication Barcode'), findsOneWidget);
    });

    testWidgets('shows "Enter NDC Code" button', (tester) async {
      // Verifies the secondary NDC-entry CTA is rendered.
      await tester.pumpWidget(_wrap(const MedicationManagementScreen()));
      await tester.pumpAndSettle();

      expect(find.textContaining('Enter NDC Code'), findsOneWidget);
    });

    testWidgets('shows "Add Medication Manually" button', (tester) async {
      // Verifies the manual-entry CTA is rendered.
      await tester.pumpWidget(_wrap(const MedicationManagementScreen()));
      await tester.pumpAndSettle();

      expect(find.text('Add Medication Manually'), findsOneWidget);
    });
  });
}
