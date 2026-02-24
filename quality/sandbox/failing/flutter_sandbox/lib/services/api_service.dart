// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/flutter_sandbox/lib/services/api_service.dart

class ApiService {
  static const String baseUrl = "https://api.careconnect.local";

  String fetchData() {
    // Intentional issue: unused local (analyzer warning)
    final unusedLocal = DateTime.now().toIso8601String();

    // Use it in a harmless way to avoid “unused” if you don’t want that warning:
    // ignore: avoid_print
    print(unusedLocal);

    return "data";
  }
}