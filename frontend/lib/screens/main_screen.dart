import 'dart:convert';

import 'package:care_connect_app/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';
import '../providers/user_provider.dart';
import '../config/navigation/bottom_nav_config.dart';
import '../config/navigation/main_screen_config.dart';
import '../services/call_notification_service.dart';
import '../services/api_service.dart';
import '../widgets/hybrid_video_call_widget.dart';

/// Main screen of the application. This is where the user is navigated to
/// after logging in. This contains the bottom nav bar and main screens
class MainScreen extends StatefulWidget {
  final int? initialTabIndex;
  final MainScreenConfig? config;

  const MainScreen({
    super.key,
    this.initialTabIndex,
    this.config,
  });

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _selectedIndex = 0;
  List<BottomNavItem> _navItems = [];
  late PageController _pageController;
  late MainScreenConfig _config;

  @override
  void initState() {
    super.initState();
    _initializeConfig();
    _pageController = PageController(initialPage: widget.initialTabIndex ?? 0);
    _selectedIndex = widget.initialTabIndex ?? 0;
    _initializeNavigation();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initializeCallNotifications();
    });
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  Future<void> _initializeCallNotifications() async {
    final userProvider = Provider.of<UserProvider>(context, listen: false);
    final user = userProvider.user;
    if (user == null) return;

    final role = user.role.toUpperCase();
    if (role != 'CAREGIVER' && role != 'PATIENT') return;

    await CallNotificationService.initialize(
      userId: user.id.toString(),
      userRole: role,
      userDisplayName: user.name,
      context: context,
    );
  }

  /// Initialize the MainScreenConfig object.
  void _initializeConfig() {
    final userProvider = Provider.of<UserProvider>(context, listen: false);

    if (widget.config != null) {
      _config = widget.config!;
    } else {
      final user = userProvider.user;

      // Check if user data is missing or invalid
      if (user == null || user.role.isEmpty || user.id <= 0) {
        _redirectToLoginWithMessage('Please log in again');
        return;
      }

      final role = user.role;
      final userId = user.id;
      final patientId = user.patientId;
      final caregiverId = user.caregiverId;

      _config = MainScreenConfig(
        userRole: role,
        userId: userId,
        patientId: patientId,
        caregiverId: caregiverId,
      );
    }
  }

  /// Redirect to login screen with a message.
  void _redirectToLoginWithMessage(String message) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      // Clear user data
      final userProvider = Provider.of<UserProvider>(context, listen: false);
      userProvider.clearUser();

      // Show message
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(message)),
        );

        // Navigate to login
        context.go('/login');
      }
    });
  }

  /// Initialize the navigation items.
  void _initializeNavigation() {
    setState(() {
      _navItems = _config.getNavItems();
      // Ensure selected index is within bounds
      if (_selectedIndex >= _navItems.length) {
        _selectedIndex = 0;
      }
    });
  }

  /// Handle bottom nav bar item tap.
  void _onItemTapped(int index) {
    final navItem = _navItems[index];

    // Check if onPress callback exists and call it
    if (navItem.onPress != null) {
      navItem.onPress!(context, (context) => Container());
      // Don't change screen if only onPress is present
      return;
    }

    // Only change screen if there's an actual screen to navigate to
    if (navItem.screen != null) {
      setState(() {
        _selectedIndex = index;
      });

      if (_config.enablePageAnimation) {
        _pageController.animateToPage(
          index,
          duration: _config.animationDuration,
          curve: _config.animationCurve,
        );
      } else {
        _pageController.jumpToPage(index);
      }
    }
  }

  void _onPageChanged(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  int? _toInt(dynamic value) {
    if (value is int) return value;
    return int.tryParse(value?.toString() ?? '');
  }

  String _trimmed(dynamic value) => (value ?? '').toString().trim();

  String _fullName(String first, String last, String fallback) {
    final name = [first.trim(), last.trim()].where((e) => e.isNotEmpty).join(' ').trim();
    if (name.isNotEmpty) return name;
    return fallback;
  }

  bool _isRoleSupportedForGlobalCall(String role) {
    final normalized = role.trim().toUpperCase();
    return normalized == 'PATIENT' || normalized == 'CAREGIVER';
  }

  Future<List<_QuickCallTarget>> _loadQuickCallTargets(UserSession user) async {
    final role = user.role.trim().toUpperCase();
    if (role == 'PATIENT') {
      final links = await ApiService.getPatientLinkedCaregiverLinks(user.id);
      return links.where((link) {
        final enabledRaw = link['patientVideoCallsEnabled'];
        return enabledRaw is bool ? enabledRaw : '$enabledRaw'.toLowerCase() != 'false';
      }).map((link) {
        final caregiverUserId = _toInt(link['caregiverUserId']);
        if (caregiverUserId == null || caregiverUserId <= 0) {
          return null;
        }
        final caregiverName = _trimmed(link['caregiverName']);
        final caregiverEmail = _trimmed(link['caregiverEmail']);
        return _QuickCallTarget(
          userId: caregiverUserId,
          role: 'CAREGIVER',
          title: caregiverName.isNotEmpty ? caregiverName : 'Caregiver $caregiverUserId',
          subtitle: 'Caregiver - Patient calls enabled',
          email: caregiverEmail,
          phone: null,
        );
      }).whereType<_QuickCallTarget>().toList()
        ..sort((a, b) => a.title.toLowerCase().compareTo(b.title.toLowerCase()));
    }

    if (role != 'CAREGIVER') {
      return const [];
    }

    final caregiverId = user.caregiverId;
    if (caregiverId == null || caregiverId <= 0) {
      return const [];
    }

    final patientsResponse = await ApiService.getCaregiverPatients(caregiverId);
    if (patientsResponse.statusCode != 200) {
      return const [];
    }

    final decoded = jsonDecode(patientsResponse.body);
    if (decoded is! List) {
      return const [];
    }

    final patientTargets = <_QuickCallTarget>[];
    final patientUserIds = <int>{};
    final careTeamByUserId = <int, _CareTeamAggregate>{};
    final currentUserId = user.id;

    for (final item in decoded) {
      if (item is! Map<String, dynamic>) continue;
      final link = item['link'];
      final patient = item['patient'];
      final linkMap = link is Map<String, dynamic> ? link : const <String, dynamic>{};
      final patientMap = patient is Map<String, dynamic> ? patient : const <String, dynamic>{};

      final patientUserId = _toInt(linkMap['patientUserId']) ?? _toInt(patientMap['userId']);
      if (patientUserId == null || patientUserId <= 0 || patientUserIds.contains(patientUserId)) {
        continue;
      }
      patientUserIds.add(patientUserId);

      final patientName = _fullName(
        _trimmed(patientMap['firstName']),
        _trimmed(patientMap['lastName']),
        _trimmed(linkMap['patientName']).isNotEmpty
            ? _trimmed(linkMap['patientName'])
            : 'Patient $patientUserId',
      );
      final patientEmail = _trimmed(patientMap['email']).isNotEmpty
          ? _trimmed(patientMap['email'])
          : _trimmed(linkMap['patientEmail']);
      final patientPhone = _trimmed(patientMap['phone']);

      patientTargets.add(
        _QuickCallTarget(
          userId: patientUserId,
          role: 'PATIENT',
          title: patientName,
          subtitle: 'Assigned patient',
          email: patientEmail.isNotEmpty ? patientEmail : null,
          phone: patientPhone.isNotEmpty ? patientPhone : null,
        ),
      );
    }

    for (final patientTarget in patientTargets) {
      final links = await ApiService.getPatientLinkedCaregiverLinks(patientTarget.userId);
      for (final link in links) {
        final caregiverUserId = _toInt(link['caregiverUserId']);
        if (caregiverUserId == null || caregiverUserId <= 0 || caregiverUserId == currentUserId) {
          continue;
        }
        final caregiverName = _trimmed(link['caregiverName']);
        final caregiverEmail = _trimmed(link['caregiverEmail']);
        final aggregate = careTeamByUserId.putIfAbsent(
          caregiverUserId,
          () => _CareTeamAggregate(
            userId: caregiverUserId,
            name: caregiverName.isNotEmpty ? caregiverName : 'Caregiver $caregiverUserId',
            email: caregiverEmail.isNotEmpty ? caregiverEmail : null,
          ),
        );
        aggregate.patientNames.add(patientTarget.title);
        aggregate.patientUserIds.add(patientTarget.userId);
        if (aggregate.email == null && caregiverEmail.isNotEmpty) {
          aggregate.email = caregiverEmail;
        }
      }
    }

    final careTeamTargets = careTeamByUserId.values.map((entry) {
      final context = entry.patientNames.toList()..sort();
      final summary = context.isEmpty ? 'Care team caregiver' : 'Care team for: ${context.join(', ')}';
      return _QuickCallTarget(
        userId: entry.userId,
        role: 'CAREGIVER',
        title: entry.name,
        subtitle: summary,
        email: entry.email,
        phone: null,
        contextPatientUserIds: entry.patientUserIds.toList()..sort(),
      );
    }).toList()
      ..sort((a, b) => a.title.toLowerCase().compareTo(b.title.toLowerCase()));

    patientTargets.sort((a, b) => a.title.toLowerCase().compareTo(b.title.toLowerCase()));
    return [...patientTargets, ...careTeamTargets];
  }

  Future<void> _startQuickVideoCall({
    required UserSession currentUser,
    required _QuickCallTarget target,
  }) async {
    final role = currentUser.role.trim().toUpperCase();
    final allowed = await ApiService.canInitiateVideoCall(
      currentUserId: currentUser.id,
      currentUserRole: role,
      targetUserId: target.userId,
      caregiverId: currentUser.caregiverId,
    );

    if (!allowed) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('You are not allowed to call ${target.title}.'),
          behavior: SnackBarBehavior.floating,
        ),
      );
      return;
    }

    final callId = 'chime_call_${DateTime.now().millisecondsSinceEpoch}';
    if (!mounted) return;
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => HybridVideoCallWidget(
          userId: currentUser.id.toString(),
          userRole: role,
          callId: callId,
          recipientId: target.userId.toString(),
          recipientRole: target.role,
          isInitiator: true,
          isVideoEnabled: true,
          userName: (currentUser.name ?? '').trim().isNotEmpty
              ? currentUser.name!.trim()
              : currentUser.email,
          userEmail: currentUser.email,
          recipientName: target.title,
          recipientEmail: target.email,
          recipientPhone: target.phone,
          callKind: target.isCareTeamCall ? 'CARE_TEAM' : 'GENERAL',
          contextPatientUserIds: target.contextPatientUserIds,
        ),
      ),
    );
  }

  Future<void> _showQuickCallPicker() async {
    final userProvider = Provider.of<UserProvider>(context, listen: false);
    final user = userProvider.user;
    if (user == null) return;
    if (!_isRoleSupportedForGlobalCall(user.role)) return;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (sheetContext) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
            child: FutureBuilder<List<_QuickCallTarget>>(
              future: _loadQuickCallTargets(user),
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const SizedBox(
                    height: 240,
                    child: Center(child: CircularProgressIndicator()),
                  );
                }

                if (snapshot.hasError) {
                  return SizedBox(
                    height: 260,
                    child: Center(
                      child: Text(
                        'Unable to load call contacts.\n${snapshot.error}',
                        textAlign: TextAlign.center,
                      ),
                    ),
                  );
                }

                final targets = snapshot.data ?? const <_QuickCallTarget>[];
                if (targets.isEmpty) {
                  final role = user.role.trim().toUpperCase();
                  final emptyText = role == 'PATIENT'
                      ? 'No caregivers are available for patient-initiated calls.'
                      : 'No assigned patients or care-team caregivers are available.';
                  return SizedBox(
                    height: 220,
                    child: Center(
                      child: Text(
                        emptyText,
                        textAlign: TextAlign.center,
                      ),
                    ),
                  );
                }

                return Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Start Video Call',
                      style: Theme.of(context).textTheme.titleLarge?.copyWith(
                            fontWeight: FontWeight.w700,
                          ),
                    ),
                    const SizedBox(height: 8),
                    Flexible(
                      child: ListView.separated(
                        shrinkWrap: true,
                        itemCount: targets.length,
                        separatorBuilder: (_, __) => const Divider(height: 1),
                        itemBuilder: (_, index) {
                          final target = targets[index];
                          final roleBadge = target.role == 'PATIENT' ? 'PATIENT' : 'CAREGIVER';
                          return ListTile(
                            leading: CircleAvatar(
                              child: Text(
                                target.title.isNotEmpty
                                    ? target.title.substring(0, 1).toUpperCase()
                                    : '?',
                              ),
                            ),
                            title: Text(target.title),
                            subtitle: Text('${target.subtitle} - $roleBadge'),
                            trailing: const Icon(Icons.video_call_outlined),
                            onTap: () async {
                              Navigator.of(sheetContext).pop();
                              await _startQuickVideoCall(
                                currentUser: user,
                                target: target,
                              );
                            },
                          );
                        },
                      ),
                    ),
                  ],
                );
              },
            ),
          ),
        );
      },
    );
  }

  Widget? _buildGlobalCallFab() {
    final userProvider = Provider.of<UserProvider>(context, listen: false);
    final user = userProvider.user;
    if (user == null || !_isRoleSupportedForGlobalCall(user.role)) {
      return null;
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: 78),
      child: FloatingActionButton(
        heroTag: 'globalCallFab',
        tooltip: 'Start video call',
        onPressed: _showQuickCallPicker,
        child: const Icon(Icons.video_call),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<UserProvider>(
      builder: (context, userProvider, child) {
        // Check if user data is missing or invalid
        final currentUser = userProvider.user;
        if (widget.config == null && (currentUser == null || currentUser.role.isEmpty || currentUser.id <= 0)) {
          // Return a loading screen while redirecting
          _redirectToLoginWithMessage('Please log in again');
          return const Scaffold(
            body: Center(
              child: CircularProgressIndicator(),
            ),
          );
        }

        // Update configuration if user changes
        final currentRole = currentUser?.role ?? '';
        final currentUserId = currentUser?.id ?? 0;

        if (widget.config == null && (_config.userRole != currentRole || _config.userId != currentUserId)) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            _initializeConfig();
            _initializeNavigation();
          });
        }
        


        return Scaffold(
          backgroundColor: _config.backgroundColor,
          appBar: _config.showAppBar ? AppBar(
            title: Text(_config.appBarTitle ?? 'CareConnect'),
            backgroundColor: _config.primaryColor ?? Theme.of(context).primaryColor,
            foregroundColor: Colors.white,
            actions: _config.appBarActions,
          ) : null,
          body: PageView.builder(
            controller: _pageController,
            onPageChanged: _onPageChanged,
            itemCount: _navItems.length,
            itemBuilder: (context, index) {
              final navItem = _navItems[index];

              return _navItems[index].screen;
            },
          ),
          floatingActionButton: _buildGlobalCallFab(),
          floatingActionButtonLocation: FloatingActionButtonLocation.endFloat,
          bottomNavigationBar: _buildBottomNavigationBar(),
        );
      },
    );
  }

  /// Build the bottom navigation bar
  Widget _buildBottomNavigationBar() {
   final t = AppLocalizations.of(context)!;
    return Container(
      decoration: BoxDecoration(
        boxShadow: [
          BoxShadow(
            color: Theme.of(context).shadowColor.withOpacity(0.1),
            blurRadius: 10,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: BottomNavigationBar(
        currentIndex: _selectedIndex,
        onTap: _onItemTapped,
        type: BottomNavigationBarType.fixed,
        backgroundColor: Theme.of(context).scaffoldBackgroundColor,
        selectedItemColor: _config.primaryColor ?? Theme.of(context).primaryColor,
        unselectedItemColor: Colors.grey[600],
        selectedFontSize: 12,
        unselectedFontSize: 12,
        iconSize: 24,
        items: _navItems.map((item) {
          return BottomNavigationBarItem(
            icon: Icon(item.icon),
            activeIcon: Icon(item.activeIcon ?? item.icon),
           label: item.localizedLabel(t),
          );
        }).toList(),
      ),
    );
  }
}

/// Extension to provide easy navigation to specific tabs
extension MainScreenNavigation on BuildContext {
  void navigateToMainScreen({
    int? tabIndex,
    MainScreenConfig? config,
  }) {
    Navigator.of(this).pushReplacement(
      MaterialPageRoute(
        builder: (context) => MainScreen(
          initialTabIndex: tabIndex,
          config: config,
        ),
      ),
    );
  }

  void navigateToMainScreenWithConfig(MainScreenConfig config, {int? tabIndex}) {
    Navigator.of(this).pushReplacement(
      MaterialPageRoute(
        builder: (context) => MainScreen(
          initialTabIndex: tabIndex,
          config: config,
        ),
      ),
    );
  }
}

class _QuickCallTarget {
  final int userId;
  final String role;
  final String title;
  final String subtitle;
  final String? email;
  final String? phone;
  final List<int>? contextPatientUserIds;

  const _QuickCallTarget({
    required this.userId,
    required this.role,
    required this.title,
    required this.subtitle,
    this.email,
    this.phone,
    this.contextPatientUserIds,
  });

  bool get isCareTeamCall =>
      role.toUpperCase() == 'CAREGIVER' &&
      contextPatientUserIds != null &&
      contextPatientUserIds!.isNotEmpty;
}

class _CareTeamAggregate {
  final int userId;
  final String name;
  String? email;
  final Set<String> patientNames = <String>{};
  final Set<int> patientUserIds = <int>{};

  _CareTeamAggregate({
    required this.userId,
    required this.name,
    this.email,
  });
}
