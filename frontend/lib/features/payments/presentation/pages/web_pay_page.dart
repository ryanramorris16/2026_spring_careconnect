import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:pay/pay.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_dotenv/flutter_dotenv.dart';
import '../../../../services/auth_token_manager.dart';
import '../../../../services/billing_quote_service.dart';
import '../../../../config/app_config.dart';

class WebPayPage extends StatefulWidget {
  final int tierId;
  final String? tier;
  final String? email;

  const WebPayPage({
    super.key,
    this.tierId = 0,
    this.tier,
    this.email,
  });

  @override
  State<WebPayPage> createState() => _WebPayPageState();
}

class _WebPayPageState extends State<WebPayPage> {
  final String backendBase = const String.fromEnvironment('BACKEND_BASE_URL', defaultValue: 'http://localhost:8080');

  PaymentConfiguration? _googleConfig;
  PaymentConfiguration? _appleConfig;
  bool _googleAvailable = false;
  bool _appleAvailable = false;
  
  // Billing quote state
  BillingQuote? _billingQuote;
  bool _loadingQuote = true;
  String? _quoteError;
  
  // Navigation parameters
  late int _tierId;
  String? _email;

  @override
  void initState() {
    super.initState();
    
    // Use tierId from widget constructor
    _tierId = widget.tierId;
    _email = widget.email;
    
    _fetchBillingQuote();
    // Payment config will be set up after quote is fetched
  }

  void _setupPaymentConfigurations() {
    // Configure Apple Pay
    final appleJson = {
      "provider": "apple",
      "data": {
        "merchantIdentifier": "com.lauh.careconnect",
        "displayName": "CareConnect",
        "machineRoundUpChargeAmount": "0.01",
        "merchantCapabilities": [
          "supports3DS",
          "supportsEMV"
        ],
        "supportedCountries": ["US"],
        "supportedNetworks": ["amex", "masterCard", "visa"],
        "requiredShippingAddressFields": [],
        "requiredBillingAddressFields": [],
        "shippingType": "delivery",
        "currencyCode": "USD",
        "countryCode": "US"
      }
    };

    try {
      _appleConfig = PaymentConfiguration.fromJsonString(jsonEncode(appleJson));
      _appleAvailable = true;
      print('✅ Apple Pay configured successfully with merchant ID: com.lauh.careconnect');
    } catch (e) {
      print('❌ Apple Pay configuration failed: $e');
      _appleConfig = null;
      _appleAvailable = false;
    }

    // Configure Google Pay
    final googleJson = {
      "provider": "google",
      "data": {
        "environment": "TEST",  // Use TEST for testing
        "apiVersion": 2,
        "apiVersionMinor": 0,
        "merchantInfo": {
          "merchantName": "CareConnect",
          "merchantId": "12345678901234567890"  // Required for Google Pay
        },
        "allowedPaymentMethods": [
          {
            "type": "CARD",
            "parameters": {
              "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
              "allowedCardNetworks": ["AMEX", "DISCOVER", "INTERAC", "MASTERCARD", "VISA"]
            },
            "tokenizationSpecification": {
              "type": "DIRECT",
              "parameters": {
                "protocolVersion": "ECv1"
              }
            }
          }
        ],
        "transactionInfo": {
          "totalPriceStatus": "FINAL",
          "totalPrice": _billingQuote?.totalDisplay.replaceFirst('\$', '') ?? "0.00",
          "currencyCode": "USD"
        }
      }
    };

    try {
      _googleConfig = PaymentConfiguration.fromJsonString(jsonEncode(googleJson));
      _googleAvailable = true;
      print('✅ Google Pay configured successfully');
    } catch (e) {
      print('❌ Google Pay configuration failed: $e');
      _googleConfig = null;
      _googleAvailable = false;
    }
  }

  Future<void> _fetchBillingQuote() async {
    try {
      final service = BillingQuoteService(backendBase: backendBase);
      // Pass state for tax calculation (default to CA for demo, should come from user address)
      final quote = await service.getQuote(
        tierId: _tierId,
        state: 'CA',  // TODO: Get from user's stored address or let user select
      );
      
      if (mounted) {
        setState(() {
          _billingQuote = quote;
          _loadingQuote = false;
          // Set up payment configurations now that we have the quote amount
          _setupPaymentConfigurations();
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _quoteError = e.toString();
          _loadingQuote = false;
        });
      }
    }
  }

