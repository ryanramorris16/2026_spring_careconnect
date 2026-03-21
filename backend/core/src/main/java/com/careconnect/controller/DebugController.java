package com.careconnect.controller;

import com.careconnect.security.Permission;
import com.careconnect.security.RequirePermission;
import com.careconnect.model.Plan;
import com.careconnect.model.User;
import com.careconnect.repository.PlanRepository;
import com.careconnect.security.AuthorizationService;
import com.careconnect.security.UnauthorizedException;
import com.careconnect.service.SubscriptionEnrichmentService;
import com.careconnect.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/debug")
@RequiredArgsConstructor
public class DebugController {

  private static final String MESSAGE_KEY = "message";
  private static final String PREMIUM_PLAN_NAME = "Premium Plan";
  private static final String STANDARD_PLAN_NAME = "Standard Plan";
  private static final String ERROR_KEY = "error";

  private final PlanRepository planRepository;
  private final SubscriptionEnrichmentService subscriptionEnrichmentService;
  private final SecurityUtil securityUtil;
  private final AuthorizationService authorizationService;

  @Value("${subscription.premium-price-ids:price_1RmqWxELoozGI1YxQql5rsvN}")
    private String premiumPriceIds;

  @Value("${subscription.standard-price-ids:price_standard}")
    private String standardPriceIds;

  @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)
  @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> getAllPlans() throws UnauthorizedException {
    User currentUser = securityUtil.resolveCurrentUser();
    authorizationService.requireAdmin(currentUser);
    List<Plan> plans = planRepository.findAll();
    return ResponseEntity.ok(Map.of(
            "plans", plans,
            "count", plans.size()
        ));
  }

  @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)
  @GetMapping("/plans/match")
    public ResponseEntity<Map<String, Object>> matchPlanToPrice() throws UnauthorizedException {
    User currentUser = securityUtil.resolveCurrentUser();
    authorizationService.requireAdmin(currentUser);
    String priceId = "price_1RmqWxELoozGI1YxQql5rsvN";

    Plan exactPlan = planRepository.findByCode(priceId);

    List<Plan> premiumPlans = planRepository.findByName(PREMIUM_PLAN_NAME);
    Plan premiumPlan = premiumPlans.isEmpty() ? null : premiumPlans.get(0);

    List<Plan> standardPlans = planRepository.findByName(STANDARD_PLAN_NAME);
    Plan standardPlan = standardPlans.isEmpty() ? null : standardPlans.get(0);

    Plan manualMapping = new Plan();
    manualMapping.setCode(priceId);
    manualMapping.setName(PREMIUM_PLAN_NAME);
    manualMapping.setPriceCents(3000);
    manualMapping.setBillingPeriod("MONTH");
    manualMapping.setIsActive(true);

    boolean isPremiumPriceId = Arrays.asList(premiumPriceIds.split(",")).contains(priceId);
    boolean isStandardPriceId = Arrays.asList(standardPriceIds.split(",")).contains(priceId);

    return ResponseEntity.ok(Map.of(
            "priceId", priceId,
            "exactMatch", exactPlan != null ? exactPlan : "No match found",
            "premiumPlan", premiumPlan != null ? premiumPlan : "No Premium Plan found",
            "standardPlan", standardPlan != null ? standardPlan : "No Standard Plan found",
            "suggestedMapping", manualMapping,
            "isPremiumPriceId", isPremiumPriceId,
            "isStandardPriceId", isStandardPriceId,
            "configuredPremiumPriceIds", premiumPriceIds,
            "configuredStandardPriceIds", standardPriceIds
        ));
  }

  @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)
  @GetMapping("/plans/create-mapping")
    public ResponseEntity<Map<String, Object>> createPriceMapping() throws UnauthorizedException {
    User currentUser = securityUtil.resolveCurrentUser();
    authorizationService.requireAdmin(currentUser);
    String priceId = "price_1RmqWxELoozGI1YxQql5rsvN";

    Plan existingPlan = planRepository.findByCode(priceId);
    if (existingPlan != null) {
      return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Mapping already exists",
                "plan", existingPlan
            ));
    }

    List<Plan> premiumPlans = planRepository.findByName(PREMIUM_PLAN_NAME);

    if (!premiumPlans.isEmpty()) {
      Plan premiumPlan = premiumPlans.get(0);
      Plan newPlan = new Plan();
      newPlan.setCode(priceId);
      newPlan.setName(premiumPlan.getName());
      newPlan.setPriceCents(premiumPlan.getPriceCents());
      newPlan.setBillingPeriod(premiumPlan.getBillingPeriod());
      newPlan.setIsActive(true);

      Plan savedPlan = planRepository.save(newPlan);
      return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Created new plan mapping based on existing Premium Plan",
                "originalPlan", premiumPlan,
                "newPlan", savedPlan
            ));
    } else {
      Plan newPlan = new Plan();
      newPlan.setCode(priceId);
      newPlan.setName(PREMIUM_PLAN_NAME);
      newPlan.setPriceCents(3000);
      newPlan.setBillingPeriod("MONTH");
      newPlan.setIsActive(true);

      Plan savedPlan = planRepository.save(newPlan);
      return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Created new Premium Plan",
                "plan", savedPlan
            ));
    }
  }

  @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)
  @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() throws UnauthorizedException {
    User currentUser = securityUtil.resolveCurrentUser();
    authorizationService.requireAdmin(currentUser);
    return ResponseEntity.ok(Map.of(
            "premiumPriceIds", premiumPriceIds,
            "standardPriceIds", standardPriceIds,
            "premiumPriceIdsList", Arrays.asList(premiumPriceIds.split(",")),
            "standardPriceIdsList", Arrays.asList(standardPriceIds.split(","))
        ));
  }

  @RequirePermission(Permission.VIEW_ASSIGNED_PATIENTS)
  @GetMapping("/subscriptions/user/{userId}")
    public ResponseEntity<Object> getEnrichedUserSubscriptions(@PathVariable Long userId) throws UnauthorizedException {
    User currentUser = securityUtil.resolveCurrentUser();
    authorizationService.requireAdmin(currentUser);
    try {
      return ResponseEntity.ok(subscriptionEnrichmentService.getEnrichedUserSubscriptions(userId));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of(
                ERROR_KEY, "Failed to get subscriptions: " + e.getMessage()
            ));
    }
  }
}
