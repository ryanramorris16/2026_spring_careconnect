import 'package:flutter/material.dart';
import '../database/database.dart';
import '../database/sync_service.dart';

class MoodTestScreen extends StatefulWidget {
  const MoodTestScreen({Key? key}) : super(key: key);

  @override
  State<MoodTestScreen> createState() => _MoodTestScreenState();
}

class _MoodTestScreenState extends State<MoodTestScreen> {
  final AppDatabase _database = AppDatabase();
  final SyncService _syncService = SyncService();
  final TextEditingController _noteController = TextEditingController();
  int _selectedMood = 3;
  List<MoodLog> _moodLogs = [];

  @override
  void initState() {
    super.initState();
    _loadMoodLogs();
    _syncService.startAutoSync(); // Start auto-sync every 30 seconds
  }

  Future<void> _loadMoodLogs() async {
    final logs = await _database.getAllMoodLogs();
    setState(() {
      _moodLogs = logs;
    });
  }

  Future<void> _saveMoodLog() async {
    await _database.insertMoodLog(
      moodScore: _selectedMood,
      note: _noteController.text.isEmpty ? null : _noteController.text,
      patientId: 1,
    );
    _noteController.clear();
    setState(() {
      _selectedMood = 3;
    });
    await _loadMoodLogs();

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Mood saved to local database! ✅')),
      );
    }
  }

  Future<void> _deleteMoodLog(String localId) async {
    await _database.deleteMoodLog(localId);
    await _loadMoodLogs();
  }

  Future<void> _syncNow() async {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Syncing...')));
    final count = await _syncService.forceSyncNow();
    if (mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('✅ Synced $count logs to server')));
      await _loadMoodLogs(); // Refresh list to show updated sync status
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mood Tracker (BNS-5)'),
        backgroundColor: Colors.teal,
        actions: [
          IconButton(
            icon: const Icon(Icons.sync),
            tooltip: 'Sync to Server',
            onPressed: _syncNow,
          ),
        ],
      ),
      body: Column(
        children: [
          Container(
            color: Colors.orange.shade100,
            padding: const EdgeInsets.all(12),
            child: const Row(
              children: [
                Icon(Icons.cloud_off, color: Colors.orange),
                SizedBox(width: 8),
                Expanded(
                  child: Text('📴 OFFLINE MODE - SQLite Persistence Active'),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'How are you feeling?',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: List.generate(5, (index) {
                        final mood = index + 1;
                        final emoji = ['😢', '😟', '😐', '🙂', '😄'][index];
                        return GestureDetector(
                          onTap: () => setState(() => _selectedMood = mood),
                          child: Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: _selectedMood == mood
                                  ? Colors.teal.shade100
                                  : Colors.grey.shade200,
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(
                                color: _selectedMood == mood
                                    ? Colors.teal
                                    : Colors.transparent,
                                width: 2,
                              ),
                            ),
                            child: Column(
                              children: [
                                Text(
                                  emoji,
                                  style: const TextStyle(fontSize: 32),
                                ),
                                Text(
                                  '$mood',
                                  style: const TextStyle(fontSize: 12),
                                ),
                              ],
                            ),
                          ),
                        );
                      }),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: _noteController,
                      decoration: const InputDecoration(
                        labelText: 'Note (optional)',
                        border: OutlineInputBorder(),
                      ),
                      maxLines: 2,
                    ),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton.icon(
                        onPressed: _saveMoodLog,
                        icon: const Icon(Icons.save),
                        label: const Text('Save Mood'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.teal,
                          padding: const EdgeInsets.all(16),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          Expanded(
            child: _moodLogs.isEmpty
                ? const Center(
                    child: Text(
                      'No mood logs yet.\nAdd one above! 👆',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 16, color: Colors.grey),
                    ),
                  )
                : ListView.builder(
                    itemCount: _moodLogs.length,
                    itemBuilder: (context, index) {
                      final log = _moodLogs[index];
                      final emoji = [
                        '😢',
                        '😟',
                        '😐',
                        '🙂',
                        '😄',
                      ][log.moodScore - 1];
                      final statusColor = log.syncStatus == 'SYNCED'
                          ? Colors.green
                          : log.syncStatus == 'CONFLICT'
                          ? Colors.red
                          : Colors.orange;
                      return Card(
                        margin: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 4,
                        ),
                        child: ListTile(
                          leading: Text(
                            emoji,
                            style: const TextStyle(fontSize: 32),
                          ),
                          title: Text('Mood: ${log.moodScore}/5'),
                          subtitle: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              if (log.note != null) Text(log.note!),
                              const SizedBox(height: 4),
                              Row(
                                children: [
                                  Icon(
                                    log.syncStatus == 'SYNCED'
                                        ? Icons.cloud_done
                                        : log.syncStatus == 'CONFLICT'
                                        ? Icons.error
                                        : Icons.cloud_upload,
                                    size: 14,
                                    color: statusColor,
                                  ),
                                  const SizedBox(width: 4),
                                  Text(
                                    log.syncStatus,
                                    style: TextStyle(
                                      fontSize: 11,
                                      color: statusColor,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    log.lastModified.toString().substring(
                                      0,
                                      16,
                                    ),
                                    style: const TextStyle(
                                      fontSize: 10,
                                      color: Colors.grey,
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                          trailing: IconButton(
                            icon: const Icon(Icons.delete, color: Colors.red),
                            onPressed: () => _deleteMoodLog(log.localId),
                          ),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _noteController.dispose();
    _syncService.stopAutoSync();
    super.dispose();
  }
}
