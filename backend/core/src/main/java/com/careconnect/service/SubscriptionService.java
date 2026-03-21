package com.careconnect.service;

import com.careconnect.model.Payment;
import com.careconnect.model.Plan;
import com.careconnect.model.Subscription;
import com.careconnect.model.User;
import com.careconnect.repository.PaymentRepository;
import com.careconnect.repository.PlanRepository;
import com.careconnect.repository.UserRepository;
import com.careconnect.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
  private final SubscriptionRepository subscriptionRepository;
  private final PaymentRepository paymentRepository;
  private final UserRepository userRepository;
  private final PlanRepository planRepository;

  public Plan createPlan(String code, String name, Integer priceCents, String billingPeriod, Boolean isActive) {
    Plan plan = new Plan();
    plan.setCode(code);
    plan.setName(name);
    plan.setPriceCents(priceCents);
    plan.setBillingPeriod(billingPeriod);
    plan.setIsActive(isActive != null ? isActive : true);
    return planRepository.save(plan);
  }

  public Plan getPlan(Long planId) {
    return planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + planId));
  }

  @Transactional
    public void cancelSubscription(Long subscriptionId) {
    Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

    sub.setStatus("CANCELLED");
    sub.setCurrentPeriodEnd(null);
    subscriptionRepository.save(sub);
  }

  @Transactional
    public List<Subscription> getUserSubscriptions(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    return subscriptionRepository.findByUser(user);
  }

  @Transactional
    public List<Subscription> getUserActiveSubscriptions(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    return subscriptionRepository.findByUserAndStatus(user, "ACTIVE");
  }

  @Transactional
    public Subscription createSubscriptionForUser(Long userId, Long planId, String platform) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    Plan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found with ID: " + planId));

    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setPlan(plan);
    subscription.setStatus("ACTIVE");
    subscription.setStartedAt(Instant.now());
    subscription.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
    subscription.setStripeSubscriptionId(platform.toLowerCase() + "_" + System.currentTimeMillis());

    return subscriptionRepository.save(subscription);
  }
}
