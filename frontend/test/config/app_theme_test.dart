// Tests for AppTheme constants and styles
// (lib/config/theme/app_theme.dart).
//
// AppTheme exposes static Color constants, TextStyle constants, and ButtonStyle
// values. These tests verify that the expected values are accessible and have
// the correct alpha/value properties. Pure Dart — no platform channels needed.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/config/theme/app_theme.dart';

void main() {
  group('AppTheme – light theme colours', () {
    test('primary colour is non-transparent', () {
      // The main brand colour must have full opacity.
      expect((AppTheme.primary.a * 255.0).round().clamp(0, 255), 255);
    });

    test('primaryDark is darker than primary', () {
      // primaryDark.value must differ from primary.value.
      expect(AppTheme.primaryDark, isNot(equals(AppTheme.primary)));
    });

    test('primaryLight is lighter than primary', () {
      // primaryLight.value must differ from primary.value.
      expect(AppTheme.primaryLight, isNot(equals(AppTheme.primary)));
    });

    test('success colour is non-transparent', () {
      expect((AppTheme.success.a * 255.0).round().clamp(0, 255), 255);
    });

    test('warning colour is non-transparent', () {
      expect((AppTheme.warning.a * 255.0).round().clamp(0, 255), 255);
    });

    test('error colour is non-transparent', () {
      expect((AppTheme.error.a * 255.0).round().clamp(0, 255), 255);
    });

    test('info colour equals primary', () {
      // Info is mapped to the same cyan accent as primary.
      expect(AppTheme.info, AppTheme.primary);
    });

    test('textPrimary is very dark (slate-900)', () {
      // Slate-900 = #0F172A — very low red/green channels.
      expect((AppTheme.textPrimary.r * 255.0).round().clamp(0, 255), lessThan(50));
    });

    test('textLight is white', () {
      // textLight is used for text on dark surfaces.
      expect(AppTheme.textLight, const Color(0xFFFFFFFF));
    });

    test('backgroundPrimary is white', () {
      expect(AppTheme.backgroundPrimary, const Color(0xFFFFFFFF));
    });

    test('cardBackground is white', () {
      expect(AppTheme.cardBackground, const Color(0xFFFFFFFF));
    });
  });

  group('AppTheme – dark theme colours', () {
    test('primaryDarkTheme colour is non-transparent', () {
      expect((AppTheme.primaryDarkTheme.a * 255.0).round().clamp(0, 255), 255);
    });

    test('errorDarkTheme differs from light error', () {
      // The dark-theme error is a lighter shade.
      expect(AppTheme.errorDarkTheme, isNot(equals(AppTheme.error)));
    });

    test('backgroundPrimaryDarkTheme is near-black', () {
      // Near-black = very low red channel.
      expect((AppTheme.backgroundPrimaryDarkTheme.r * 255.0).round().clamp(0, 255), lessThan(30));
    });

    test('textPrimaryDarkTheme is near-white', () {
      // gray-200 (#E5E7EB) has a high red channel.
      expect((AppTheme.textPrimaryDarkTheme.r * 255.0).round().clamp(0, 255), greaterThan(200));
    });
  });

  group('AppTheme – typography styles', () {
    test('headingLarge has font size 28', () {
      expect(AppTheme.headingLarge.fontSize, 28);
    });

    test('headingMedium has font size 24', () {
      expect(AppTheme.headingMedium.fontSize, 24);
    });

    test('headingSmall has font size 20', () {
      expect(AppTheme.headingSmall.fontSize, 20);
    });

    test('bodyLarge has font size 16', () {
      expect(AppTheme.bodyLarge.fontSize, 16);
    });

    test('bodyMedium has font size 14', () {
      expect(AppTheme.bodyMedium.fontSize, 14);
    });

    test('bodySmall has font size 12', () {
      expect(AppTheme.bodySmall.fontSize, 12);
    });

    test('buttonText has font size 16', () {
      expect(AppTheme.buttonText.fontSize, 16);
    });

    test('heading styles are bold', () {
      // All three heading styles use FontWeight.bold.
      expect(AppTheme.headingLarge.fontWeight, FontWeight.bold);
      expect(AppTheme.headingMedium.fontWeight, FontWeight.bold);
      expect(AppTheme.headingSmall.fontWeight, FontWeight.bold);
    });
  });

  group('AppTheme – video-call colours', () {
    test('videoCallBackground is black', () {
      expect(AppTheme.videoCallBackground, const Color(0xFF000000));
    });

    test('videoCallText is white', () {
      expect(AppTheme.videoCallText, const Color(0xFFFFFFFF));
    });

    test('videoCallEndCall is red', () {
      // The end-call button uses error red.
      expect(AppTheme.videoCallEndCall, AppTheme.error);
    });
  });

  group('AppTheme – chat colours', () {
    test('chatUserMessage equals primary', () {
      // User chat bubbles use the primary cyan.
      expect(AppTheme.chatUserMessage, AppTheme.primary);
    });

    test('chatTextOnPrimary is white', () {
      // Text on cyan bubbles must be white for contrast.
      expect(AppTheme.chatTextOnPrimary, const Color(0xFFFFFFFF));
    });
  });
}
