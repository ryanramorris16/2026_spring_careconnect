package com.careconnect.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.careconnect.model.Plan;
import com.careconnect.model.Subscription;
import com.careconnect.model.User;
import com.careconnect.repository.PaymentRepository;
import com.careconnect.repository.PlanRepository;
import com.careconnect.repository.SubscriptionRepository;
import com.careconnect.repository.UserRepository;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@link SubscriptionService}.
 *
 * <p>All external dependencies (repositories and {@link StripeCheckoutService}) are
 * mocked with Mockito so these tests validate the service's business logic in
 * isolation — no database, Spring context, or live Stripe API connection is needed.</p>
 *
 * <p>Methods that call static Stripe SDK APIs (e.g. {@code Subscription.retrieve()},
 * {@code Customer.create()}) are only exercised along code paths that bypass those
 * calls (null/empty Stripe IDs, plan codes already prefixed with {@code "price_"},
 * subscriptions not found in the local database). Paths that unconditionally call the
 * Stripe API would require PowerMock / Mockito inline mocking and are intentionally
 * left to integration tests.</p>
 */
class SubscriptionServiceTest {

    // -------------------------------------------------------------------------
    // Mocked dependencies
    // -------------------------------------------------------------------------

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private StripeCheckoutService stripeCheckoutService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    // -------------------------------------------------------------------------
    // Test setup
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inject the @Value fields that Spring would normally resolve from properties
        ReflectionTestUtils.setField(subscriptionService, "stripeSecretKey", "sk_test_dummy");
        ReflectionTestUtils.setField(subscriptionService, "frontendBaseUrl", "http://localhost:3000");
    }

    // ==========================================================================
    // createPlan
    // ==========================================================================

    @Test
    @DisplayName("createPlan: delegates to StripeCheckoutService and returns its result")
    void testCreatePlan_delegatesToStripeCheckoutService() {
        // SubscriptionService.createPlan is a thin pass-through; the actual creation
        // logic lives in StripeCheckoutService. Verify the call is forwarded intact
        // and the returned plan is propagated back to the caller.
        Plan expected = new Plan();
        expected.setCode("STANDARD");
        expected.setName("Standard Plan");
        expected.setPriceCents(2000);
        expected.setBillingPeriod("MONTH");
        expected.setIsActive(true);

        when(stripeCheckoutService.createPlan("STANDARD", "Standard Plan", 2000, "MONTH", true))
                .thenReturn(expected);

        Plan result = subscriptionService.createPlan("STANDARD", "Standard Plan", 2000, "MONTH", true);

        assertNotNull(result);
        assertEquals("STANDARD", result.getCode());
        assertEquals("Standard Plan", result.getName());
        verify(stripeCheckoutService).createPlan("STANDARD", "Standard Plan", 2000, "MONTH", true);
    }

    // ==========================================================================
    // getPlan
    // ==========================================================================

    @Test
    @DisplayName("getPlan: returns the Plan entity when the ID is found")
    void testGetPlan_found() {
        // A plan stored in the repository must be returned unchanged.
        Plan plan = new Plan();
        plan.setId(1L);
        plan.setName("Premium Plan");
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        Plan result = subscriptionService.getPlan(1L);

        assertNotNull(result);
        assertEquals("Premium Plan", result.getName());
        verify(planRepository).findById(1L);
    }

    @Test
    @DisplayName("getPlan: throws IllegalArgumentException when no plan exists for the ID")
    void testGetPlan_notFound() {
        // A missing plan must surface as IllegalArgumentException containing the ID
        // so callers can produce a meaningful error response.
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionService.getPlan(99L)
        );

        assertTrue(ex.getMessage().contains("99"),
                "Exception message should include the missing plan ID");
        verify(planRepository).findById(99L);
    }

    // ==========================================================================
    // findOrCreatePlanByStripeId
    // ==========================================================================

    @Test
    @DisplayName("findOrCreatePlanByStripeId: returns the existing plan when the code is already registered")
    void testFindOrCreatePlanByStripeId_existingPlan() {
        // When a plan with the given Stripe price ID already exists, it must be
        // returned as-is without creating a duplicate or overwriting its name.
        Plan existing = new Plan();
        existing.setCode("price_abc123");
        existing.setName("Existing Plan");
        when(planRepository.findByCode("price_abc123")).thenReturn(existing);

        Plan result = subscriptionService.findOrCreatePlanByStripeId("price_abc123", "New Name Override", 3000);

        assertEquals("Existing Plan", result.getName(), "Existing plan name must not be overwritten");
        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("findOrCreatePlanByStripeId: creates and saves a new plan when no matching code exists")
    void testFindOrCreatePlanByStripeId_newPlan() {
        // When no plan is found for the Stripe price ID, a new one is created with
        // the provided nickname, amount, and sensible defaults (monthly, active).
        when(planRepository.findByCode("price_new999")).thenReturn(null);
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> {
            Plan saved = inv.getArgument(0);
            saved.setId(10L); // Simulate DB-generated ID
            return saved;
        });

        Plan result = subscriptionService.findOrCreatePlanByStripeId("price_new999", "My Plan", 1500);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("price_new999", result.getCode());
        assertEquals("My Plan", result.getName());
        assertEquals(1500, result.getPriceCents());
        assertEquals("MONTH", result.getBillingPeriod(), "Billing period should default to MONTH");
        assertTrue(result.getIsActive(), "New plans should default to active");
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    @DisplayName("findOrCreatePlanByStripeId: uses 'Plan <id>' as fallback name when nickname is null")
    void testFindOrCreatePlanByStripeId_nullNicknameUsesFallback() {
        // When no nickname is supplied, the plan name defaults to "Plan <stripePriceId>"
        // so the record remains identifiable even without a human-readable nickname.
        when(planRepository.findByCode("price_xyz")).thenReturn(null);
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        Plan result = subscriptionService.findOrCreatePlanByStripeId("price_xyz", null, 2000);

        assertEquals("Plan price_xyz", result.getName());
    }

    // ==========================================================================
    // syncPlanWithStripe
    // ==========================================================================

    @Test
    @DisplayName("syncPlanWithStripe: returns plan unchanged when code already starts with 'price_'")
    void testSyncPlanWithStripe_validStripePriceId_noApiCall() {
        // A plan whose code already matches the Stripe price ID format ("price_*")
        // requires no Stripe API interaction; it is returned immediately.
        Plan plan = new Plan();
        plan.setId(5L);
        plan.setCode("price_existingStripeId");
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan));

        Plan result = subscriptionService.syncPlanWithStripe(5L, false);

        assertEquals("price_existingStripeId", result.getCode());
        verify(planRepository, never()).save(any()); // No update needed
    }

    @Test
    @DisplayName("syncPlanWithStripe: throws IllegalArgumentException when code is not 'price_*' and createIfMissing=false")
    void testSyncPlanWithStripe_invalidCode_createIfMissingFalse_throws() {
        // If the plan doesn't have a valid Stripe price ID and we aren't allowed to
        // create one, the service must reject the call with a clear exception.
        Plan plan = new Plan();
        plan.setId(6L);
        plan.setCode("STANDARD"); // Not a Stripe price ID
        when(planRepository.findById(6L)).thenReturn(Optional.of(plan));

        assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionService.syncPlanWithStripe(6L, false)
        );
    }

    @Test
    @DisplayName("syncPlanWithStripe: throws IllegalArgumentException when code is null and createIfMissing=false")
    void testSyncPlanWithStripe_nullCode_createIfMissingFalse_throws() {
        // A null plan code is treated the same as a missing Stripe price ID —
        // it doesn't start with "price_" so it must fail when createIfMissing=false.
        Plan plan = new Plan();
        plan.setId(7L);
        plan.setCode(null);
        when(planRepository.findById(7L)).thenReturn(Optional.of(plan));

        assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionService.syncPlanWithStripe(7L, false)
        );
    }

    // ==========================================================================
    // cancelSubscription
    // ==========================================================================

    @Test
    @DisplayName("cancelSubscription: throws IllegalArgumentException when the subscription ID does not exist")
    void testCancelSubscription_notFound() {
        // Attempting to cancel a subscription that doesn't exist in the DB must
        // fail immediately with a domain exception before any Stripe call is made.
        when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionService.cancelSubscription(999L)
        );
    }

    @Test
    @DisplayName("cancelSubscription: marks subscription CANCELLED and clears all Stripe fields when stripeSubscriptionId is null")
    void testCancelSubscription_noStripeId_updatesLocalRecord() {
        // When no Stripe subscription ID is stored, the Stripe API call is skipped.
        // The local record must still be fully cleared and persisted as CANCELLED.
        Subscription sub = new Subscription();
        sub.setId(1L);
        sub.setStripeSubscriptionId(null); // null → no Stripe API call
        sub.setStripeCustomerId("cus_abc");
        sub.setStatus("ACTIVE");
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.cancelSubscription(1L);

        assertEquals("CANCELLED", sub.getStatus());
        assertNull(sub.getStripeSubscriptionId());
        assertNull(sub.getStripeCustomerId());
        assertNull(sub.getPriceId());
        assertNull(sub.getPlan());
        assertNull(sub.getCurrentPeriodEnd());
        verify(subscriptionRepository).save(sub);
    }

    @Test
    @DisplayName("cancelSubscription: marks subscription CANCELLED when stripeSubscriptionId is an empty string")
    void testCancelSubscription_emptyStripeId_updatesLocalRecord() {
        // An empty Stripe subscription ID is treated identically to null —
        // no API call is made and only the local record is updated.
        Subscription sub = new Subscription();
        sub.setId(2L);
        sub.setStripeSubscriptionId(""); // empty → no Stripe API call
        sub.setStatus("ACTIVE");
        when(subscriptionRepository.findById(2L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.cancelSubscription(2L);

        assertEquals("CANCELLED", sub.getStatus());
        verify(subscriptionRepository).save(sub);
    }

    // ==========================================================================
    // cancelSubscriptionByStripeId
    // ==========================================================================

    @Test
    @DisplayName("cancelSubscriptionByStripeId: is a no-op when no subscription matches the Stripe ID")
    void testCancelSubscriptionByStripeId_notFound_noOp() {
        // If the Stripe subscription ID is unknown locally, no exception is thrown
        // and no save is attempted — the absent record can't be updated.
        when(subscriptionRepository.findByStripeSubscriptionId("sub_unknown"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> subscriptionService.cancelSubscriptionByStripeId("sub_unknown"));
        verify(subscriptionRepository, never()).save(any());
    }

    // ==========================================================================
    // saveCheckoutSession
    // ==========================================================================

    @Test
    @DisplayName("saveCheckoutSession: throws IllegalArgumentException when the user does not exist")
    void testSaveCheckoutSession_userNotFound() {
        // Saving a checkout session requires a valid user; a missing user must cause
        // the method to fail before any subscription or payment is persisted.
        Session mockSession = mock(Session.class);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionService.saveCheckoutSession(99L, "standard", 2000L, mockSession)
        );
    }

    @Test
    @DisplayName("saveCheckoutSession: creates a new Subscription and Payment when none exist for the session")
    void testSaveCheckoutSession_createsSubscriptionAndPayment() {
        // Happy path: user exists, session carries IDs, no prior records exist.
        // Both a Subscription and a Payment must be created and persisted.
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("pw")
                .build();
        Session mockSession = mock(Session.class);
        when(mockSession.getCustomer()).thenReturn("cus_abc");
        when(mockSession.getSubscription()).thenReturn("sub_xyz");
        when(mockSession.getId()).thenReturn("cs_test_session");
        when(mockSession.getPaymentIntent()).thenReturn("pi_test_intent");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByStripeSubscriptionId("sub_xyz"))
                .thenReturn(Optional.empty()); // No existing subscription
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.saveCheckoutSession(1L, "standard", 2000L, mockSession);

        // Subscription and Payment must each be saved exactly once
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(paymentRepository).save(any());
    }

    @Test
    @DisplayName("saveCheckoutSession: updates user's stripeCustomerId when the user doesn't have one yet")
    void testSaveCheckoutSession_updatesUserStripeCustomerId() {
        // If the session provides a Stripe customer ID and the user has none stored,
        // the user record must be updated so future sessions can reuse the customer.
        User user = User.builder()
                .id(2L)
                .email("user@example.com")
                .password("pw")
                .build();
        // User has no stripeCustomerId yet (field is null by default above)
        Session mockSession = mock(Session.class);
        when(mockSession.getCustomer()).thenReturn("cus_new_customer");
        when(mockSession.getSubscription()).thenReturn(null); // No subscription in this session
        when(mockSession.getId()).thenReturn("cs_test_session_2");
        when(mockSession.getPaymentIntent()).thenReturn(null);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.saveCheckoutSession(2L, "premium", 3000L, mockSession);

        // The user must now hold the Stripe customer ID from the session
        assertEquals("cus_new_customer", user.getStripeCustomerId());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("saveCheckoutSession: reuses an existing subscription when stripeSubscriptionId is already in the DB")
    void testSaveCheckoutSession_reusesExistingSubscription() {
        // If a subscription record already exists for the Stripe subscription ID
        // in the session, it must be reused rather than a new one being created.
        User user = User.builder().id(3L).email("reuse@example.com").password("pw").build();
        Subscription existingSub = new Subscription();
        existingSub.setId(10L);
        existingSub.setStripeSubscriptionId("sub_existing");

        Session mockSession = mock(Session.class);
        when(mockSession.getCustomer()).thenReturn("cus_reuse");
        when(mockSession.getSubscription()).thenReturn("sub_existing");
        when(mockSession.getId()).thenReturn("cs_reuse_session");
        when(mockSession.getPaymentIntent()).thenReturn("pi_reuse");

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByStripeSubscriptionId("sub_existing"))
                .thenReturn(Optional.of(existingSub)); // Already exists
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.saveCheckoutSession(3L, "standard", 2000L, mockSession);

        // The existing subscription must not be replaced by a new save
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        // Payment must still be persisted
        verify(paymentRepository).save(any());
    }

    // ==========================================================================
    // getUserSubscriptions
    // ==========================================================================

    @Test
    @DisplayName("getUserSubscriptions: throws IllegalArgumentException when the user does not exist")
    void testGetUserSubscriptions_userNotFound() {
        // Requesting subscriptions for a nonexistent user must fail with a clear
        // domain exception so the controller can return an appropriate 4xx response.
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionService.getUserSubscriptions(99L)
        );
    }

    @Test
    @DisplayName("getUserSubscriptions: returns DB subscriptions without Stripe sync when user has no Stripe customer ID")
    void testGetUserSubscriptions_noStripeCustomerId_skipsSync() {
        // When the user has no Stripe customer ID, the service must skip the Stripe
        // sync and return whatever is stored locally, avoiding unnecessary API calls.
        User user = User.builder().id(1L).email("user@example.com").password("pw").build();
        // stripeCustomerId is null (not set via builder)
        Subscription sub = new Subscription();
        sub.setId(10L);
        sub.setStatus("ACTIVE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(List.of(sub));

        List<Subscription> result = subscriptionService.getUserSubscriptions(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.get(0).getStatus());
        verify(subscriptionRepository).findByUser(user);
    }

    @Test
    @DisplayName("getUserSubscriptions: returns empty list when user has no subscriptions")
    void testGetUserSubscriptions_noSubscriptions_returnsEmptyList() {
        // An empty result from the repository is valid; the method must not throw
        // or convert it to null.
        User user = User.builder().id(3L).email("new@example.com").password("pw").build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(List.of());

        List<Subscription> result = subscriptionService.getUserSubscriptions(3L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==========================================================================
    // getUserActiveSubscriptions
    // ==========================================================================

    @Test
    @DisplayName("getUserActiveSubscriptions: throws IllegalArgumentException when the user does not exist")
    void testGetUserActiveSubscriptions_userNotFound() {
        // Requesting active subscriptions for a nonexistent user must fail fast.
        when(userRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionService.getUserActiveSubscriptions(88L)
        );
    }

    @Test
    @DisplayName("getUserActiveSubscriptions: returns only ACTIVE subscriptions when user has no Stripe customer ID")
    void testGetUserActiveSubscriptions_noStripeCustomerId_returnsActive() {
        // Without a Stripe customer ID the sync is skipped; the repository is queried
        // directly with status="ACTIVE" and the result is returned unchanged.
        User user = User.builder().id(5L).email("active@example.com").password("pw").build();
        Subscription activeSub = new Subscription();
        activeSub.setStatus("ACTIVE");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndStatus(user, "ACTIVE"))
                .thenReturn(List.of(activeSub));

        List<Subscription> result = subscriptionService.getUserActiveSubscriptions(5L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.get(0).getStatus());
        verify(subscriptionRepository).findByUserAndStatus(user, "ACTIVE");
    }

    @Test
    @DisplayName("getUserActiveSubscriptions: returns empty list when no ACTIVE subscriptions exist")
    void testGetUserActiveSubscriptions_noActive_returnsEmptyList() {
        // A user may have cancelled subscriptions but no active ones; the result
        // must be an empty list, not null or an exception.
        User user = User.builder().id(6L).email("inactive@example.com").password("pw").build();
        when(userRepository.findById(6L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserAndStatus(user, "ACTIVE")).thenReturn(List.of());

        List<Subscription> result = subscriptionService.getUserActiveSubscriptions(6L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==========================================================================
    // handleStripeWebhook
    // ==========================================================================

    @Test
    @DisplayName("handleStripeWebhook: throws RuntimeException when the JSON payload is malformed")
    void testHandleStripeWebhook_invalidJson_throwsRuntimeException() {
        // A non-JSON payload cannot be parsed by Stripe's Webhook utility.
        // The service must catch JsonSyntaxException and rethrow as RuntimeException.
        assertThrows(
                RuntimeException.class,
                () -> subscriptionService.handleStripeWebhook(
                        "not_valid_json_at_all",
                        "t=1,v1=fakesig",
                        "whsec_test_secret"
                )
        );
    }

    @Test
    @DisplayName("handleStripeWebhook: throws RuntimeException when the webhook signature is invalid")
    void testHandleStripeWebhook_invalidSignature_throwsRuntimeException() {
        // Even with a plausible JSON body, a wrong signature must be rejected.
        // SignatureVerificationException is caught and wrapped as RuntimeException.
        String validishJson = "{\"id\":\"evt_test\",\"object\":\"event\"}";
        assertThrows(
                RuntimeException.class,
                () -> subscriptionService.handleStripeWebhook(
                        validishJson,
                        "t=1,v1=invalidsignature",
                        "whsec_wrongsecret"
                )
        );
    }
}
