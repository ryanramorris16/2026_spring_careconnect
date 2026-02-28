import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

// Pain level card
import '../widgets/pain_level_card.dart';

// Header
import '../widgets/patient_header_card.dart';

// Info tab pieces
import '../widgets/contact_info_card.dart';
import '../widgets/emergency_contact_card.dart';

// Mood tab
import '../widgets/mood_history_card.dart';

// Health tab
import '../widgets/current_medications_card.dart';
// Recent Activity tab
// Virtual Check-in history
// Virtual Check-In domain entities
import 'package:care_connect_app/features/health/virtual_check_in/models/virtual_check_in.dart';
import 'package:care_connect_app/features/health/virtual_check_in/models/virtual_check_in_question.dart';

// Virtual Check-In UI
import 'package:care_connect_app/features/health/virtual_check_in/presentation/widgets/virtual_check_in_config_sheet.dart';
import 'package:care_connect_app/features/health/virtual_check_in/presentation/widgets/virtual_check_in_history_card.dart';

// (If this page calls the APIs, add:)


// 👉 Alias BOTH sides to avoid type clashes
import '../models/symptom_entry.dart' as model;
import '../widgets/recent_symptom_card.dart' as sympt;

// API and models
import '../../../../services/api_service.dart';
import '../../../../services/call_notification_service.dart';
import '../../../health/medication-tracker/models/medication-model.dart';
import '../../../../providers/user_provider.dart';
import 'package:care_connect_app/features/activities/presentation/pages/adl_iadl_management_screen.dart';
import 'package:care_connect_app/features/activities/presentation/pages/behavioral_incident_screens.dart';
import 'package:care_connect_app/features/dashboards/dashboards_screen.dart';
import 'package:care_connect_app/features/evv/presentation/pages/incident_report_screens.dart';
import 'incident_report_history_screen.dart';
import '../../../audit/audit_log_screen.dart';

class PatientDetailsPage extends StatefulWidget {
  final String patientId;
  /// NEW: when true, caregiver UI (can configure); when false, patient UI (no configure).
  final bool isCaregiver;

  const PatientDetailsPage({
    super.key,
    required this.patientId,
    this.isCaregiver = false, // default to patient behavior
  });

  @override
  State<PatientDetailsPage> createState() => _PatientDetailsPageState();
}

class _PatientDetailsPageState extends State<PatientDetailsPage> {
  List<Medication> medications = [];
  bool _isLoadingMedications = false;
  String? _medicationError;

  Map<String, dynamic>? _patientProfile;
  bool _isLoadingProfile = false;
  String? _profileError;

  final _likesController = TextEditingController();
  final _dislikesController = TextEditingController();
  final _habitsController = TextEditingController();
  final _phobiasController = TextEditingController();
  String? _preferredCommunicationMethod; // verbal | visual | written | gesture
  bool _isSavingPersonalization = false;
  bool _isEditingPersonalization = false;

  // Known Risks
  List<Map<String, dynamic>> _riskTypes = [];
  Map<int, int> _riskIdByTypeId = {}; // riskTypeId -> patient_risk id (for DELETE)
  bool _isLoadingRisks = false;
  String? _risksError;
  bool _isEditingRisks = false;
  Set<int> _editingCheckedTypeIds = {}; // while editing, which risk type ids are checked
  bool _isSavingRisks = false;

