import 'dart:async';
import 'dart:io' show Platform;
import 'package:in_app_purchase/in_app_purchase.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class NativeBillingService {
  final InAppPurchase _iap = InAppPurchase.instance;
  StreamSubscription<List<PurchaseDetails>>? _subscription;

  void init() {
    final purchaseUpdated = _iap.purchaseStream;
    _subscription = purchaseUpdated.listen(_onPurchaseUpdated, onDone: () {
      _subscription?.cancel();
    }, onError: (error) {
      // handle error
    });
  }

  Future<void> dispose() async {
    await _subscription?.cancel();
  }

  Future<void> buySubscription(String productId, {required int userId}) async {
    final available = await _iap.isAvailable();
    if (!available) throw Exception('In-app purchases not available');

    final ProductDetailsResponse response = await _iap.queryProductDetails({productId}.toSet());
    if (response.notFoundIDs.isNotEmpty) throw Exception('Product not found: $productId');

    final product = response.productDetails.first;
    final purchaseParam = PurchaseParam(productDetails: product);
    await _iap.buyNonConsumable(purchaseParam: purchaseParam);
  }

  Future<void> _onPurchaseUpdated(List<PurchaseDetails> purchases) async {
    for (final purchase in purchases) {
      try {
        if (purchase.status == PurchaseStatus.purchased || purchase.status == PurchaseStatus.restored) {
          // Verify receipt with backend
          await _verifyPurchaseWithServer(purchase);
          if (purchase.pendingCompletePurchase) await _iap.completePurchase(purchase);
        } else if (purchase.status == PurchaseStatus.error) {
          // handle error
        }
      } catch (e) {
        // log
      }
    }
  }

  Future<void> _verifyPurchaseWithServer(PurchaseDetails purchase) async {
    final receipt = purchase.verificationData.serverVerificationData;
    final source = Platform.isIOS ? 'apple' : (Platform.isAndroid ? 'google' : 'web');
    final uri = Uri.parse('${_backendBaseUrl()}/v1/api/billing/verify/$source');

    final body = {
      'userId': _currentUserId(),
      'platform': source.toUpperCase(),
      'receipt': receipt,
      'productId': purchase.productID,
      'packageName': Platform.isAndroid ? _androidPackageName() : null,
    };

    final resp = await http.post(uri,
        headers: {'Content-Type': 'application/json'}, body: jsonEncode(body));

    if (resp.statusCode != 200) {
      throw Exception('Receipt verification failed: ${resp.body}');
    }
  }

  // Helpers - adjust to your auth / config
  String _backendBaseUrl() {
    // Use environment or config in your app; fallback to localhost
    return const String.fromEnvironment('BACKEND_BASE_URL', defaultValue: 'http://localhost:8080');
  }

  int _currentUserId() {
    // Replace with your app's auth user id retrieval
    return int.parse(const String.fromEnvironment('TEST_USER_ID', defaultValue: '0'));
  }

  String _androidPackageName() {
    return const String.fromEnvironment('ANDROID_PACKAGE_NAME', defaultValue: 'com.careconnect.app');
  }
}
