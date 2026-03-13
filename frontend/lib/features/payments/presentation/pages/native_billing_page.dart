import 'package:flutter/material.dart';
import '../../services/native_billing_service.dart';

class NativeBillingPage extends StatefulWidget {
  const NativeBillingPage({super.key});

  @override
  State<NativeBillingPage> createState() => _NativeBillingPageState();
}

class _NativeBillingPageState extends State<NativeBillingPage> {
  final NativeBillingService _billing = NativeBillingService();

  @override
  void initState() {
    super.initState();
    _billing.init();
  }

  @override
  void dispose() {
    _billing.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Native Billing')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            ElevatedButton(
              onPressed: () async {
                try {
                  await _billing.buySubscription('premium_monthly', userId: 0);
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Purchase started')));
                } catch (e) {
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: $e')));
                }
              },
              child: const Text('Buy Premium Monthly'),
            ),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: () async {
                try {
                  await _billing.buySubscription('standard_monthly', userId: 0);
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Purchase started')));
                } catch (e) {
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: $e')));
                }
              },
              child: const Text('Buy Standard Monthly'),
            ),
          ],
        ),
      ),
    );
  }
}
