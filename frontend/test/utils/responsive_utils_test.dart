// Tests for ResponsiveUtils and ResponsiveContext extension
// (lib/utils/responsive_utils.dart).
//
// Pure MediaQuery-based static methods — testable by controlling
// tester.view.physicalSize to simulate different device widths.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/utils/responsive_utils.dart';

// Helper: build a widget that captures results via callback
Widget _buildWithContext(void Function(BuildContext) capture) {
  return MaterialApp(
    home: Builder(
      builder: (context) {
        capture(context);
        return const SizedBox();
      },
    ),
  );
}

void main() {
  group('ResponsiveUtils.getDeviceType', () {
    testWidgets('returns mobile for width < 600', (tester) async {
      tester.view.physicalSize = const Size(400, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      DeviceType? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getDeviceType(ctx);
      }));
      expect(result, DeviceType.mobile);
    });

    testWidgets('returns tablet for width 600–899', (tester) async {
      tester.view.physicalSize = const Size(700, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      DeviceType? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getDeviceType(ctx);
      }));
      expect(result, DeviceType.tablet);
    });

    testWidgets('returns desktop for width 900–1199', (tester) async {
      tester.view.physicalSize = const Size(1000, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      DeviceType? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getDeviceType(ctx);
      }));
      expect(result, DeviceType.desktop);
    });

    testWidgets('returns largeDesktop for width >= 1200', (tester) async {
      tester.view.physicalSize = const Size(1500, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      DeviceType? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getDeviceType(ctx);
      }));
      expect(result, DeviceType.largeDesktop);
    });
  });

  group('ResponsiveUtils.getGridColumnCount', () {
    testWidgets('returns 1 column for mobile', (tester) async {
      tester.view.physicalSize = const Size(400, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      int? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getGridColumnCount(ctx);
      }));
      expect(result, 1);
    });

    testWidgets('returns 2 columns for tablet', (tester) async {
      tester.view.physicalSize = const Size(700, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      int? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getGridColumnCount(ctx);
      }));
      expect(result, 2);
    });

    testWidgets('returns 3 columns for desktop', (tester) async {
      tester.view.physicalSize = const Size(1000, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      int? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getGridColumnCount(ctx);
      }));
      expect(result, 3);
    });

    testWidgets('returns 4 columns for largeDesktop', (tester) async {
      tester.view.physicalSize = const Size(1500, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      int? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getGridColumnCount(ctx);
      }));
      expect(result, 4);
    });
  });

  group('ResponsiveUtils.getHorizontalMargin', () {
    testWidgets('returns 16.0 for mobile', (tester) async {
      tester.view.physicalSize = const Size(400, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      double? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getHorizontalMargin(ctx);
      }));
      expect(result, 16.0);
    });

    testWidgets('returns 5% of width for tablet', (tester) async {
      tester.view.physicalSize = const Size(700, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      double? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getHorizontalMargin(ctx);
      }));
      expect(result, closeTo(700 * 0.05, 0.1));
    });

    testWidgets('returns 8% of width for desktop', (tester) async {
      tester.view.physicalSize = const Size(1000, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      double? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getHorizontalMargin(ctx);
      }));
      expect(result, closeTo(1000 * 0.08, 0.1));
    });
  });

  group('ResponsiveUtils.shouldConstrainWidth', () {
    testWidgets('returns false for mobile', (tester) async {
      tester.view.physicalSize = const Size(400, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      bool? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.shouldConstrainWidth(ctx);
      }));
      expect(result, isFalse);
    });

    testWidgets('returns true for desktop', (tester) async {
      tester.view.physicalSize = const Size(1300, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      bool? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.shouldConstrainWidth(ctx);
      }));
      expect(result, isTrue);
    });
  });

  group('ResponsiveUtils.getResponsiveFontSize', () {
    testWidgets('returns base size for mobile', (tester) async {
      tester.view.physicalSize = const Size(400, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      double? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getResponsiveFontSize(ctx, baseFontSize: 14.0);
      }));
      expect(result, 14.0);
    });

    testWidgets('returns larger size for tablet', (tester) async {
      tester.view.physicalSize = const Size(700, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      double? result;
      await tester.pumpWidget(_buildWithContext((ctx) {
        result = ResponsiveUtils.getResponsiveFontSize(ctx, baseFontSize: 14.0);
      }));
      expect(result, greaterThan(14.0));
    });
  });

  group('ResponsiveUtils.constrainedWidthContainer', () {
    testWidgets('wraps in Center+Container on desktop', (tester) async {
      tester.view.physicalSize = const Size(1300, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(MaterialApp(
        home: Builder(
          builder: (context) => ResponsiveUtils.constrainedWidthContainer(
            context: context,
            child: const Text('content'),
          ),
        ),
      ));
      expect(find.byType(Center), findsWidgets);
      expect(find.text('content'), findsOneWidget);
    });

    testWidgets('returns child directly on mobile', (tester) async {
      tester.view.physicalSize = const Size(400, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      await tester.pumpWidget(MaterialApp(
        home: Builder(
          builder: (context) => ResponsiveUtils.constrainedWidthContainer(
            context: context,
            child: const Text('content'),
          ),
        ),
      ));
      expect(find.text('content'), findsOneWidget);
    });
  });

  group('ResponsiveContext extension', () {
    testWidgets('isMobile is true on mobile viewport', (tester) async {
      tester.view.physicalSize = const Size(400, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      bool? isMobile;
      await tester.pumpWidget(_buildWithContext((ctx) {
        isMobile = ctx.isMobile;
      }));
      expect(isMobile, isTrue);
    });

    testWidgets('isDesktopOrLarger is true on desktop viewport', (tester) async {
      tester.view.physicalSize = const Size(1300, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      bool? isDesktopOrLarger;
      await tester.pumpWidget(_buildWithContext((ctx) {
        isDesktopOrLarger = ctx.isDesktopOrLarger;
      }));
      expect(isDesktopOrLarger, isTrue);
    });

    testWidgets('gridColumns matches getGridColumnCount', (tester) async {
      tester.view.physicalSize = const Size(700, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      int? columns;
      await tester.pumpWidget(_buildWithContext((ctx) {
        columns = ctx.gridColumns;
      }));
      expect(columns, 2);
    });

    testWidgets('responsiveValue returns mobile value on mobile', (tester) async {
      tester.view.physicalSize = const Size(400, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      String? value;
      await tester.pumpWidget(_buildWithContext((ctx) {
        value = ctx.responsiveValue<String>(
          mobile: 'mobile',
          tablet: 'tablet',
          desktop: 'desktop',
        );
      }));
      expect(value, 'mobile');
    });

    testWidgets('responsiveValue falls back to mobile when tablet/desktop null', (tester) async {
      tester.view.physicalSize = const Size(700, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);

      String? value;
      await tester.pumpWidget(_buildWithContext((ctx) {
        value = ctx.responsiveValue<String>(mobile: 'fallback');
      }));
      expect(value, 'fallback');
    });
  });
}