  Future<void> _handlePayResult(Map<String, dynamic> result, String platform, String productId) async {
    // Extract token from result (varies by provider)
    String? token;
    try {
      if (platform == 'google') {
        // Google Pay returns the token in paymentMethodData
        token = result['paymentMethodData']?['tokenizationData']?['token'];
        if (token == null) {
          token = jsonEncode(result['paymentMethodData']);
        }
      } else if (platform == 'apple') {
        token = result['paymentData'] ?? jsonEncode(result);
      }
    } catch (_) {
      token = jsonEncode(result);
    }

    if (token == null || token.isEmpty) {
      throw Exception('Failed to extract payment token');
    }

    // Route to appropriate payment endpoint based on platform
    if (platform == 'google') {
      await _processGooglePayment(token, productId);
    } else if (platform == 'apple') {
      await _processApplePayment(token, productId);
    } else {
      // Legacy endpoint for other platforms
      final uri = Uri.parse('$backendBase/v1/api/billing/verify/${platform}');
      final body = {
        'platform': platform.toUpperCase(),
        'receipt': token,
        'productId': productId,
        'packageName': const String.fromEnvironment('WEB_PACKAGE_NAME', defaultValue: '')
      };

      final headers = await AuthTokenManager.getAuthHeaders();
      final resp = await http.post(uri, headers: headers, body: jsonEncode(body));
      if (resp.statusCode != 200) {
        throw Exception('Verification failed: ${resp.body}');
      }
    }
  }

  Future<void> _processGooglePayment(String token, String productId) async {
    // Call the Google Pay payment endpoint
    final uri = Uri.parse('$backendBase/v1/api/billing/pay/google');
    
    final body = {
      'token': token,
      'tierId': _tierId,
      'state': 'CA',  // TODO: Use actual user state
    };

    try {
      final headers = <String, String>{
        'Content-Type': 'application/json',
      };
      
      // Try to add auth headers if available
      try {
        final authHeaders = await AuthTokenManager.getAuthHeaders();
        headers.addAll(authHeaders);
      } catch (_) {
        // Proceed without auth
      }

      final resp = await http.post(
        uri,
        headers: headers,
        body: jsonEncode(body),
      ).timeout(const Duration(seconds: 10));

      if (resp.statusCode != 200) {
        throw Exception('Payment failed: ${resp.body}');
      }

      final responseBody = jsonDecode(resp.body) as Map<String, dynamic>;
      print('✅ Payment successful: ${responseBody['transactionId']}');
      
    } catch (e) {
      print('❌ Payment error: $e');
      rethrow;
    }
  }

  Future<void> _processApplePayment(String token, String productId) async {
    // Call the Apple Pay payment endpoint
    final uri = Uri.parse('$backendBase/v1/api/billing/pay/apple');
    
    final body = {
      'token': token,
      'tierId': _tierId,
      'state': 'CA',  // TODO: Use actual user state
    };

    try {
      final headers = <String, String>{
        'Content-Type': 'application/json',
      };
      
      // Try to add auth headers if available
      try {
        final authHeaders = await AuthTokenManager.getAuthHeaders();
        headers.addAll(authHeaders);
      } catch (_) {
        // Proceed without auth
      }

      final resp = await http.post(
        uri,
        headers: headers,
        body: jsonEncode(body),
      ).timeout(const Duration(seconds: 10));

      if (resp.statusCode != 200) {
        throw Exception('Apple Pay payment failed: ${resp.body}');
      }

      final responseBody = jsonDecode(resp.body) as Map<String, dynamic>;
      print('✅ Apple Pay successful: ${responseBody['transactionId']}');
      
    } catch (e) {
      print('❌ Apple Pay error: $e');
      rethrow;
    }
  }

  Future<void> _processApplePayment(String token, String productId) async {
    // Call the Apple Pay payment endpoint
    final uri = Uri.parse('$backendBase/v1/api/billing/pay/apple');
    
    final body = {
      'token': token,
      'tierId': _tierId,
      'state': 'CA',  // TODO: Use actual user state
    };

    try {
      final headers = <String, String>{
        'Content-Type': 'application/json',
      };
      
      // Try to add auth headers if available
      try {
        final authHeaders = await AuthTokenManager.getAuthHeaders();
        headers.addAll(authHeaders);
      } catch (_) {
        // Proceed without auth
      }

      final resp = await http.post(
        uri,
        headers: headers,
        body: jsonEncode(body),
      ).timeout(const Duration(seconds: 10));

      if (resp.statusCode != 200) {
        throw Exception('Apple Pay payment failed: ${resp.body}');
      }

      final responseBody = jsonDecode(resp.body) as Map<String, dynamic>;
      print('✅ Apple Pay successful: ${responseBody['transactionId']}');
      
    } catch (e) {
      print('❌ Apple Pay error: $e');
      rethrow;
    }
  }

