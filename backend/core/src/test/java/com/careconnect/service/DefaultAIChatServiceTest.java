package com.careconnect.service;

import com.careconnect.dto.ChatConversationSummary;
import com.careconnect.dto.ChatMessageSummary;
import com.careconnect.dto.ChatRequest;
import com.careconnect.dto.ChatResponse;
import com.careconnect.dto.UploadedFileDTO;
import com.careconnect.model.ChatConversation;
import com.careconnect.model.ChatMessage;
import com.careconnect.model.Patient;
import com.careconnect.model.UserAIConfig;
import com.careconnect.repository.ChatConversationRepository;
import com.careconnect.repository.ChatMessageRepository;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.UserAIConfigRepository;
import com.careconnect.service.cache.AIChatCacheService;
import com.careconnect.service.security.InputSanitizationService;
import com.careconnect.service.security.LangChainGovernanceService;
import com.careconnect.service.security.ResponseSanitizationService;
import com.careconnect.service.security.SecurityAuditService;
import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultAIChatService}.
 *
 * <p>All collaborators are replaced with Mockito mocks so the service's business
 * logic can be validated in isolation — no Spring context or database is needed.
 *
 * <p>The {@link ChatModel} mock uses {@code RETURNS_DEEP_STUBS} so that the
 * chained call {@code chatModel.chat(messages).aiMessage().text()} can be
 * stubbed without needing concrete LangChain4j response types.
 *
 * <p>Coverage targets: processChat (happy path + validation + AI error handling),
 * getPatientConversations, getConversationMessages, getRecentMessagesForUser,
 * and deactivateConversation.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class DefaultAIChatServiceTest {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final Long USER_ID    = 1L;
    private static final Long PATIENT_ID = 2L;
    private static final String CONV_ID  = "conv-uuid-test-1234";
    private static final String AI_TEXT  = "Hello, I can help you with that!";

    // ── Mocked collaborators ───────────────────────────────────────────────────

    /** Deep-stubs allow chaining: chatModel.chat(list).aiMessage().text() */
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatModel chatModel;

    @Mock private UserAIConfigRepository       userAIConfigRepository;
    @Mock private ChatConversationRepository   chatConversationRepository;
    @Mock private ChatMessageRepository        chatMessageRepository;
    @Mock private PatientRepository            patientRepository;
    @Mock private MedicalContextService        medicalContextService;
    @Mock private PatientContextRetrievalService patientContextRetrievalService;
    @Mock private ChatMemoryFactory            chatMemoryFactory;
    @Mock private ChatAuditService             chatAuditService;
    @Mock private CaregiverPatientLinkService  caregiverPatientLinkService;
    @Mock private InputSanitizationService     inputSanitizationService;
    @Mock private ResponseSanitizationService  responseSanitizationService;
    @Mock private LangChainGovernanceService   langChainGovernanceService;
    @Mock private AIChatCacheService           cacheService;
    @Mock private SecurityAuditService         securityAuditService;
    @Mock private DocumentProcessingService    documentProcessingService;
    @Mock private ChatMemory                   chatMemory;

    // ── Subject under test ─────────────────────────────────────────────────────

    private DefaultAIChatService service;

    // ── Shared fixtures ────────────────────────────────────────────────────────

    private Patient          patient;
    private UserAIConfig     aiConfig;
    private ChatConversation conversation;

    // ── Setup ──────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Construct the service manually so all mocks are injected via constructor
        service = new DefaultAIChatService(
                chatModel, userAIConfigRepository, chatConversationRepository,
                chatMessageRepository, patientRepository, medicalContextService,
                patientContextRetrievalService, chatMemoryFactory, chatAuditService,
                caregiverPatientLinkService, inputSanitizationService,
                responseSanitizationService, langChainGovernanceService,
                cacheService, securityAuditService, documentProcessingService);

        // Build fixtures
        patient = new Patient();

        aiConfig = UserAIConfig.builder()
                .userId(USER_ID)
                .patientId(PATIENT_ID)
                .preferredAiProvider(UserAIConfig.AIProvider.DEEPSEEK)
                .deepseekModel("deepseek-chat")
                .openaiModel("gpt-4o")
                .maxTokens(2048)
                .conversationHistoryLimit(20)
                .build();

        conversation = ChatConversation.builder()
                .conversationId(CONV_ID)
                .userId(USER_ID)
                .patientId(PATIENT_ID)
                .chatType(ChatConversation.ChatType.GENERAL_SUPPORT)
                .title("Test conversation")
                .isActive(true)
                .totalTokensUsed(0)
                .build();
        // @PrePersist does not run in unit tests — set manually so isAfter() checks work
        conversation.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        // ── Common stubs ─────────────────────────────────────────────────────

        // Cache lookups
        when(cacheService.findPatient(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(cacheService.findUserAIConfig(eq(USER_ID), any())).thenReturn(Optional.of(aiConfig));
        when(cacheService.saveUserAIConfig(any())).thenReturn(aiConfig);
        when(cacheService.findConversation(any())).thenReturn(Optional.empty());
        when(cacheService.saveConversation(any())).thenReturn(conversation);

        // Sanitization: pass-through by default
        when(inputSanitizationService.sanitizeUserInput(any(), any(), any()))
                .thenReturn(new InputSanitizationService.SanitizationResult(
                        "safe message", false, Collections.emptyList()));
        when(inputSanitizationService.sanitizeSystemPrompt(any(), any(), any()))
                .thenReturn(new InputSanitizationService.SanitizationResult(
                        "safe system prompt", false, Collections.emptyList()));
        when(responseSanitizationService.sanitizeAIResponse(any(), any(), any(), any()))
                .thenReturn(new ResponseSanitizationService.SanitizationResult(
                        AI_TEXT, Collections.emptyList()));

        // Chat memory
        when(chatMemoryFactory.createSessionBasedChatMemory(any(), any())).thenReturn(chatMemory);
        when(chatMemory.messages()).thenReturn(Collections.emptyList());

        // Repository helpers called while building the response
        when(chatMessageRepository.countByConversation(any())).thenReturn(5);
        when(chatMessageRepository.sumTokensUsedByConversation(any())).thenReturn(100);
        when(chatMessageRepository.findTopNByConversationOrderByCreatedAtAsc(any(), anyInt()))
                .thenReturn(Collections.emptyList());

        // Medical context (empty by default — individual tests override as needed)
        when(medicalContextService.buildPatientContext(any(), any(), any())).thenReturn("");

        // AI model: deep-stub returns AI_TEXT by default so happy-path tests are concise.
        // Cast is required to resolve the overload ambiguity between
        // chat(List<ChatMessage>) and chat(ChatMessage...) in LangChain4j 1.x.
        when(chatModel.chat(ArgumentMatchers.<dev.langchain4j.data.message.ChatMessage>anyList())
                .aiMessage().text()).thenReturn(AI_TEXT);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  processChat — validation / early-exit paths
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("processChat — input validation")
    class ProcessChatValidation {

        @Test
        @DisplayName("throws IllegalArgumentException when both patientId and userId are null")
        void bothIdsNull_throwsIllegalArgumentException() {
            // A request with no user identity cannot be processed at all
            ChatRequest req = new ChatRequest();

            assertThrows(IllegalArgumentException.class,
                    () -> service.processChat(req));
        }

        @Test
        @DisplayName("returns error response when userId is absent but patientId is set")
        void missingUserId_returnsErrorResponse() {
            // patientId is present but the inner guard (userId == null) fires first
            ChatRequest req = new ChatRequest();
            req.setPatientId(PATIENT_ID);
            // userId intentionally left null

            ChatResponse resp = service.processChat(req);

            assertFalse(resp.getSuccess(),
                    "A request without userId must produce a failure response");
            assertEquals("PROCESSING_ERROR", resp.getErrorCode());
        }

        @Test
        @DisplayName("returns error response when message is blank and no files are attached")
        void blankMessageAndNoFiles_returnsErrorResponse() {
            ChatRequest req = new ChatRequest();
            req.setUserId(USER_ID);
            req.setPatientId(PATIENT_ID);
            req.setMessage("   "); // whitespace only

            ChatResponse resp = service.processChat(req);

            assertFalse(resp.getSuccess());
            assertTrue(resp.getErrorMessage().contains("Message content"),
                    "Error message should mention missing message content");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when the patient cannot be found in cache/DB")
        void patientNotFound_throwsIllegalArgumentException() {
            // Simulate a cache/DB miss for the patient
            when(cacheService.findPatient(PATIENT_ID)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.processChat(buildBasicRequest()),
                    "Patient lookup failure should propagate as IllegalArgumentException");
        }

        @Test
        @DisplayName("nullifies a blank (whitespace-only) conversationId before processing")
        void blankConversationId_isTreatedAsAbsent() {
            // The service strips blank conversationIds so a new conversation is created
            ChatRequest req = buildBasicRequest();
            req.setConversationId("   ");

            ChatResponse resp = service.processChat(req);

            assertTrue(resp.getSuccess());
            // A new conversation must have been persisted via the cache service
            verify(cacheService, atLeastOnce()).saveConversation(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  processChat — input & system-prompt sanitization
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("processChat — sanitization guardrails")
    class ProcessChatSanitization {

        @Test
        @DisplayName("returns error response when user message is blocked by the sanitizer")
        void userInputBlocked_returnsErrorResponse() {
            // Simulates detection of SQL injection or XSS in user message
            when(inputSanitizationService.sanitizeUserInput(any(), any(), any()))
                    .thenReturn(new InputSanitizationService.SanitizationResult(
                            "", true, List.of("SQL injection detected")));

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertFalse(resp.getSuccess());
            assertTrue(resp.getErrorMessage().contains("cannot be processed"),
                    "Blocked input should produce a descriptive error");
        }

        @Test
        @DisplayName("returns error response when the system prompt is blocked")
        void systemPromptBlocked_returnsErrorResponse() {
            // System prompt injection attempt detected by the sanitizer
            when(inputSanitizationService.sanitizeSystemPrompt(any(), any(), any()))
                    .thenReturn(new InputSanitizationService.SanitizationResult(
                            "", true, List.of("Prompt injection detected")));

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertFalse(resp.getSuccess());
            assertTrue(resp.getErrorMessage().contains("System configuration error"));
        }

        @Test
        @DisplayName("returns error response when uploaded file content is blocked")
        void uploadedFileContentBlocked_returnsErrorResponse() {
            // First call (user message text) passes; second call (extracted file content) is blocked
            InputSanitizationService.SanitizationResult pass =
                    new InputSanitizationService.SanitizationResult(
                            "safe message", false, Collections.emptyList());
            InputSanitizationService.SanitizationResult block =
                    new InputSanitizationService.SanitizationResult(
                            "", true, List.of("Malicious content in file"));

            when(inputSanitizationService.sanitizeUserInput(any(), any(), any()))
                    .thenReturn(pass)    // first invocation: user message
                    .thenReturn(block);  // second invocation: extracted file text

            UploadedFileDTO file = UploadedFileDTO.builder()
                    .filename("report.pdf")
                    .contentType("application/pdf")
                    .build();
            when(documentProcessingService.extractTextContent(any())).thenReturn("extracted text");

            ChatRequest req = buildBasicRequest();
            req.setUploadedFiles(List.of(file));

            ChatResponse resp = service.processChat(req);

            assertFalse(resp.getSuccess());
            assertTrue(resp.getErrorMessage().contains("uploaded document"),
                    "Blocked file content should mention the uploaded document");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  processChat — AI model response / exception handling
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("processChat — AI model response handling")
    class ProcessChatAiHandling {

        @Test
        @DisplayName("returns fallback message when the AI model returns a null response")
        void nullAiResponse_returnsFallbackMessage() {
            // Explicitly override the deep-stub to return null for the entire response
            when(chatModel.chat(ArgumentMatchers.<dev.langchain4j.data.message.ChatMessage>anyList())).thenReturn(null);

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess(),
                    "A null AI response is handled gracefully — overall success is true");
            assertTrue(resp.getAiResponse().contains("having trouble processing"),
                    "User-facing fallback message should explain the issue");
        }

        @Test
        @DisplayName("returns auth-error message when AuthenticationException is thrown")
        void authenticationException_returnsFallbackMessage() {
            // Mocking avoids constructor dependency on the concrete exception class
            AuthenticationException authEx = mock(AuthenticationException.class);
            when(chatModel.chat(ArgumentMatchers.<dev.langchain4j.data.message.ChatMessage>anyList())).thenThrow(authEx);

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess());
            assertTrue(resp.getAiResponse().contains("authentication issues"),
                    "Authentication failure should surface a specific user message");
        }

        @Test
        @DisplayName("returns config-error message when IllegalStateException is thrown")
        void illegalStateException_returnsFallbackMessage() {
            // Simulates a missing or misconfigured API key
            when(chatModel.chat(ArgumentMatchers.<dev.langchain4j.data.message.ChatMessage>anyList())).thenThrow(new IllegalStateException("API key not set"));

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess());
            assertTrue(resp.getAiResponse().contains("currently unavailable"),
                    "Configuration error should surface a service-unavailable message");
        }

        @Test
        @DisplayName("returns 503 message when RuntimeException mentions 'service unavailable'")
        void runtimeException_503_returnsFallbackMessage() {
            when(chatModel.chat(ArgumentMatchers.<dev.langchain4j.data.message.ChatMessage>anyList())).thenThrow(
                    new RuntimeException("503 service unavailable"));

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess());
            assertTrue(resp.getAiResponse().contains("temporarily unavailable"));
        }

        @Test
        @DisplayName("returns 503 message when RuntimeException message contains '503'")
        void runtimeException_503Code_returnsFallbackMessage() {
            when(chatModel.chat(ArgumentMatchers.<dev.langchain4j.data.message.ChatMessage>anyList())).thenThrow(
                    new RuntimeException("upstream error: 503"));

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess());
            assertTrue(resp.getAiResponse().contains("temporarily unavailable"));
        }

        @Test
        @DisplayName("returns rate-limit message when RuntimeException mentions '429'")
        void runtimeException_429_returnsFallbackMessage() {
            when(chatModel.chat(ArgumentMatchers.<dev.langchain4j.data.message.ChatMessage>anyList())).thenThrow(
                    new RuntimeException("429 rate limit exceeded"));

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess());
            assertTrue(resp.getAiResponse().contains("high volume of requests"),
                    "Rate-limit scenario should guide the user to wait and retry");
        }

        @Test
        @DisplayName("returns rate-limit message when RuntimeException mentions 'rate limit'")
        void runtimeException_rateLimitText_returnsFallbackMessage() {
            when(chatModel.chat(ArgumentMatchers.<dev.langchain4j.data.message.ChatMessage>anyList())).thenThrow(
                    new RuntimeException("OpenAI: rate limit hit"));

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess());
            assertTrue(resp.getAiResponse().contains("high volume of requests"));
        }

        @Test
        @DisplayName("returns generic error message for any other RuntimeException")
        void genericRuntimeException_returnsFallbackMessage() {
            when(chatModel.chat(ArgumentMatchers.<dev.langchain4j.data.message.ChatMessage>anyList())).thenThrow(
                    new RuntimeException("unexpected network failure"));

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess());
            assertTrue(resp.getAiResponse().contains("encountered an error"),
                    "Unrecognised runtime errors should produce a generic retry message");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  processChat — happy-path scenarios
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("processChat — happy-path scenarios")
    class ProcessChatHappyPath {

        @Test
        @DisplayName("returns successful response for a standard patient chat")
        void patientChat_returnsSuccessResponse() {
            // Medical context is available for the patient
            when(medicalContextService.buildPatientContext(eq(PATIENT_ID), any(), any()))
                    .thenReturn("Vitals: BP 120/80, HR 72");

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess());
            assertEquals(CONV_ID, resp.getConversationId(),
                    "Response must reference the conversation returned by cache");
            assertEquals(AI_TEXT, resp.getAiResponse());
            assertEquals("DEEPSEEK_VIA_LANGCHAIN4J", resp.getAiProvider());
            assertNotNull(resp.getTimestamp());
        }

        @Test
        @DisplayName("returns successful response for a caregiver-only chat (no patientId)")
        void caregiverOnlyChat_returnsSuccessResponse() {
            // Caregiver sends a general question without a specific patient in context
            ChatRequest req = new ChatRequest();
            req.setUserId(USER_ID);
            req.setMessage("How do I manage caregiver burnout?");

            ChatResponse resp = service.processChat(req);

            assertTrue(resp.getSuccess());
            assertEquals(AI_TEXT, resp.getAiResponse());
        }

        @Test
        @DisplayName("uses temperature from request when explicitly provided")
        void requestTemperature_propagatedToResponse() {
            ChatRequest req = buildBasicRequest();
            req.setTemperature(0.9);

            ChatResponse resp = service.processChat(req);

            assertTrue(resp.getSuccess());
            assertEquals(0.9, resp.getTemperatureUsed(), 0.001,
                    "Response temperature should match what was set on the request");
        }

        @Test
        @DisplayName("defaults temperature to 0.1 when the request omits it")
        void noTemperatureInRequest_defaultsToPointOne() {
            ChatRequest req = buildBasicRequest();
            // temperature is null — service should default to 0.1

            ChatResponse resp = service.processChat(req);

            assertTrue(resp.getSuccess());
            assertEquals(0.1, resp.getTemperatureUsed(), 0.001);
        }

        @Test
        @DisplayName("reuses an existing conversation when conversationId is found in cache")
        void existingConversationId_conversationIsReused() {
            // Conversation was started previously and is found in cache
            conversation.setCreatedAt(LocalDateTime.now().minusHours(2));
            when(cacheService.findConversation(CONV_ID)).thenReturn(Optional.of(conversation));

            ChatRequest req = buildBasicRequest();
            req.setConversationId(CONV_ID);

            ChatResponse resp = service.processChat(req);

            assertTrue(resp.getSuccess());
            assertEquals(CONV_ID, resp.getConversationId());
            // The existing conversation should not be persisted again via saveConversation
            verify(cacheService, never()).saveConversation(any());
        }

        @Test
        @DisplayName("creates a new conversation when the provided conversationId is not found")
        void unknownConversationId_newConversationCreated() {
            // Cache returns empty even though an ID was supplied (e.g. expired/invalid)
            when(cacheService.findConversation("unknown-id")).thenReturn(Optional.empty());

            ChatRequest req = buildBasicRequest();
            req.setConversationId("unknown-id");

            ChatResponse resp = service.processChat(req);

            assertTrue(resp.getSuccess());
            // A brand-new conversation must be saved
            verify(cacheService, atLeastOnce()).saveConversation(any());
        }

        @Test
        @DisplayName("creates a new conversation when no conversationId is provided")
        void noConversationId_newConversationCreated() {
            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess());
            verify(cacheService, atLeastOnce()).saveConversation(any());
        }

        @Test
        @DisplayName("appends extracted file content to the message for uploaded files")
        void uploadedFileWithContent_fileTextIncluded() {
            UploadedFileDTO file = UploadedFileDTO.builder()
                    .filename("lab_results.pdf")
                    .contentType("application/pdf")
                    .build();
            when(documentProcessingService.extractTextContent(any()))
                    .thenReturn("Lab result: HbA1c 6.5%");

            ChatRequest req = buildBasicRequest();
            req.setUploadedFiles(List.of(file));

            ChatResponse resp = service.processChat(req);

            assertTrue(resp.getSuccess());
            // Document processing should have been invoked once
            verify(documentProcessingService).extractTextContent(any());
        }

        @Test
        @DisplayName("handles uploaded files gracefully when extraction returns empty text")
        void uploadedFileWithNoExtractedText_successWithoutFileContent() {
            UploadedFileDTO file = UploadedFileDTO.builder()
                    .filename("empty.pdf")
                    .contentType("application/pdf")
                    .build();
            when(documentProcessingService.extractTextContent(any())).thenReturn("");

            ChatRequest req = buildBasicRequest();
            req.setUploadedFiles(List.of(file));

            ChatResponse resp = service.processChat(req);

            assertTrue(resp.getSuccess());
        }

        @Test
        @DisplayName("handles file extraction exception gracefully and continues processing")
        void uploadedFileThrowsDuringExtraction_processingContinues() {
            UploadedFileDTO file = UploadedFileDTO.builder()
                    .filename("corrupt.pdf")
                    .contentType("application/pdf")
                    .build();
            when(documentProcessingService.extractTextContent(any()))
                    .thenThrow(new RuntimeException("Corrupt PDF"));

            ChatRequest req = buildBasicRequest();
            req.setUploadedFiles(List.of(file));

            // The service catches per-file exceptions internally; overall request succeeds
            ChatResponse resp = service.processChat(req);

            assertTrue(resp.getSuccess());
        }

        @Test
        @DisplayName("uses CAREGIVER_SYSTEM_PROMPT when no patientId is present in request")
        void caregiverChat_withoutPatientId_usesCaregiverSystemPrompt() {
            // When patientId is absent the service should select the caregiver prompt.
            // We verify indirectly: system-prompt sanitization is called with a non-null value.
            ChatRequest req = new ChatRequest();
            req.setUserId(USER_ID);
            req.setMessage("General care question?");

            service.processChat(req);

            // sanitizeSystemPrompt must have been invoked with some non-empty prompt
            verify(inputSanitizationService, atLeastOnce())
                    .sanitizeSystemPrompt(argThat(p -> p != null && !p.isBlank()), any(), any());
        }

        @Test
        @DisplayName("includes context_included and conversation metadata in success response")
        void successResponse_containsMetadata() {
            when(chatMessageRepository.countByConversation(any())).thenReturn(7);
            when(chatMessageRepository.sumTokensUsedByConversation(any())).thenReturn(300);

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess());
            assertEquals(7, resp.getTotalMessagesInConversation());
            assertEquals(300, resp.getTotalTokensUsedInConversation());
            assertNotNull(resp.getContextIncluded());
            assertFalse(resp.getApproachingTokenLimit());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getPatientConversations
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPatientConversations")
    class GetPatientConversations {

        @Test
        @DisplayName("returns a list of conversation summaries for a patient")
        void returnsConversationSummaries() {
            when(chatConversationRepository
                    .findByPatientIdAndIsActiveTrueOrderByUpdatedAtDesc(PATIENT_ID))
                    .thenReturn(List.of(conversation));
            when(chatMessageRepository.countByConversation(conversation)).thenReturn(3);

            List<ChatConversationSummary> summaries =
                    service.getPatientConversations(PATIENT_ID);

            assertEquals(1, summaries.size());
            ChatConversationSummary s = summaries.get(0);
            assertEquals(CONV_ID, s.getConversationId());
            assertEquals("Test conversation", s.getTitle());
            assertEquals(3, s.getTotalMessages());
            assertTrue(s.getIsActive());
        }

        @Test
        @DisplayName("returns an empty list when no active conversations exist for patient")
        void noConversations_returnsEmptyList() {
            when(chatConversationRepository
                    .findByPatientIdAndIsActiveTrueOrderByUpdatedAtDesc(PATIENT_ID))
                    .thenReturn(Collections.emptyList());

            List<ChatConversationSummary> summaries =
                    service.getPatientConversations(PATIENT_ID);

            assertTrue(summaries.isEmpty());
        }

        @Test
        @DisplayName("maps AI provider name to string in the summary")
        void aiProviderMappedToStringName() {
            conversation.setAiProviderUsed(UserAIConfig.AIProvider.DEEPSEEK);
            when(chatConversationRepository
                    .findByPatientIdAndIsActiveTrueOrderByUpdatedAtDesc(PATIENT_ID))
                    .thenReturn(List.of(conversation));

            List<ChatConversationSummary> summaries =
                    service.getPatientConversations(PATIENT_ID);

            assertEquals("DEEPSEEK", summaries.get(0).getAiProvider());
        }

        @Test
        @DisplayName("maps null AI provider to null in the summary without throwing")
        void nullAiProvider_mappedToNull() {
            conversation.setAiProviderUsed(null);
            when(chatConversationRepository
                    .findByPatientIdAndIsActiveTrueOrderByUpdatedAtDesc(PATIENT_ID))
                    .thenReturn(List.of(conversation));

            List<ChatConversationSummary> summaries =
                    service.getPatientConversations(PATIENT_ID);

            assertNull(summaries.get(0).getAiProvider());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getConversationMessages
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getConversationMessages")
    class GetConversationMessages {

        @Test
        @DisplayName("returns message summaries for a valid, active conversationId")
        void validConversation_returnsMessageSummaries() {
            ChatMessage msg = buildChatMessage(ChatMessage.MessageType.USER, "What are my vitals?");

            when(chatConversationRepository.findByConversationIdAndIsActiveTrue(CONV_ID))
                    .thenReturn(Optional.of(conversation));
            when(chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation))
                    .thenReturn(List.of(msg));

            List<ChatMessageSummary> summaries = service.getConversationMessages(CONV_ID);

            assertEquals(1, summaries.size());
            ChatMessageSummary s = summaries.get(0);
            assertEquals("What are my vitals?", s.getContent());
            assertEquals(ChatMessage.MessageType.USER, s.getMessageType());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when conversationId is not found")
        void unknownConversationId_throwsIllegalArgumentException() {
            when(chatConversationRepository.findByConversationIdAndIsActiveTrue("missing"))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.getConversationMessages("missing"),
                    "Looking up a non-existent conversation should throw");
        }

        @Test
        @DisplayName("returns empty list when conversation exists but has no messages")
        void conversationWithNoMessages_returnsEmptyList() {
            when(chatConversationRepository.findByConversationIdAndIsActiveTrue(CONV_ID))
                    .thenReturn(Optional.of(conversation));
            when(chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation))
                    .thenReturn(Collections.emptyList());

            List<ChatMessageSummary> summaries = service.getConversationMessages(CONV_ID);

            assertTrue(summaries.isEmpty());
        }

        @Test
        @DisplayName("maps all message fields into the summary DTO correctly")
        void messageFieldsMappedCorrectly() {
            ChatMessage msg = buildChatMessage(ChatMessage.MessageType.ASSISTANT,
                    "Your BP is 120/80.");
            msg.setId(42L);
            msg.setTokensUsed(50);
            msg.setProcessingTimeMs(350L);
            msg.setAiModelUsed("deepseek-chat");

            when(chatConversationRepository.findByConversationIdAndIsActiveTrue(CONV_ID))
                    .thenReturn(Optional.of(conversation));
            when(chatMessageRepository.findByConversationOrderByCreatedAtAsc(conversation))
                    .thenReturn(List.of(msg));

            ChatMessageSummary s = service.getConversationMessages(CONV_ID).get(0);

            assertEquals(42L,            s.getMessageId());
            assertEquals(50,             s.getTokensUsed());
            assertEquals(350L,           s.getProcessingTimeMs());
            assertEquals("deepseek-chat", s.getAiModelUsed());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  getRecentMessagesForUser
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getRecentMessagesForUser")
    class GetRecentMessagesForUser {

        @Test
        @DisplayName("returns empty list when the user has no active conversations")
        void noConversations_returnsEmptyList() {
            when(chatConversationRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(USER_ID))
                    .thenReturn(Collections.emptyList());

            List<ChatMessageSummary> result = service.getRecentMessagesForUser(USER_ID, 10);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns messages from the most recent active conversation")
        void singleConversation_returnsMessages() {
            ChatMessage msg = buildChatMessage(ChatMessage.MessageType.ASSISTANT,
                    "Your BP is 120/80.");

            when(chatConversationRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(USER_ID))
                    .thenReturn(List.of(conversation));
            when(chatMessageRepository.findTopNByConversationOrderByCreatedAtAsc(conversation, 5))
                    .thenReturn(List.of(msg));

            List<ChatMessageSummary> result = service.getRecentMessagesForUser(USER_ID, 5);

            assertEquals(1, result.size());
            assertEquals("Your BP is 120/80.", result.get(0).getContent());
        }

        @Test
        @DisplayName("uses only the most recent conversation when multiple conversations exist")
        void multipleConversations_onlyMostRecentIsQueried() {
            ChatConversation older = ChatConversation.builder()
                    .conversationId("older-conv")
                    .userId(USER_ID)
                    .patientId(PATIENT_ID)
                    .isActive(true)
                    .totalTokensUsed(0)
                    .build();
            older.setCreatedAt(LocalDateTime.now().minusHours(3));

            ChatMessage recentMsg = buildChatMessage(
                    ChatMessage.MessageType.USER, "Latest message");

            // Service returns [conversation, older] in descending order (conversation is newest)
            when(chatConversationRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(USER_ID))
                    .thenReturn(List.of(conversation, older));
            when(chatMessageRepository.findTopNByConversationOrderByCreatedAtAsc(conversation, 3))
                    .thenReturn(List.of(recentMsg));

            List<ChatMessageSummary> result = service.getRecentMessagesForUser(USER_ID, 3);

            // Only messages from 'conversation' (the most recent) should be returned
            assertEquals(1, result.size());
            assertEquals("Latest message", result.get(0).getContent());
            // 'older' conversation's messages must NOT be queried
            verify(chatMessageRepository, never())
                    .findTopNByConversationOrderByCreatedAtAsc(eq(older), anyInt());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  deactivateConversation
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deactivateConversation")
    class DeactivateConversation {

        @Test
        @DisplayName("sets isActive=false and saves the conversation when found")
        void deactivate_success() {
            when(chatConversationRepository.findByConversationIdAndIsActiveTrue(CONV_ID))
                    .thenReturn(Optional.of(conversation));

            service.deactivateConversation(CONV_ID);

            assertFalse(conversation.getIsActive(),
                    "Conversation must be marked inactive after deactivation");
            verify(chatConversationRepository).save(conversation);
        }

        @Test
        @DisplayName("logs the deletion through the audit service")
        void deactivate_auditEventLogged() {
            when(chatConversationRepository.findByConversationIdAndIsActiveTrue(CONV_ID))
                    .thenReturn(Optional.of(conversation));

            service.deactivateConversation(CONV_ID);

            // The audit service must record who deleted which conversation and why
            verify(chatAuditService).logConversationDeleted(
                    eq(USER_ID), eq(CONV_ID), anyString());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when the conversationId is not found")
        void deactivate_notFound_throwsIllegalArgumentException() {
            when(chatConversationRepository.findByConversationIdAndIsActiveTrue("ghost"))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.deactivateConversation("ghost"),
                    "Deactivating a non-existent conversation must throw");
        }

        @Test
        @DisplayName("does not save when conversation is not found (exception raised first)")
        void deactivate_notFound_repositorySaveNotCalled() {
            when(chatConversationRepository.findByConversationIdAndIsActiveTrue("ghost"))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.deactivateConversation("ghost"));

            verify(chatConversationRepository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  parseContextIncluded — medical context tag parsing
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Medical context — context type tagging")
    class MedicalContextParsing {

        /**
         * The medical-context parsing logic lives inside {@code parseContextIncluded},
         * which is a private helper.  We exercise it indirectly through
         * {@code processChat}: the method's output is NOT surfaced in the response
         * (the main path sets contextIncluded to a fixed list), but we can confirm
         * that the correct medical-context string was passed to the sanitization
         * and AI layers when different context sections are present.
         */

        @Test
        @DisplayName("builds patient context when patientId is present")
        void patientContext_built_whenPatientIdSet() {
            when(medicalContextService.buildPatientContext(eq(PATIENT_ID), any(), any()))
                    .thenReturn("Vitals: HR 72  Medications: Aspirin  Allergies: Penicillin");

            ChatResponse resp = service.processChat(buildBasicRequest());

            assertTrue(resp.getSuccess());
            verify(medicalContextService).buildPatientContext(eq(PATIENT_ID), any(), any());
        }

        @Test
        @DisplayName("skips patient context when no patientId is set (caregiver-only mode)")
        void patientContext_skipped_whenNoPatientId() {
            ChatRequest req = new ChatRequest();
            req.setUserId(USER_ID);
            req.setMessage("General question");

            service.processChat(req);

            // medicalContextService must not be called without a patient
            verify(medicalContextService, never()).buildPatientContext(any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a minimal, valid {@link ChatRequest} for the patient-chat happy path.
     * Individual tests can override fields as needed.
     */
    private ChatRequest buildBasicRequest() {
        ChatRequest req = new ChatRequest();
        req.setUserId(USER_ID);
        req.setPatientId(PATIENT_ID);
        req.setMessage("What are my recent lab results?");
        return req;
    }

    /**
     * Creates a {@link ChatMessage} with the given type and content,
     * with {@code createdAt} pre-set so that summary conversion does not NPE.
     */
    private ChatMessage buildChatMessage(ChatMessage.MessageType type, String content) {
        ChatMessage msg = ChatMessage.builder()
                .conversation(conversation)
                .messageType(type)
                .content(content)
                .build();
        msg.setCreatedAt(LocalDateTime.now());
        return msg;
    }
}
