package com.careconnect.controller;

import com.careconnect.dto.BillingQuoteRequest;
import com.careconnect.dto.BillingQuoteResponse;
import com.careconnect.model.Plan;
import com.careconnect.model.User;
import com.careconnect.repository.PlanRepository;
import com.careconnect.repository.UserRepository;
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

    /**
     * Get a billing quote (itemized breakdown: subtotal + taxes + total)
     * for a given subscription tier and user location.
     * The state is either provided in request or fetched from user's stored address.
     */
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
        @ApiResponse(responseCode = "400", description = "Invalid request or missing data",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                        "errorMessage": "Tier not found"
                    }
                    """)
            )
        )
    })
    public ResponseEntity<BillingQuoteResponse> getQuote(@RequestBody BillingQuoteRequest request) {
        try {
            // Validate tier exists
            Plan plan = planRepository.findById(request.getTierId()).orElse(null);
            if (plan == null) {
                return ResponseEntity.badRequest().body(
                    BillingQuoteResponse.builder()
                        .errorMessage("Tier not found")
                        .build()
                );
            }

            // Get state from request or user's stored address
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

            // Calculate taxes
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

    /**
     * Process Google Pay payment
     * In production, this would verify the token with Google and process payment through Stripe/Payment Gateway
     * For now, this is a test endpoint that accepts the Google Pay token
     */
    @PostMapping("/pay/google")
    @Operation(
        summary = "Process Google Pay payment",
        description = "Accept and process a Google Pay token"
    )
    @ApiResponse(responseCode = "200", description = "Payment processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payment request")
    public ResponseEntity<Map<String, Object>> processGooglePayment(
            @RequestBody Map<String, Object> paymentRequest) {
        try {
            String token = (String) paymentRequest.get("token");
            Long tierId = ((Number) paymentRequest.get("tierId")).longValue();
            String state = (String) paymentRequest.getOrDefault("state", "CA");

            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "success", false,
                        "message", "Payment token is required"
                    )
                );
            }

            // Verify tier exists
            Plan plan = planRepository.findById(tierId).orElse(null);
            if (plan == null) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "success", false,
                        "message", "Invalid subscription tier"
                    )
                );
            }

            // In production: 
            // 1. Decrypt/verify Google Pay token
            // 2. Process payment through Stripe
            // 3. Create subscription record
            // 4. Send confirmation email
            
            // For now (demo mode), just log and return success
            Long subtotalCents = plan.getPriceCents().longValue();
            Double taxRate = taxCalculationService.getTaxRateByState(state);
            Long taxCents = taxCalculationService.calculateTaxCents(subtotalCents, taxRate);
            Long totalCents = subtotalCents + taxCents;

            return ResponseEntity.ok(
                Map.of(
                    "success", true,
                    "message", "Payment processed successfully",
                    "transactionId", "demo_" + System.currentTimeMillis(),
                    "amount", totalCents / 100.0,
                    "planName", plan.getName(),
                    "currency", "USD"
                )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "success", false,
                    "message", "Payment processing failed: " + e.getMessage()
                )
            );
        }
    }

    /**
     * Process Apple Pay payment
     * In production, this would verify the token with Apple and process payment through Stripe/Payment Gateway
     * For now, this is a test endpoint that accepts the Apple Pay token
     */
    @PostMapping("/pay/apple")
    @Operation(
        summary = "Process Apple Pay payment",
        description = "Accept and process an Apple Pay token"
    )
    @ApiResponse(responseCode = "200", description = "Payment processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payment request")
    public ResponseEntity<Map<String, Object>> processApplePayment(
            @RequestBody Map<String, Object> paymentRequest) {
        try {
            String token = (String) paymentRequest.get("token");
            Long tierId = ((Number) paymentRequest.get("tierId")).longValue();
            String state = (String) paymentRequest.getOrDefault("state", "CA");

            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "success", false,
                        "message", "Payment token is required"
                    )
                );
            }

            // Verify tier exists
            Plan plan = planRepository.findById(tierId).orElse(null);
            if (plan == null) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "success", false,
                        "message", "Invalid subscription tier"
                    )
                );
            }

            // In production: 
            // 1. Decrypt/verify Apple Pay token
            // 2. Process payment through Stripe
            // 3. Create subscription record
            // 4. Send confirmation email
            
            // For now (demo mode), just log and return success
            Long subtotalCents = plan.getPriceCents().longValue();
            Double taxRate = taxCalculationService.getTaxRateByState(state);
            Long taxCents = taxCalculationService.calculateTaxCents(subtotalCents, taxRate);
            Long totalCents = subtotalCents + taxCents;

            return ResponseEntity.ok(
                Map.of(
                    "success", true,
                    "message", "Apple Pay payment processed successfully",
                    "transactionId", "apple_" + System.currentTimeMillis(),
                    "amount", totalCents / 100.0,
                    "planName", plan.getName(),
                    "currency", "USD"
                )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "success", false,
                    "message", "Apple Pay processing failed: " + e.getMessage()
                )
            );
        }
    }
}