  Future<void> _startVideoCall(String patientName) async {
    final user = Provider.of<UserProvider>(context, listen: false).user;
    if (user == null) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Unable to determine your account.')),
      );
      return;
    }

    final role = user.role.toUpperCase();
    final currentUserId = user.id;

    int? targetUserId;
    var recipientName = patientName;

    if (role == 'CAREGIVER') {
      targetUserId = int.tryParse(widget.patientId);
      if (targetUserId == null) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Invalid patient ID for video call.')),
        );
        return;
      }
    } else if (role == 'PATIENT') {
      final linkedCaregiverUserIds = await ApiService.getPatientLinkedCaregiverUserIds(
        currentUserId,
      );

      if (!mounted) return;

      if (linkedCaregiverUserIds.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('No assigned caregiver found for video call.')),
        );
        return;
      }

      targetUserId = linkedCaregiverUserIds.first;
      recipientName = 'Caregiver';
    } else {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Video calling is not available for this role.')),
      );
      return;
    }

    final allowed = await ApiService.canInitiateVideoCall(
      currentUserId: currentUserId,
      currentUserRole: role,
      targetUserId: targetUserId,
      caregiverId: user.caregiverId,
    );

    if (!mounted) return;

    if (!allowed) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('You are not allowed to start this video call.')),
      );
      return;
    }

    final callId = 'chime_call_${DateTime.now().millisecondsSinceEpoch}';
    final recipientRole = role == 'CAREGIVER' ? 'PATIENT' : 'CAREGIVER';
    await CallNotificationService.sendCallInvitation(
      recipientId: targetUserId.toString(),
      recipientRole: recipientRole,
      callId: callId,
      isVideoCall: true,
    );

    context.push(
      '/video-call-chime'
      '?userId=$currentUserId'
      '&recipientId=$targetUserId'
      '&userName=${Uri.encodeComponent(user.name ?? role)}'
      '&recipientName=${Uri.encodeComponent(recipientName)}'
      '&initiator=true'
      '&video=true'
      '&audio=true'
      '&callId=$callId',
    );
  }
  
  @override
  void initState() {
    super.initState();
    _fetchPatientProfile();
    _fetchMedications();
    _fetchRiskTypes();
    _fetchPatientRisks();
  }

  @override
  void dispose() {
    _likesController.dispose();
    _dislikesController.dispose();
    _habitsController.dispose();
    _phobiasController.dispose();
    super.dispose();
  }

  Future<void> _fetchPatientProfile() async {
    setState(() {
      _isLoadingProfile = true;
      _profileError = null;
    });

    final patientIdInt = int.tryParse(widget.patientId);
    if (patientIdInt == null) {
      setState(() {
        _isLoadingProfile = false;
        _profileError = 'Invalid patient ID';
      });
      return;
    }

    try {
      final resp = await ApiService.getPatientProfile(patientIdInt);
      if (resp.statusCode != 200) {
        setState(() {
          _isLoadingProfile = false;
          _profileError = 'Failed to load patient profile: ${resp.statusCode}';
        });
        return;
      }

      final data = jsonDecode(resp.body) as Map<String, dynamic>;

      setState(() {
        _patientProfile = data;
        _isLoadingProfile = false;
      });

      _populatePersonalizationControllers(data);
    } catch (e) {
      setState(() {
        _isLoadingProfile = false;
        _profileError = 'Error loading patient profile: $e';
      });
    }
  }

  void _populatePersonalizationControllers(Map<String, dynamic> data) {
    // Support both camelCase and snake_case keys until backend is consistent.
    String? readString(String camel, String snake) {
      final v = data[camel] ?? data[snake];
      if (v == null) return null;
      return v.toString();
    }

    _likesController.text = readString('likes', 'likes') ?? '';
    _dislikesController.text = readString('dislikes', 'dislikes') ?? '';
    _habitsController.text = readString('habits', 'habits') ?? '';
    _phobiasController.text = readString('phobias', 'phobias') ?? '';

    final pref = readString(
      'preferredCommunicationMethod',
      'preferred_communication_method',
    );
    final normalized = pref?.trim().toLowerCase();
    const allowed = {'verbal', 'visual', 'written', 'gesture'};
    setState(() {
      _preferredCommunicationMethod =
          allowed.contains(normalized) ? normalized : null;
    });
  }

  Future<void> _savePersonalization() async {
    final patientIdInt = int.tryParse(widget.patientId);
    if (patientIdInt == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Invalid patient ID')),
      );
      return;
    }

    setState(() => _isSavingPersonalization = true);
    try {
      // Preserve existing fields by starting from the fetched profile map.
      final payload = <String, dynamic>{...?_patientProfile};

      payload['likes'] = _likesController.text.trim();
      payload['dislikes'] = _dislikesController.text.trim();
      payload['habits'] = _habitsController.text.trim();
      payload['phobias'] = _phobiasController.text.trim();
      payload['preferredCommunicationMethod'] = _preferredCommunicationMethod;

      final resp = await ApiService.updatePatientProfile(patientIdInt, payload);
      if (resp.statusCode == 200) {
        final body = jsonDecode(resp.body) as Map<String, dynamic>;
        final updated = (body['data'] ?? body) as Map<String, dynamic>;
        setState(() {
          _patientProfile = updated;
        });
        _populatePersonalizationControllers(updated);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Personalization saved')),
          );
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Save failed: ${resp.statusCode}'),
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Save failed: $e')),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isSavingPersonalization = false;
          _isEditingPersonalization = false;
        });
      }
    }
  }

  Widget _buildPersonalizationCard() {
    final theme = Theme.of(context);

    if (_isLoadingProfile) {
      return Card(
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: theme.colorScheme.primary,
                ),
              ),
              const SizedBox(width: 12),
              const Text('Loading personalization...'),
            ],
          ),
        ),
      );
    }

    if (_profileError != null) {
      return Card(
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Personalization',
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                _profileError!,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.error,
                ),
              ),
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton.icon(
                  onPressed: _fetchPatientProfile,
                  icon: const Icon(Icons.refresh),
                  label: const Text('Retry'),
                ),
              ),
            ],
          ),
        ),
      );
    }

    final canEdit = widget.isCaregiver;

    InputDecoration deco(String label) => InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
          isDense: true,
        );

    Widget row({
      required IconData icon,
      required String label,
      required TextEditingController controller,
    }) {
      if (_isEditingPersonalization && canEdit) {
        return Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(top: 10, right: 8),
              child: Icon(icon,
                  size: 20, color: theme.colorScheme.primary.withOpacity(0.8)),
            ),
            Expanded(
              child: TextField(
                controller: controller,
                enabled: true,
                maxLines: 2,
                decoration: deco(label),
              ),
            ),
          ],
        );
      } else {
        final value = controller.text.trim();
        return Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(top: 2, right: 8),
              child: Icon(icon,
                  size: 20, color: theme.colorScheme.primary.withOpacity(0.8)),
            ),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: Colors.grey.shade700,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    value.isEmpty ? 'Not set' : value,
                    style: theme.textTheme.bodyMedium,
                  ),
                ],
              ),
            ),
          ],
        );
      }
    }

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    'Personalization',
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                    if (canEdit)
                      ElevatedButton.icon(
                        onPressed: _isSavingPersonalization
                            ? null
                            : () {
                                if (_isEditingPersonalization) {
                                  _savePersonalization();
                                } else {
                                  setState(() {
                                    _isEditingPersonalization = true;
                                  });
                                }
                              },
                        icon: _isSavingPersonalization
                            ? const SizedBox(
                                width: 16,
                                height: 16,
                                child:
                                    CircularProgressIndicator(strokeWidth: 2),
                              )
                            : Icon(
                                _isEditingPersonalization
                                    ? Icons.check
                                    : Icons.edit,
                              ),
                        label: Text(
                          _isEditingPersonalization ? 'Save' : 'Edit',
                        ),
                      ),
              ],
            ),
            const SizedBox(height: 12),
                row(
                  icon: Icons.thumb_up_alt_outlined,
                  label: 'Likes',
                  controller: _likesController,
                ),
                const SizedBox(height: 12),
                row(
                  icon: Icons.thumb_down_alt_outlined,
                  label: 'Dislikes',
                  controller: _dislikesController,
                ),
                const SizedBox(height: 12),
                row(
                  icon: Icons.repeat_rounded,
                  label: 'Habits',
                  controller: _habitsController,
                ),
                const SizedBox(height: 12),
                row(
                  icon: Icons.warning_amber_outlined,
                  label: 'Phobias',
                  controller: _phobiasController,
                ),
                const SizedBox(height: 12),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(top: 10, right: 8),
                      child: Icon(
                        Icons.chat_bubble_outline,
                        size: 20,
                        color: theme.colorScheme.primary.withOpacity(0.8),
                      ),
                    ),
                    Expanded(
                      child: _isEditingPersonalization && canEdit
                          ? DropdownButtonFormField<String>(
                              value: _preferredCommunicationMethod,
                              items: const [
                                DropdownMenuItem(
                                  value: 'verbal',
                                  child: Text('Verbal'),
                                ),
                                DropdownMenuItem(
                                  value: 'visual',
                                  child: Text('Visual'),
                                ),
                                DropdownMenuItem(
                                  value: 'written',
                                  child: Text('Written'),
                                ),
                                DropdownMenuItem(
                                  value: 'gesture',
                                  child: Text('Gesture'),
                                ),
                              ],
                              onChanged: (v) => setState(
                                () => _preferredCommunicationMethod = v,
                              ),
                              decoration:
                                  deco('Preferred communication method'),
                            )
                          : Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Preferred communication method',
                                  style:
                                      theme.textTheme.bodySmall?.copyWith(
                                    color: Colors.grey.shade700,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  (_preferredCommunicationMethod ?? '')
                                          .isEmpty
                                      ? 'Not set'
                                      : _preferredCommunicationMethod!
                                          .substring(0, 1)
                                          .toUpperCase() +
                                          _preferredCommunicationMethod!
                                              .substring(1),
                                  style: theme.textTheme.bodyMedium,
                                ),
                              ],
                            ),
                    ),
                  ],
                ),
          ],
        ),
      ),
    );
  }

  /// Fetch medications from the backend API
  Future<void> _fetchMedications() async {
    setState(() {
      _isLoadingMedications = true;
      _medicationError = null;
    });

    try {
      // Parse patientId from String to int
      final patientIdInt = int.tryParse(widget.patientId);

      if (patientIdInt == null) {
        setState(() {
          _isLoadingMedications = false;
          _medicationError = 'Invalid patient ID';
        });
        return;
      }

      final http.Response resp =
          await ApiService.getPatientMedicationsForPatient(patientIdInt);

      if (resp.statusCode == 200) {
        final List<dynamic> data = jsonDecode(resp.body);

        setState(() {
          medications = data.map((json) => Medication.fromJson(json)).toList();
          _isLoadingMedications = false;
        });
      } else {
        setState(() {
          _isLoadingMedications = false;
          _medicationError = 'Failed to load medications: ${resp.statusCode}';
        });
      }
    } catch (e) {
      setState(() {
        _isLoadingMedications = false;
        _medicationError = 'Error loading medications: $e';
      });
    }
  }

  Future<void> _fetchRiskTypes() async {
    setState(() { _isLoadingRisks = true; _risksError = null; });
    try {
      final resp = await ApiService.getRiskTypes();
      if (resp.statusCode == 200) {
        final list = (jsonDecode(resp.body) as List<dynamic>)
            .map((e) => Map<String, dynamic>.from(e as Map))
            .toList();
        setState(() { _riskTypes = list; });
      } else {
        setState(() { _risksError = 'Failed to load risk types'; });
      }
    } catch (e) {
      setState(() { _risksError = 'Error: $e'; });
    }
    if (mounted) setState(() { _isLoadingRisks = false; });
  }

  Future<void> _fetchPatientRisks() async {
    final patientIdInt = int.tryParse(widget.patientId);
    if (patientIdInt == null) return;
    try {
      final resp = await ApiService.getPatientRisks(patientIdInt);
      if (resp.statusCode == 200) {
        final list = jsonDecode(resp.body) as List<dynamic>;
        final map = <int, int>{};
        for (final e in list) {
          final m = e as Map<String, dynamic>;
          final rtId = m['riskTypeId'] is int ? m['riskTypeId'] as int : int.tryParse(m['riskTypeId'].toString());
          final id = m['id'] is int ? m['id'] as int : int.tryParse(m['id'].toString());
          if (rtId != null && id != null) map[rtId] = id;
        }
        setState(() { _riskIdByTypeId = map; });
      }
    } catch (_) {}
  }

  /// Flagged risk names for the persistent alert banner (derived from _riskTypes + _riskIdByTypeId).
  List<String> get _flaggedRiskNames {
    final names = <String>[];
    for (final rt in _riskTypes) {
      final id = rt['id'] is int ? rt['id'] as int : int.tryParse(rt['id'].toString());
      if (id != null && _riskIdByTypeId.containsKey(id)) {
        final name = rt['name']?.toString();
        if (name != null && name.isNotEmpty) names.add(name);
      }
    }
    return names;
  }

  /// Persistent risk alert banner at top of patient profile when client has flagged risks.
  Widget _buildRiskBanner() {
    final names = _flaggedRiskNames;
    if (names.isEmpty) return const SizedBox.shrink();
    final text = 'Known Risks: ${names.join(', ')}';
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
      color: Colors.red.shade700,
      child: SafeArea(
        bottom: false,
        child: Row(
          children: [
            const Icon(Icons.warning_amber_rounded, color: Colors.white, size: 24),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                text,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Tappable card in In-home: "Dashboards" → opens dashboards hub (Competency Trends, etc.).
  Widget _buildDashboardsCard(String patientName) {
    final theme = Theme.of(context);
    final clientId = int.tryParse(widget.patientId);
    if (clientId == null) return const SizedBox.shrink();
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: InkWell(
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute<void>(
              builder: (context) => DashboardsScreen(
                clientId: clientId,
                clientName: patientName,
              ),
            ),
          );
        },
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Icon(Icons.dashboard_outlined, color: theme.colorScheme.primary, size: 28),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      'Dashboards',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'View competency trends and other reports',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: theme.colorScheme.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }

  /// Read-only Audit Log viewer for supervisors/caregivers.
  Widget _buildAuditLogCard(String patientName) {
    final theme = Theme.of(context);
    final clientId = int.tryParse(widget.patientId);
    if (clientId == null) return const SizedBox.shrink();
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: InkWell(
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute<void>(
              builder: (context) => AuditLogScreen(
                clientId: clientId,
                clientName: patientName,
              ),
            ),
          );
        },
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Icon(Icons.fact_check_outlined, color: theme.colorScheme.primary, size: 28),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      'Audit Log',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'View read-only history of caregiver-entered records',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: theme.colorScheme.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }

  /// Tappable card in the Health tab: "ADL/IADL management" → opens hub to manage ADL & IADL.
  Widget _buildAdlIadlManagementCard(String patientName) {
    final theme = Theme.of(context);
    final clientId = int.tryParse(widget.patientId);
    if (clientId == null) return const SizedBox.shrink();
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: InkWell(
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute<void>(
              builder: (context) => AdlIadlManagementScreen(
                clientId: clientId,
                clientName: patientName,
              ),
            ),
          );
        },
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Icon(Icons.self_improvement, color: theme.colorScheme.primary, size: 28),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      'ADL / IADL management',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Log and manage daily living activities for this client',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: theme.colorScheme.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBehaviorLogCard(String patientName) {
    final theme = Theme.of(context);
    final clientId = int.tryParse(widget.patientId);
    if (clientId == null) return const SizedBox.shrink();
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: InkWell(
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute<void>(
              builder: (context) => BehavioralIncidentFormScreen(
                clientId: clientId,
                clientName: patientName,
              ),
            ),
          );
        },
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Icon(Icons.report_problem_outlined, color: theme.colorScheme.primary, size: 28),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      'Log Behavior',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Document behavioral incidents during visits',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: theme.colorScheme.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBehaviorHistoryCard(String patientName) {
    final theme = Theme.of(context);
    final clientId = int.tryParse(widget.patientId);
    if (clientId == null) return const SizedBox.shrink();
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: InkWell(
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute<void>(
              builder: (context) => BehavioralIncidentHistoryScreen(
                clientId: clientId,
                clientName: patientName,
              ),
            ),
          );
        },
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Icon(Icons.history, color: theme.colorScheme.primary, size: 28),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      'Behavioral history',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'View prior behavioral incident logs',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: theme.colorScheme.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildIncidentReportsCard(String patientName) {
    final theme = Theme.of(context);
    final clientId = int.tryParse(widget.patientId);
    if (clientId == null) return const SizedBox.shrink();
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: InkWell(
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute<void>(
              builder: (context) => IncidentReportWizardScreen(
                clientId: clientId,
                clientName: patientName,
              ),
            ),
          );
        },
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Icon(Icons.report, color: theme.colorScheme.primary, size: 28),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      'File incident report',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Record structured medical or risk incidents',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: theme.colorScheme.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildIncidentHistoryCard(String patientName) {
    final theme = Theme.of(context);
    final clientId = int.tryParse(widget.patientId);
    if (clientId == null) return const SizedBox.shrink();
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: InkWell(
        onTap: () {
          Navigator.of(context).push(
            MaterialPageRoute<void>(
              builder: (context) => IncidentReportHistoryScreen(
                clientId: clientId,
                clientName: patientName,
              ),
            ),
          );
        },
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Icon(Icons.history, color: theme.colorScheme.primary, size: 28),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      'Incident report history',
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'View structured incident reports for this client',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: theme.colorScheme.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildKnownRisksCard() {
    final theme = Theme.of(context);
    final canEdit = widget.isCaregiver;

    if (_isLoadingRisks && _riskTypes.isEmpty) {
      return Card(
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: _risksError != null
              ? Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text('Known Risks', style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    Text(_risksError!, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
                  ],
                )
              : const Center(child: CircularProgressIndicator()),
        ),
      );
    }

    // View mode: show only selected risks + Edit button
    if (!_isEditingRisks) {
      final selectedNames = <String>[];
      for (final rt in _riskTypes) {
        final id = rt['id'] is int ? rt['id'] as int : int.tryParse(rt['id'].toString());
        if (id != null && _riskIdByTypeId.containsKey(id)) {
          selectedNames.add(rt['name']?.toString() ?? '');
        }
      }
      return Card(
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(Icons.warning_amber_rounded, color: theme.colorScheme.primary, size: 22),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Known Risks',
                      style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                    ),
                  ),
                  if (canEdit)
                    ElevatedButton.icon(
                      onPressed: () {
                        setState(() {
                          _isEditingRisks = true;
                          _editingCheckedTypeIds = _riskIdByTypeId.keys.toSet();
                        });
                      },
                      icon: const Icon(Icons.edit, size: 18),
                      label: const Text('Edit'),
                    ),
                ],
              ),
              const SizedBox(height: 12),
              if (selectedNames.isEmpty)
                Text('No risks flagged.', style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant))
              else
                ...selectedNames.map((name) => Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: Row(
                    children: [
                      Icon(Icons.check_circle, size: 18, color: theme.colorScheme.primary),
                      const SizedBox(width: 8),
                      Expanded(child: Text(name, style: theme.textTheme.bodyMedium)),
                    ],
                  ),
                )),
            ],
          ),
        ),
      );
    }

    // Edit mode: all checkboxes + Save button
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.warning_amber_rounded, color: theme.colorScheme.primary, size: 22),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Known Risks',
                    style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                  ),
                ),
                if (canEdit)
                  ElevatedButton.icon(
                    onPressed: _isSavingRisks ? null : () => _saveKnownRisks(),
                    icon: _isSavingRisks
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.check, size: 18),
                    label: const Text('Save'),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            if (_riskTypes.isEmpty && !_isLoadingRisks)
              Text('No risk types available.', style: theme.textTheme.bodySmall)
            else
              ..._riskTypes.map((rt) {
                final id = rt['id'] is int ? rt['id'] as int : int.tryParse(rt['id'].toString());
                final name = rt['name']?.toString() ?? '';
                if (id == null) return const SizedBox.shrink();
                final isChecked = _editingCheckedTypeIds.contains(id);
                return CheckboxListTile(
                  value: isChecked,
                  onChanged: (value) {
                    setState(() {
                      if (value == true) {
                        _editingCheckedTypeIds.add(id);
                      } else {
                        _editingCheckedTypeIds.remove(id);
                      }
                    });
                  },
                  title: Text(name),
                  controlAffinity: ListTileControlAffinity.leading,
                  contentPadding: EdgeInsets.zero,
                );
              }),
          ],
        ),
      ),
    );
  }

  Future<void> _saveKnownRisks() async {
    final patientIdInt = int.tryParse(widget.patientId);
    if (patientIdInt == null || !widget.isCaregiver) return;

    setState(() => _isSavingRisks = true);
    try {
      final toAdd = _editingCheckedTypeIds.where((id) => !_riskIdByTypeId.containsKey(id)).toList();
      final toRemove = _riskIdByTypeId.keys.where((id) => !_editingCheckedTypeIds.contains(id)).toList();

      for (final riskTypeId in toAdd) {
        final res = await ApiService.flagPatientRisk(patientIdInt, riskTypeId);
        if (res.statusCode != 201 && res.statusCode != 200) {
          if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed to add risk: ${res.statusCode}')));
          return;
        }
      }
      for (final riskTypeId in toRemove) {
        final riskId = _riskIdByTypeId[riskTypeId];
        if (riskId != null) {
          final res = await ApiService.unflagPatientRisk(patientIdInt, riskId);
          if (res.statusCode != 204 && res.statusCode != 200) {
            if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed to remove risk: ${res.statusCode}')));
            return;
          }
        }
      }

      await _fetchPatientRisks();
      if (mounted) {
        setState(() {
          _isEditingRisks = false;
          _isSavingRisks = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Known risks saved')));
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isSavingRisks = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Save failed: $e')));
      }
    }
  }

  /// Build the medications section with loading/error handling
  Widget _buildMedicationsSection() {
    if (_isLoadingMedications) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32.0),
          child: CircularProgressIndicator(
            color: Theme.of(context).colorScheme.primary,
          ),
        ),
      );
    }

    if (_medicationError != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.error_outline,
                color: Theme.of(context).colorScheme.error,
                size: 48,
              ),
              const SizedBox(height: 16),
              Text(
                _medicationError!,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Theme.of(context).colorScheme.error,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: _fetchMedications,
                icon: const Icon(Icons.refresh),
                label: const Text('Retry'),
              ),
            ],
          ),
        ),
      );
    }

    // Get caregiverId from user provider
    final caregiverId = Provider.of<UserProvider>(
      context,
      listen: false,
    ).user?.caregiverId;

    return CurrentMedicationsSection(
      entries: medications,
      onMedicationUpdated: _fetchMedications,
      // Refresh medications after delete/approve
      caregiverId: caregiverId,
    );
  }

  @override
  Widget build(BuildContext context) {
    // TODO: Fetch from your store/service using patientId.
    // For now, demo data mirrors your mockups (Sarah Johnson).
    final patientName = _patientProfile == null
        ? 'Patient'
        : '${_patientProfile?['firstName'] ?? ''} ${_patientProfile?['lastName'] ?? ''}'
            .trim();
    final mrn = (_patientProfile?['maNumber'] ?? _patientProfile?['mrn'] ?? '')
        .toString()
        .trim()
        .isEmpty
        ? 'MRN'
        : (_patientProfile?['maNumber'] ?? _patientProfile?['mrn']).toString();

    // --- Mood demo data ---
    final moodEntries = <MoodHistoryEntry>[
      MoodHistoryEntry(
        date: DateTime(2024, 12, 27),
        label: 'Poor',
        score5: 2,
        emoji: '😟',
        note: 'Feeling exhausted and overwhelmed',
      ),
      MoodHistoryEntry(
        date: DateTime(2024, 12, 26),
        label: 'Fair',
        score5: 3,
        emoji: '😐',
        note: 'Better after medication adjustment',
      ),
      MoodHistoryEntry(
        date: DateTime(2024, 12, 25),
        label: 'Poor',
        score5: 2,
        emoji: '😟',
        note: 'Holiday stress affecting mood',
      ),
      MoodHistoryEntry(
        date: DateTime(2024, 12, 24),
        label: 'Good',
        score5: 4,
        emoji: '🙂',
        note: 'Family visit lifted spirits',
      ),
      MoodHistoryEntry(
        date: DateTime(2024, 12, 23),
        label: 'Fair',
        score5: 3,
        emoji: '😐',
      ),
    ];

    // MODEL entries
    final modelSymptomEntries = <model.SymptomEntry>[
      model.SymptomEntry(
        id: 's4',
        date: DateTime(2024, 12, 27),
        name: 'Fatigue, Headache, Joint pain',
        severity: 'Moderate',
        note: 'Symptoms worsened during holiday stress',
      ),
      model.SymptomEntry(
        id: 's3',
        date: DateTime(2024, 12, 25),
        name: 'Fatigue, Nausea',
        severity: 'Severe',
        note: 'Emergency contact needed due to severe symptoms',
      ),
      model.SymptomEntry(
        id: 's2',
        date: DateTime(2024, 12, 23),
        name: 'Mild headache',
        severity: 'Mild',
        note: null, // demo of nullable
      ),
      model.SymptomEntry(
        id: 's1',
        date: DateTime(2024, 12, 21),
        name: 'No symptoms reported',
        severity: 'Mild',
        note: 'Feeling much better, no symptoms reported',
      ),
    ];

    // Convert to the WIDGET type
    final uiSymptomEntries = <sympt.SymptomEntry>[
      for (final s in modelSymptomEntries)
        sympt.SymptomEntry(
          id: s.id,
          date: s.date,
          name: s.name,
          severity: s.severity,
          note: s.note ?? '', // <-- fix: ensure non-null String
        ),
    ];

    // --- Virtual Check-In demo data ---
    final virtualCheckIns = <VirtualCheckIn>[
      VirtualCheckIn(
        id: 'vc1',
        type: CheckInType.routine,
        clinicianName: 'Dr. Sarah Johnson',
        startedAt: DateTime(2024, 12, 4, 10, 30),
        durationMinutes: 15,
        status: CheckInStatus.completed,
        moodLabel: 'Good',
        nextCheckIn: DateTime(2024, 12, 11, 10, 30),
        summary: 'Reviewed medication plan; patient stable and adherent.',
      ),
      VirtualCheckIn(
        id: 'vc2',
        type: CheckInType.followUp,
        clinicianName: 'Nurse Williams',
        startedAt: DateTime(2024, 12, 1, 14, 0),
        durationMinutes: 20,
        status: CheckInStatus.completed,
        moodLabel: 'Fair',
        nextCheckIn: DateTime(2024, 12, 8, 14, 0),
        summary: 'Follow-up on home BP readings; stable overall.',
      ),
      VirtualCheckIn(
        id: 'vc3',
        type: CheckInType.urgent,
        clinicianName: 'Dr. Smith',
        startedAt: DateTime(2024, 11, 28, 9, 0),
        durationMinutes: 10,
        status: CheckInStatus.completed,
        moodLabel: 'Poor',
        nextCheckIn: DateTime(2024, 12, 5, 9, 0),
        summary: 'Urgent check-in for severe headache; symptoms improved.',
      ),
    ];

    // --- Virtual Check-In configuration (popup) ---
    return DefaultTabController(
      length: 4,
      child: Scaffold(
        appBar: _DetailsAppBar(
          title: patientName,
          subtitle: 'Patient Details • $mrn',
        ),
        body: Column(
          children: [
            _buildRiskBanner(),
            PatientHeaderCard(
              fullName: patientName,
              mrn: mrn,
              age: 49,
              sex: 'Female',
              currentMoodLabel: 'Poor',
              currentMoodEmoji: '😟',
              diagnoses: const [
                'Type 2 Diabetes',
                'Hypertension',
                'Chronic Fatigue Syndrome',
              ],
              allergies: const ['Penicillin', 'Shellfish'],
              /*heartRateBpm: 72,
              bpSystolic: 120,
              bpDiastolic: 80,
              oxygenPercent: 98,
              temperatureF: 98.0,*/
              emergencyPhones: const ['+15559876543', '+15552227788'],
              onStartVideoCall: () => _startVideoCall(patientName),
            ),

            // Tab bar row (like your mock)
            const _TabsStrip(),

            // Tab views
            Expanded(
              child: TabBarView(
                children: [
                  // Info
                  ListView(
                    padding: const EdgeInsets.only(top: 12, bottom: 16),
                    children: [
                      ContactInfoCard(
                        phone: '(555) 123-4567',
                        email: 'sarah.johnson@email.com',
                        dateOfBirth: DateTime(1975, 3, 15),
                        addressLine1: '123 Main St',
                        addressLine2: 'Apt 4B',
                        city: 'Springfield',
                        state: 'IL',
                        postalCode: '62701',
                      ),
                      const EmergencyContactCard(
                        contactName: 'Michael Johnson',
                        relationship: 'Spouse',
                        phone: '(555) 987-6543',
                      ),
                      _buildPersonalizationCard(),
                    ],
                  ),

                  // Mood
                  ListView(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    children: [MoodHistorySection(entries: moodEntries)],
                  ),

                  // Health
                  ListView(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    children: [
                      _buildKnownRisksCard(),
                      const SizedBox(height: 8),
                      const PainLevelCard(
                        lastReportedText: '6 hours ago',
                        currentPain: 4,
                        location: 'Lower back',
                        dizziness: 2,
                        fatigue: 7,
                      ),
                      // Recent Symptoms (UI-typed list)
                      sympt.RecentSymptomsSection(entries: uiSymptomEntries),
                      const SizedBox(height: 8),
                      _buildMedicationsSection(),
                    ],
                  ),

                  // In-home
                  ListView(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    children: [
                      // ADL/IADL management
                      if (int.tryParse(widget.patientId) != null)
                        _buildAdlIadlManagementCard(patientName),
                      const SizedBox(height: 8),
                      // Log Behavior
                      if (int.tryParse(widget.patientId) != null)
                        _buildBehaviorLogCard(patientName),
                      const SizedBox(height: 8),
                      // File Incident Report
                      if (int.tryParse(widget.patientId) != null)
                        _buildIncidentReportsCard(patientName),
                      const SizedBox(height: 8),
                      // Dashboards
                      if (int.tryParse(widget.patientId) != null)
                        _buildDashboardsCard(patientName),
                      const SizedBox(height: 8),
                      // Behavioral History
                      if (int.tryParse(widget.patientId) != null)
                        _buildBehaviorHistoryCard(patientName),
                      const SizedBox(height: 8),
                      // Incident Report History
                      if (int.tryParse(widget.patientId) != null)
                        _buildIncidentHistoryCard(patientName),
                      const SizedBox(height: 8),
                      // Audit Log (supervisors/caregivers only)
                      if (int.tryParse(widget.patientId) != null && widget.isCaregiver)
                        _buildAuditLogCard(patientName),
                    ],
                  ),

                  // ---- Virtual Check-In tab ----
                  // ---- Virtual Check-In tab ----
                  ListView(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    children: [
                      VirtualCheckInHistoryCard(
                        entries: virtualCheckIns,
                        showConfigure: widget.isCaregiver, // caregivers only
                        onConfigure: widget.isCaregiver ? () async {
                          // Seed with your current config if you have it:
                          final initialQuestions = <VirtualCheckInQuestion>[];
                          // TODO: replace with your real ID source:
                          final checkInId = 1; // TODO: replace with real patient id


                          final updated = await showModalBottomSheet<List<VirtualCheckInQuestion>?>(
                            context: context,
                            isScrollControlled: true,
                            useSafeArea: true,
                            shape: const RoundedRectangleBorder(
                              borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
                            ),
                            builder: (_) => VirtualCheckInConfigSheet(
                              checkInId: checkInId,          // ✅ required
                              initial: initialQuestions,
                            ),
                          );

                          if (!context.mounted) return;
                          if (updated != null) {
                            // TODO: persist `updated` to backend and refresh UI
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('Virtual check-in configuration saved')),
                            );
                          }
                        } : null, // patients: no button
                      ),
                    ],
                  ),

                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
/// shows back arrow + name + MRN line
class _DetailsAppBar extends StatelessWidget implements PreferredSizeWidget {
  const _DetailsAppBar({
    required this.title,
    required this.subtitle,
  });

  final String title;
  final String subtitle;

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return AppBar(
      elevation: 0,
      backgroundColor: cs.surface,
      iconTheme: IconThemeData(color: cs.onSurface),
      titleSpacing: 0,
      title: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              color: cs.onSurface,
              fontWeight: FontWeight.w700,
            ),
          ),
          Text(
            subtitle,
            style: Theme.of(
              context,
            ).textTheme.bodySmall?.copyWith(color: cs.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

/// The tab buttons row (Info • Mood • Health • Virtual Check-In)
class _TabsStrip extends StatelessWidget {
  const _TabsStrip();     // <-- add const + super.key

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Material(
      color: cs.surface,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(8, 4, 8, 4),
        child: TabBar(
          isScrollable: false,
          labelColor: cs.primary,
          unselectedLabelColor: cs.onSurface.withOpacity(.7),
          indicator: UnderlineTabIndicator(
            borderSide: BorderSide(color: cs.primary, width: 3),
          ),
          tabs: const [
            Tab(text: 'Info', icon: Icon(Icons.info_outline, size: 18)),
            Tab(text: 'Mood', icon: Icon(Icons.favorite_border, size: 18)),
            Tab(text: 'Health', icon: Icon(Icons.health_and_safety_outlined, size: 18)),
            Tab(text: 'In-home', icon: Icon(Icons.home_outlined, size: 18)),
            Tab(text: 'Virtual Check-In', icon: Icon(Icons.video_call_outlined, size: 18)),
          ],
        ),
      ),
    );
  }
}
