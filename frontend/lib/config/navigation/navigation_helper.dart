import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../../services/user_role_storage_service.dart';
import '../../providers/user_provider.dart';
import 'main_screen_config.dart';

const List<String> _patientTabNames = ['home', 'health', 'messages', 'profile'];
const List<String> _caregiverTabNames = [
  'patients',
  'tasks',
  'analytics',
  'messages',
  'profile',
];

/// Navigation helper that works with stored user data instead of URL parameters.
class NavigationHelper {
  /// Navigates to the main screen using the currently stored user data.
  static Future<void> navigateToMainScreen(
    BuildContext context, {
    int? tabIndex,
    bool clearHistory = false,
  }) async {
    final userData = await UserRoleStorageService.instance.getUserData();

    if (userData == null || !userData.isLoggedIn) {
      if (context.mounted) {
        context.go('/login');
      }
      return;
    }

    // Build the dashboard URL without role parameter
    String dashboardUrl = '/dashboard';
    final role = userData.role.toUpperCase();

    if (tabIndex != null) {
      // Convert tab index to tab name based on role
      final tabName = _getTabNameFromIndex(role, tabIndex);
      if (tabName != null) {
        dashboardUrl += '?tab=$tabName';
      }
    }

    if (context.mounted) {
      if (clearHistory) {
        context.go(dashboardUrl);
      } else {
        context.push(dashboardUrl);
      }
    }
  }

  /// Navigates to a specific tab in the main screen.
  static Future<void> navigateToTab(
    BuildContext context,
    String tabName,
  ) async {
    final userData = await UserRoleStorageService.instance.getUserData();

    if (userData == null || !userData.isLoggedIn) {
      if (context.mounted) {
        context.go('/login');
      }
      return;
    }

    if (context.mounted) {
      context.go('/dashboard?tab=$tabName');
    }
  }

  /// Returns the [MainScreenConfig] for the currently stored user data.
  static Future<MainScreenConfig?> getMainScreenConfig() async {
    final userData = await UserRoleStorageService.instance.getUserData();

    if (userData == null || !userData.isLoggedIn || userData.userId <= 0) {
      return null;
    }

    final role = userData.role.toUpperCase();

    switch (role) {
      case 'PATIENT':
        return MainScreenConfig.forPatient(
          userId: userData.userId,
          patientId: userData.patientId,
        );
      case 'CAREGIVER':
        return MainScreenConfig.forCaregiver(
          userId: userData.userId,
          caregiverId: userData.caregiverId,
          patientId: userData.patientId,
        );
      case 'FAMILY_LINK':
        return MainScreenConfig.forFamilyMember(
          userId: userData.userId,
          patientId: userData.patientId,
        );
      case 'ADMIN':
        return MainScreenConfig(
          userRole: 'ADMIN',
          userId: userData.userId,
          showAppBar: true,
          appBarTitle: 'Admin Dashboard',
          primaryColor: Colors.red,
        );
      default:
        return null;
    }
  }

  /// Check if user is authenticated
  static Future<bool> isAuthenticated() async {
      return await UserRoleStorageService.instance.isLoggedIn();
  }

  /// Logs out the current user and clears stored state.
  static Future<void> logout(BuildContext context) async {
    await UserRoleStorageService.instance.clearUserData();

    // Clear provider data as well
    if (context.mounted) {
      final userProvider = Provider.of<UserProvider>(context, listen: false);
      userProvider.clearUser();

      context.go('/login');
    }
  }

  /// Returns the tab name for a tab index based on the provided user role.
  static String? _getTabNameFromIndex(String role, int tabIndex) {
    final tabNames = _getTabNamesForRole(role);

    if (tabIndex < 0 || tabIndex >= tabNames.length) {
      return null;
    }

    return tabNames[tabIndex];
  }

  /// Returns the tab index for a tab name based on the provided user role.
  static int? getTabIndexFromName(String role, String tabName) {
    final normalizedRole = role.toUpperCase();
    final normalizedTabName = tabName.toLowerCase();
    final tabNames = _getTabNamesForRole(normalizedRole);
    final index = tabNames.indexOf(normalizedTabName);

    if (index == -1) {
      return null;
    }

    return index;
  }

  static List<String> _getTabNamesForRole(String role) {
    final normalizedRole = role.toUpperCase();
    return normalizedRole == 'PATIENT' ? _patientTabNames : _caregiverTabNames;
  }
}

/// Extension methods on [BuildContext] for common navigation helper actions.
extension NavigationContextExtension on BuildContext {
  /// Navigates to the main screen using stored user data.
  Future<void> navigateToMainScreen({
    int? tabIndex,
    bool clearHistory = false,
  }) async {
    await NavigationHelper.navigateToMainScreen(
      this,
      tabIndex: tabIndex,
      clearHistory: clearHistory,
    );
  }

  /// Navigates to a specific dashboard tab.
  Future<void> navigateToTab(String tabName) async {
    await NavigationHelper.navigateToTab(this, tabName);
  }

  /// Logs out the current user.
  Future<void> logoutUser() async {
    await NavigationHelper.logout(this);
  }
}
