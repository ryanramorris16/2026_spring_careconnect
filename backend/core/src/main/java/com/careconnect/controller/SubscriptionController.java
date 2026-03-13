package com.careconnect.controller;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import com.careconnect.model.Subscription;
import com.careconnect.model.Plan;
import com.careconnect.model.User;
import com.careconnect.repository.UserRepository;
import com.careconnect.repository.PlanRepository;
import com.careconnect.repository.SubscriptionRepository;
import com.careconnect.dto.PlanDTO;
import com.careconnect.dto.SubscriptionResponseDTO;
import com.careconnect.service.SubscriptionEnrichmentService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionEnrichmentService subscriptionEnrichmentService;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionController(
        SubscriptionEnrichmentService subscriptionEnrichmentService,
        UserRepository userRepository,
        PlanRepository planRepository,
        SubscriptionRepository subscriptionRepository
    ) {
        this.subscriptionEnrichmentService = subscriptionEnrichmentService;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlanDTO>> listPlans() {
        List<Plan> activePlans = planRepository.findByIsActiveTrue();
        List<PlanDTO> dtos = activePlans.stream()
            .map(p -> new PlanDTO(
                String.valueOf(p.getId()),
                p.getIsActive() != null && p.getIsActive(),
                p.getPriceCents() != null ? p.getPriceCents() : 0,
                "usd",
                p.getBillingPeriod() != null ? p.getBillingPeriod().toLowerCase() : "month",
                1,
                String.valueOf(p.getId()),
                p.getName()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelSubscription(@PathVariable String id) {
        try {
            Long subscriptionId;
            try {
                subscriptionId = Long.parseLong(id);
            } catch (NumberFormatException e) {
                // Try to find by external subscription id
                Subscription sub = subscriptionRepository.findAll().stream()
                    .filter(s -> id.equals(s.getExternalSubscriptionId()) || id.equals(s.getStripeSubscriptionId()))
                    .findFirst()
                    .orElse(null);
                if (sub == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Subscription not found: " + id));
                }
                subscriptionId = sub.getId();
            }

            Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

            sub.setStatus("CANCELLED");
            sub.setCurrentPeriodEnd(null);
            subscriptionRepository.save(sub);

            return ResponseEntity.ok().body(Map.of(
                "message", "Subscription cancelled successfully",
                "subscriptionId", id
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to cancel subscription: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserSubscriptions(@PathVariable Long userId) {
        try {
            List<SubscriptionResponseDTO> subscriptionDTOs = subscriptionEnrichmentService.getEnrichedUserSubscriptions(userId);
            return ResponseEntity.ok(subscriptionDTOs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<?> getUserActiveSubscriptions(@PathVariable Long userId) {
        try {
            List<SubscriptionResponseDTO> subscriptionDTOs = subscriptionEnrichmentService.getEnrichedActiveUserSubscriptions(userId);
            return ResponseEntity.ok(subscriptionDTOs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
