import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Global app configuration for environment variables and settings
class AppConfig {
  /// Google Places API Key for address autocomplete
  static String getGooglePlacesApiKey() {
    return dotenv.env['GOOGLE_PLACES_API_KEY'] ?? '';
  }

  /// Backend base URL
  static String getBackendBaseUrl() {
    return dotenv.env['CC_BASE_URL_WEB'] ?? 'http://localhost:8080';
  }

  /// Apple Merchant ID for Apple Pay
  static String getAppleMerchantId() {
    return dotenv.env['APPLE_MERCHANT_ID'] ?? '';
  }

  /// Google Pay Merchant ID
  static String getGooglePayMerchantId() {
    return dotenv.env['GOOGLE_PAY_MERCHANT_ID'] ?? '';
  }

  /// Check if Google Places API key is configured
  static bool isGooglePlacesConfigured() {
    final key = getGooglePlacesApiKey();
    return key.isNotEmpty && !key.contains('your_google_places_api_key');
  }
}
