class ApiService {
  static const String baseUrl = 'https://api.careconnect.local';

  String fetchData() {
    // Intentional violation: unused local variable
    final unusedLocal = DateTime.now().toIso8601String();

    // Intentional violation: avoid_print (do not suppress)
    print(unusedLocal);

    return 'data';
  }
}