  Future<void> _initiateApplePayment() async {
    try {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Processing Apple Pay...'),
          duration: Duration(seconds: 2),
        ),
      );

      // For demo: simulate Apple Pay token and process
      final demoToken = jsonEncode({
        'paymentData': 'APPLE_PAY_TOKEN_${DateTime.now().millisecondsSinceEpoch}'
      });

      await _processApplePayment(demoToken, _tierId.toString());

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('✅ Apple Pay payment successful!'),
            duration: Duration(seconds: 2),
          ),
        );
        await Future.delayed(const Duration(seconds: 2));
        if (mounted) {
          Navigator.of(context).popUntil((route) => route.isFirst);
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('❌ Apple Pay failed: $e'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 3),
          ),
        );
      }
    }
  }

  Future<void> _initiateGooglePayment() async {
    try {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Processing Google Pay...'),
          duration: Duration(seconds: 2),
        ),
      );

      // For demo: simulate Google Pay token and process
      final demoToken = jsonEncode({
        'paymentMethodData': {
          'tokenizationData': {
            'token': 'GOOGLE_PAY_TOKEN_${DateTime.now().millisecondsSinceEpoch}'
          }
        }
      });

      await _processGooglePayment(demoToken, _tierId.toString());

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('✅ Google Pay payment successful!'),
            duration: Duration(seconds: 2),
          ),
        );
        await Future.delayed(const Duration(seconds: 2));
        if (mounted) {
          Navigator.of(context).popUntil((route) => route.isFirst);
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('❌ Google Pay failed: $e'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 3),
          ),
        );
      }
    }
  }

  Widget _buildPaymentButton({
    required String label,
    required IconData icon,
    required VoidCallback onPressed,
  }) {
    return ElevatedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon, size: 24),
      label: Text(label),
      style: ElevatedButton.styleFrom(
        backgroundColor: const Color(0xFF14366E),
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 24),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
        elevation: 2,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Complete Your Purchase'),
        backgroundColor: const Color(0xFF14366E),
        foregroundColor: Colors.white,
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loadingQuote) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_quoteError != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Colors.red),
            const SizedBox(height: 16),
            Text('Error loading quote: $_quoteError'),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _fetchBillingQuote,
              child: const Text('Retry'),
            ),
          ],
        ),
      );
    }

    if (_billingQuote == null) {
      return const Center(child: Text('No quote available'));
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Order Summary Section
          _buildOrderSummary(),
          const SizedBox(height: 32),

          // Payment Methods
          const Text(
            'Select Payment Method',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Color(0xFF14366E),
            ),
          ),
          const SizedBox(height: 16),

          // Apple Pay Button
          _buildPaymentButton(
            label: 'Apple Pay',
            icon: Icons.apple,
            onPressed: () => _initiateApplePayment(),
          ),
          const SizedBox(height: 12),

          // Google Pay Button
          _buildPaymentButton(
            label: 'Google Pay',
            icon: Icons.payment,
            onPressed: () => _initiateGooglePayment(),
          ),

          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _buildOrderSummary() {
    final quote = _billingQuote!;
    
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey[300]!),
        borderRadius: BorderRadius.circular(12),
        color: Colors.grey[50],
      ),
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Order Summary',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Color(0xFF14366E),
            ),
          ),
          const SizedBox(height: 16),

          // Subscription tier
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                quote.tierName,
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
              ),
              Text(
                quote.subtotalDisplay,
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
              ),
            ],
          ),
          const SizedBox(height: 12),

          // Divider
          Container(
            height: 1,
            color: Colors.grey[300],
            margin: const EdgeInsets.symmetric(vertical: 12),
          ),

          // Taxes
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Taxes (${quote.taxPercentageDisplay})',
                    style: TextStyle(fontSize: 14, color: Colors.grey[600]),
                  ),
                  Text(
                    quote.taxJurisdiction,
                    style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                  ),
                ],
              ),
              Text(
                quote.taxDisplay,
                style: TextStyle(fontSize: 14, color: Colors.grey[600]),
              ),
            ],
          ),
          const SizedBox(height: 12),

          // Divider
          Container(
            height: 1,
            color: Colors.grey[300],
            margin: const EdgeInsets.symmetric(vertical: 12),
          ),

          // Total
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Total',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF14366E),
                ),
              ),
              Text(
                quote.totalDisplay,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF14366E),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            'Currency: ${quote.currency}',
            style: TextStyle(fontSize: 12, color: Colors.grey[500]),
          ),
        ],
      ),
    );
  }
}
