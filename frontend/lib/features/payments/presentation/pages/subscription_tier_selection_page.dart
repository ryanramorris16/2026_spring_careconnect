import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class SubscriptionTierSelectionPage extends StatefulWidget {
  /// Optional email from signup flow
  final String? email;

  const SubscriptionTierSelectionPage({
    super.key,
    this.email,
  });

  @override
  State<SubscriptionTierSelectionPage> createState() =>
      _SubscriptionTierSelectionPageState();
}

class _SubscriptionTierSelectionPageState
    extends State<SubscriptionTierSelectionPage> {
  String? _selectedTier;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Choose Your Plan'),
        backgroundColor: const Color(0xFF14366E),
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Header
            const SizedBox(height: 16),
            const Text(
              'Select a Subscription Plan',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: Color(0xFF14366E),
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            const Text(
              'Choose the plan that works best for you',
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),

            // Free Tier
            _buildTierCard(
              title: 'Free',
              price: '\$0',
              period: '/month',
              features: [
                'Basic health tracking',
                'Limited storage',
                'Community support',
              ],
              tierId: 'free',
              color: Colors.grey[200]!,
              onTap: () => _selectTier('free'),
              isSelected: _selectedTier == 'free',
            ),
            const SizedBox(height: 16),

            // Standard Tier
            _buildTierCard(
              title: 'Standard',
              price: '\$9.99',
              period: '/month',
              features: [
                'All Free features',
                'Unlimited storage',
                'Priority email support',
                'Advanced analytics',
              ],
              tierId: 'standard_monthly',
              color: const Color(0xFF14366E),
              isSelected: _selectedTier == 'standard_monthly',
              onTap: () => _selectTier('standard_monthly'),
              isPopular: true,
            ),
            const SizedBox(height: 16),

            // Premium Tier
            _buildTierCard(
              title: 'Premium',
              price: '\$29.99',
              period: '/month',
              features: [
                'All Standard features',
                'Video consultations',
                'Personal health coach',
                'Priority phone support',
                'Custom health plans',
              ],
              tierId: 'premium_monthly',
              color: Colors.amber[700]!,
              isSelected: _selectedTier == 'premium_monthly',
              onTap: () => _selectTier('premium_monthly'),
            ),
            const SizedBox(height: 32),

            // Continue Button
            ElevatedButton(
              onPressed: _selectedTier != null ? _continueToPayment : null,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF14366E),
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              child: const Text(
                'Continue to Payment',
                style: TextStyle(
                  fontSize: 16,
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  void _selectTier(String tierId) {
    setState(() {
      _selectedTier = tierId;
    });
  }

  void _continueToPayment() {
    if (_selectedTier == null) return;

    // Map tier names to IDs (should match backend Plan IDs)
    final tierIdMap = {
      'free': 1,
      'standard_monthly': 2,
      'premium_monthly': 3,
    };

    final tierId = tierIdMap[_selectedTier] ?? 0;

    // Navigate to payment page (web-pay) with selected tier and tierId
    context.go('/web-pay', extra: {
      'tierId': tierId,
      'tier': _selectedTier,
      'email': widget.email,
    });
  }

  Widget _buildTierCard({
    required String title,
    required String price,
    required String period,
    required List<String> features,
    required String tierId,
    required Color color,
    required VoidCallback onTap,
    required bool isSelected,
    bool isPopular = false,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          border: Border.all(
            color: isSelected ? color : Colors.grey[300]!,
            width: isSelected ? 2 : 1,
          ),
          borderRadius: BorderRadius.circular(12),
          color: isSelected ? color.withOpacity(0.05) : Colors.white,
        ),
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Popular badge
            if (isPopular)
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: color,
                  borderRadius: BorderRadius.circular(4),
                ),
                child: const Text(
                  'Most Popular',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            if (isPopular) const SizedBox(height: 12),

            // Title and Price
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    RichText(
                      text: TextSpan(
                        children: [
                          TextSpan(
                            text: price,
                            style: TextStyle(
                              fontSize: 24,
                              fontWeight: FontWeight.bold,
                              color: color,
                            ),
                          ),
                          TextSpan(
                            text: period,
                            style: const TextStyle(
                              fontSize: 14,
                              color: Colors.grey,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                // Selection indicator
                Container(
                  width: 24,
                  height: 24,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: isSelected ? color : Colors.grey[300]!,
                      width: 2,
                    ),
                    color: isSelected ? color : Colors.transparent,
                  ),
                  child: isSelected
                      ? Icon(
                          Icons.check,
                          color: Colors.white,
                          size: 16,
                        )
                      : null,
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Features list
            ...features.map((feature) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  children: [
                    Icon(
                      Icons.check_circle_outline,
                      size: 18,
                      color: color,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        feature,
                        style: const TextStyle(
                          fontSize: 14,
                          color: Colors.black87,
                        ),
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          ],
        ),
      ),
    );
  }
}
