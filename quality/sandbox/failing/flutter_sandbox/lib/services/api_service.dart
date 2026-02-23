class ApiService {
  // Finding #1 — hardcoded credential
  static const String baseUrl = "https://api.careconnect.local";
  static const String apiKey = "SECRET_BACKEND_KEY_999999";

  String fetchData() {
    // Finding #2 — dead code
    if (false) {
      return "never";
    }

    return "data";
  }
}
