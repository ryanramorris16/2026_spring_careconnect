package com.careconnect.controller;

import com.careconnect.dto.BillingQuoteRequest;
import com.careconnect.dto.BillingQuoteResponse;
import com.careconnect.model.*;
import com.careconnect.repository.PlanRepository;
import com.careconnect.repository.SubscriptionRepository;
import com.careconnect.repository.UserRepository;
import com.careconnect.service.PaymentService;
import com.careconnect.service.TaxCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/api/billing")
@Tag(name = "Billing", description = "Billing and subscription endpoints")
public class BillingQuoteController {

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaxCalculationService taxCalculationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @PostMapping("/quote")
    @Operation(
        summary = "Get billing quote with tax breakdown",
        description = "Calculate subtotal, taxes, and total for a subscription tier based on user's address/state",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Billing quote request with tier ID and optional state",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BillingQuoteRequest.class),
                examples = @ExampleObject(
                    name = "Quote Request Example",
                    value = """
                    {
                        "tierId": 1,
                        "userId": 123,
                        "state": "CA"
                    }
                    """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quote calculated",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "tierId": 1,
                        "tierName": "Premium Monthly",
                        "subtotalCents": 2000,
                        "taxCents": 145,
                        "totalCents": 2145,
                        "currency": "USD",
                        "taxRate": 0.0725,
                        "taxJurisdiction": "CA"
                    }
                    """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request or missing data")
    })
    public ResponseEntity<BillingQuoteResponse> getQuote(@RequestBody BillingQuoteRequest request) {
        try {
            Plan plan = planRepository.findById(request.getTierId()).orElse(null);
            if (plan == null) {
                return ResponseEntity.badRequest().body(
                    BillingQuoteResponse.builder().errorMessage("Tier not found").build()
                );
            }

            String state = request.getState();
            if (state == null || state.trim().isEmpty()) {
                if (request.getUserId() != null) {
                    User user = userRepository.findById(request.getUserId()).orElse(null);
                    if (user != null && user.getState() != null) {
                        state = user.getState();
                    }
                }
            }

            if (state == null || state.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    BillingQuoteResponse.builder()
                        .tierId(request.getTierId())
                        .tierName(plan.getName())
                        .errorMessage("State not provided and user address not found")
                        .build()
                );
            }

            Long subtotalCents = plan.getPriceCents().longValue();
            Double taxRate = taxCalculationService.getTaxRateByState(state);
            Long taxCents = taxCalculationService.calculateTaxCents(subtotalCents, taxRate);
            Long totalCents = subtotalCents + taxCents;

            BillingQuoteResponse response = BillingQuoteResponse.builder()
                .tierId(plan.getId())
                .tierName(plan.getName())
                .subtotalCents(subtotalCents)
                .taxCents(taxCents)
                .totalCents(totalCents)
                .currency("USD")
                .taxRate(taxRate)
                .taxJurisdiction(state)
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                BillingQuoteResponse.builder()
                    .errorMessage("Error calculating quote: " + e.getMessage())
                    .build()
            );
        }
    }

    @PostMapping("/pay/google")
    @Operation(summary = "Process Google Pay payment", description = "Accept a Google Pay token, record Payment and Subscription in DB")
    @ApiResponse(responseCode = "200", description = "Payment processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payment request")
    public ResponseEntity<Map<String, Object>> processGooglePayment(
            @RequestBody Map<String, Object> paymentRequest) {
        return processWalletPayment(paymentRequest, BillingPlatform.GOOGLE);
    }

    @PostMapping("/pay/apple")
    @Operation(summary = "Process Apple Pay payment", description = "Accept an Apple Pay token, record Payment and Subscription in DB")
    @ApiResponse(responseCode = "200", description = "Payment processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payment request")
    public ResponseEntity<Map<String, Object>> processApplePayment(
            @RequestBody Map<String, Object> paymentRequest) {
        return processWalletPayment(paymentRequest, BillingPlatform.APPLE);
    }

    private ResponseEntity<Map<String, Object>> processWalletPayment(
            Map<String, Object> paymentRequest, BillingPlatform platform) {
        try {
            String token = (String) paymentRequest.get("token");
            Long tierId = ((Number) paymentRequest.get("tierId")).longValue();
            String state = (String) paymentRequest.getOrDefault("state", "CA");
            Long userId = paymentRequest.containsKey("userId") && paymentRequest.get("userId") != null
                    ? ((Number) paymentRequest.get("userId")).longValue()
                    : null;

            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Payment token is required"));
            }

            Plan plan = planRepository.findById(tierId).orElse(null);
            if (plan == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid subscription tier"));
            }

            Long subtotalCents = plan.getPriceCents().longValue();
            Double taxRate = taxCalculationService.getTaxRateByState(state);
            Long taxCents = taxCalculationService.calculateTaxCents(subtotalCents, taxRate);
            Long totalCents = subtotalCents + taxCents;

            String transactionId = platform.name().toLowerCase() + "_" + System.currentTimeMillis();

            User user = null;
            if (userId != null && userId > 0) {
                user = userRepository.findById(userId).orElse(null);
            }

            Subscription subscription = new Subscription();
            subscription.setStripeSubscriptionId(transactionId);
            subscription.setPlatform(platform);
            subscription.setExternalSubscriptionId(transactionId);
            subscription.setStatus("ACTIVE");
            subscription.setStartedAt(Instant.now());
            subscription.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
            subscription.setPlan(plan);
            if (user != null) {
                subscription.setUser(user);
            }
            subscriptionRepository.save(subscription);

            Payment payment = Payment.builder()
                    .platform(platform)
                    .platformPurchaseToken(token)
                    .externalTransactionId(transactionId)
                    .amountCents(totalCents.intValue())
                    .status("SUCCEEDED")
                    .attemptedAt(Instant.now())
                    .subscription(subscription)
                    .user(user)
                    .build();
            paymentService.savePayment(payment);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", platform.name() + " Pay payment processed successfully");
            result.put("transactionId", transactionId);
            result.put("subscriptionId", subscription.getId());
            result.put("amount", totalCents / 100.0);
            result.put("planName", plan.getName());
            result.put("currency", "USD");
            result.put("status", "ACTIVE");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Payment processing failed: " + e.getMessage()));
        }
    }
}
