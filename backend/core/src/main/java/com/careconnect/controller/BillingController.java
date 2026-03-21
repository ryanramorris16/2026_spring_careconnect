package com.careconnect.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.careconnect.dto.BillingVerifyRequest;
import com.careconnect.dto.BillingVerifyResponse;
import com.careconnect.service.AppleBillingService;
import com.careconnect.service.GoogleBillingService;
import org.springframework.web.bind.annotation.RequestHeader;
import com.careconnect.security.JwtTokenProvider;

@RestController
@RequestMapping("/v1/api/billing")
public class BillingController {

  private final AppleBillingService appleBillingService;
  private final GoogleBillingService googleBillingService;
  private final com.careconnect.service.PaymentService paymentService;
  private final com.careconnect.repository.SubscriptionRepository subscriptionRepository;
  private final com.careconnect.repository.UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;

  @Autowired
    public BillingController(AppleBillingService appleBillingService,
                             GoogleBillingService googleBillingService,
                             com.careconnect.service.PaymentService paymentService,
                             com.careconnect.repository.SubscriptionRepository subscriptionRepository,
                             com.careconnect.repository.UserRepository userRepository,
                             JwtTokenProvider jwtTokenProvider) {
    this.appleBillingService = appleBillingService;
    this.googleBillingService = googleBillingService;
    this.paymentService = paymentService;
    this.subscriptionRepository = subscriptionRepository;
    this.userRepository = userRepository;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @PostMapping("/verify/apple")
    public ResponseEntity<?> verifyApple(@RequestBody BillingVerifyRequest request,
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
    try {
      com.careconnect.model.User resolvedUser = null;
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        try {
          if (jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            resolvedUser = userRepository.findByEmail(email).orElse(null);
          }
        } catch (Exception ex) {
                    // ignore and proceed without resolved user
        }
      }

      BillingVerifyResponse resp = appleBillingService.verifyReceipt(request);

      com.careconnect.model.Payment p = com.careconnect.model.Payment.builder()
                    .platform(com.careconnect.model.BillingPlatform.APPLE)
                    .platformPurchaseToken(request.getReceipt())
                    .platformPayerId(resp.getExternalTransactionId())
                    .externalTransactionId(resp.getExternalTransactionId())
                    .status(resp.isSuccess() ? "SUCCEEDED" : "FAILED")
                    .amountCents(null)
                    .attemptedAt(resp.getPurchaseDate())
                    .build();

      paymentService.savePayment(p);

      com.careconnect.model.User user = resolvedUser;
      if (user == null && request.getUserId() != null) {
        user = userRepository.findById(request.getUserId()).orElse(null);
      }

      if (user != null) {
        com.careconnect.model.Subscription sub = null;
        if (resp.getExternalSubscriptionId() != null) {
          java.util.Optional<com.careconnect.model.Subscription> existing = subscriptionRepository.findAll().stream()
                            .filter(s -> resp.getExternalSubscriptionId().equals(s.getExternalSubscriptionId()))
                            .findFirst();
          if (existing.isPresent()) {
            sub = existing.get();
          }
        }
        if (sub == null) sub = new com.careconnect.model.Subscription();
        sub.setUser(user);
        sub.setPlatform(com.careconnect.model.BillingPlatform.APPLE);
        sub.setExternalSubscriptionId(resp.getExternalSubscriptionId());
        sub.setStatus(resp.getStatus());
        sub.setStartedAt(resp.getPurchaseDate());
        sub.setCurrentPeriodEnd(resp.getExpiryDate());
        sub.setLastValidatedAt(java.time.Instant.now());
        subscriptionRepository.save(sub);
        p.setSubscription(sub);
        paymentService.savePayment(p);
      }

      return ResponseEntity.ok(resp);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/verify/google")
    public ResponseEntity<?> verifyGoogle(@RequestBody BillingVerifyRequest request,
                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
    try {
      com.careconnect.model.User resolvedUser = null;
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        try {
          if (jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            resolvedUser = userRepository.findByEmail(email).orElse(null);
          }
        } catch (Exception ex) {
                    // ignore and continue
        }
      }

      BillingVerifyResponse resp = googleBillingService.verifyReceipt(request);

      com.careconnect.model.Payment p = com.careconnect.model.Payment.builder()
                    .platform(com.careconnect.model.BillingPlatform.GOOGLE)
                    .platformPurchaseToken(request.getReceipt())
                    .platformPayerId(resp.getExternalTransactionId())
                    .externalTransactionId(resp.getExternalTransactionId())
                    .status(resp.isSuccess() ? "SUCCEEDED" : "FAILED")
                    .amountCents(null)
                    .attemptedAt(resp.getPurchaseDate())
                    .build();

      paymentService.savePayment(p);

      com.careconnect.model.User user = resolvedUser;
      if (user == null && request.getUserId() != null) {
        user = userRepository.findById(request.getUserId()).orElse(null);
      }

      if (user != null) {
        com.careconnect.model.Subscription sub = null;
        if (resp.getExternalSubscriptionId() != null) {
          java.util.Optional<com.careconnect.model.Subscription> existing = subscriptionRepository.findAll().stream()
                            .filter(s -> resp.getExternalSubscriptionId().equals(s.getExternalSubscriptionId()))
                            .findFirst();
          if (existing.isPresent()) {
            sub = existing.get();
          }
        }
        if (sub == null) sub = new com.careconnect.model.Subscription();
        sub.setUser(user);
        sub.setPlatform(com.careconnect.model.BillingPlatform.GOOGLE);
        sub.setExternalSubscriptionId(resp.getExternalSubscriptionId());
        sub.setStatus(resp.getStatus());
        sub.setStartedAt(resp.getPurchaseDate());
        sub.setCurrentPeriodEnd(resp.getExpiryDate());
        sub.setLastValidatedAt(java.time.Instant.now());
        subscriptionRepository.save(sub);
        p.setSubscription(sub);
        paymentService.savePayment(p);
      }

      return ResponseEntity.ok(resp);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }
  }

  // Webhook endpoints (platform server-to-server notifications)
  @PostMapping("/webhook/apple")
    public ResponseEntity<?> appleWebhook(@RequestBody String body, @RequestHeader(value = "Authorization", required = false) String auth) {
    // For production: validate signature and process notification types
    return ResponseEntity.ok(java.util.Map.of("message", "apple webhook received"));
  }

  @PostMapping("/webhook/google")
    public ResponseEntity<?> googleWebhook(@RequestBody String body, @RequestHeader(value = "Authorization", required = false) String auth) {
    // For production: validate signature and process notification types
    return ResponseEntity.ok(java.util.Map.of("message", "google webhook received"));
  }
}
