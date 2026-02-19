package com.careconnect.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.careconnect.dto.ChatConversationSummary;
import com.careconnect.dto.ChatMessageSummary;
import com.careconnect.dto.ChatRequest;
import com.careconnect.dto.ChatResponse;
import com.careconnect.dto.UserAIConfigDTO;
import com.careconnect.model.ChatConversation.ChatType;
import com.careconnect.model.ChatMessage;
import com.careconnect.model.UserAIConfig.AIProvider;
import com.careconnect.repository.ChatConversationRepository;
import com.careconnect.service.AIChatService;
import com.careconnect.service.ChatCleanupService;
import com.careconnect.service.UserAIConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Unit tests for {@link AIChatController}.
 *
 * <p>Uses {@link WebMvcTest} to test the controller layer in isolation
 * with all service/repository dependencies mocked.</p>
 */
@WebMvcTest(AIChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class AIChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AIChatService aiChatService;

    @MockitoBean
    private UserAIConfigService userAIConfigService;

    @MockitoBean
    private ChatConversationRepository chatConversationRepository;

    @MockitoBean
    private ChatCleanupService chatCleanupService;

    @Autowired
    private ObjectMapper objectMapper;

    private ChatRequest sampleRequest;
    private ChatResponse successResponse;
    private ChatConversationSummary sampleConversation;
    private ChatMessageSummary sampleMessage;
    private UserAIConfigDTO sampleConfig;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());

        sampleRequest = ChatRequest.builder()
                .userId(1L)
                .patientId(2L)
                .message("How is my patient doing?")
                .chatType(ChatType.GENERAL_SUPPORT)
                .build();

        successResponse = ChatResponse.builder()
                .conversationId("conv-123")
                .aiResponse("Your patient is doing well.")
                .success(true)
                .isNewConversation(false)
                .timestamp(LocalDateTime.now())
                .build();

        sampleConversation = ChatConversationSummary.builder()
                .conversationId("conv-123")
                .title("General Chat")
                .chatType(ChatType.GENERAL_SUPPORT)
                .totalMessages(5)
                .isActive(true)
                .build();

        sampleMessage = ChatMessageSummary.builder()
                .messageId(10L)
                .content("Hello!")
                .messageType(ChatMessage.MessageType.USER)
                .createdAt(LocalDateTime.now())
                .build();

        sampleConfig = UserAIConfigDTO.builder()
                .id(1L)
                .userId(1L)
                .patientId(2L)
                .aiProvider(AIProvider.OPENAI)
                .build();
    }

    // -----------------------------------------------------------------------
    // POST /v1/api/ai-chat/chat
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /chat - success returns 200 with AI response body")
    void sendMessage_success_returns200() throws Exception {
        Mockito.when(aiChatService.processChat(any(ChatRequest.class))).thenReturn(successResponse);

        mockMvc.perform(post("/v1/api/ai-chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId", is("conv-123")))
                .andExpect(jsonPath("$.success", is(true)));

        Mockito.verify(aiChatService).processChat(any(ChatRequest.class));
    }

    @Test
    @DisplayName("POST /chat - service returns failure returns 400 with error details")
    void sendMessage_serviceReturnsFailure_returns400() throws Exception {
        ChatResponse failResponse = ChatResponse.builder()
                .success(false)
                .errorMessage("AI unavailable")
                .errorCode("AI_ERROR")
                .build();
        Mockito.when(aiChatService.processChat(any(ChatRequest.class))).thenReturn(failResponse);

        mockMvc.perform(post("/v1/api/ai-chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("AI_ERROR")));
    }

    @Test
    @DisplayName("POST /chat - service throws exception returns 500 with INTERNAL_ERROR code")
    void sendMessage_serviceThrows_returns500() throws Exception {
        Mockito.when(aiChatService.processChat(any(ChatRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/v1/api/ai-chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INTERNAL_ERROR")));
    }

    // -----------------------------------------------------------------------
    // GET /v1/api/ai-chat/conversations/{patientId}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /conversations/{patientId} - success returns 200 with conversation list")
    void getPatientConversations_success_returns200() throws Exception {
        Mockito.when(aiChatService.getPatientConversations(2L)).thenReturn(List.of(sampleConversation));

        mockMvc.perform(get("/v1/api/ai-chat/conversations/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].conversationId", is("conv-123")))
                .andExpect(jsonPath("$[0].title", is("General Chat")));

        Mockito.verify(aiChatService).getPatientConversations(2L);
    }

    @Test
    @DisplayName("GET /conversations/{patientId} - empty list returns 200 with empty array")
    void getPatientConversations_empty_returns200() throws Exception {
        Mockito.when(aiChatService.getPatientConversations(99L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/api/ai-chat/conversations/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /conversations/{patientId} - service throws returns 400")
    void getPatientConversations_serviceThrows_returns400() throws Exception {
        Mockito.when(aiChatService.getPatientConversations(anyLong()))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/v1/api/ai-chat/conversations/2"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // GET /v1/api/ai-chat/conversation/{conversationId}/messages
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /conversation/{conversationId}/messages - success returns 200 with messages")
    void getConversationMessages_success_returns200() throws Exception {
        Mockito.when(aiChatService.getConversationMessages("conv-123"))
                .thenReturn(List.of(sampleMessage));

        mockMvc.perform(get("/v1/api/ai-chat/conversation/conv-123/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].messageId", is(10)))
                .andExpect(jsonPath("$[0].content", is("Hello!")));

        Mockito.verify(aiChatService).getConversationMessages("conv-123");
    }

    @Test
    @DisplayName("GET /conversation/{conversationId}/messages - service throws returns 400")
    void getConversationMessages_serviceThrows_returns400() throws Exception {
        Mockito.when(aiChatService.getConversationMessages(anyString()))
                .thenThrow(new RuntimeException("Conversation not found"));

        mockMvc.perform(get("/v1/api/ai-chat/conversation/bad-id/messages"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // GET /v1/api/ai-chat/history
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /history - with conversationId fetches messages for that conversation")
    void getConversationHistory_withConversationId_returns200() throws Exception {
        Mockito.when(aiChatService.getConversationMessages("conv-123"))
                .thenReturn(List.of(sampleMessage));

        mockMvc.perform(get("/v1/api/ai-chat/history")
                        .param("userId", "1")
                        .param("conversationId", "conv-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].content", is("Hello!")));

        Mockito.verify(aiChatService).getConversationMessages("conv-123");
    }

    @Test
    @DisplayName("GET /history - without conversationId calls getRecentMessagesForUser with default limit")
    void getConversationHistory_withoutConversationId_callsRecentMessages() throws Exception {
        Mockito.when(aiChatService.getRecentMessagesForUser(1L, 50))
                .thenReturn(Collections.emptyList());
        Mockito.when(chatConversationRepository
                .findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/api/ai-chat/history")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(0)));

        Mockito.verify(aiChatService).getRecentMessagesForUser(1L, 50);
    }

    @Test
    @DisplayName("GET /history - custom limit is forwarded to service")
    void getConversationHistory_customLimit_passedToService() throws Exception {
        Mockito.when(aiChatService.getRecentMessagesForUser(1L, 10))
                .thenReturn(Collections.emptyList());
        Mockito.when(chatConversationRepository
                .findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/v1/api/ai-chat/history")
                        .param("userId", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk());

        Mockito.verify(aiChatService).getRecentMessagesForUser(1L, 10);
    }

    @Test
    @DisplayName("GET /history - service throws returns 400")
    void getConversationHistory_serviceThrows_returns400() throws Exception {
        Mockito.when(aiChatService.getRecentMessagesForUser(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/v1/api/ai-chat/history")
                        .param("userId", "1"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // POST /v1/api/ai-chat/conversation/{conversationId}/deactivate
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /conversation/{conversationId}/deactivate - success returns 200")
    void deactivateConversation_success_returns200() throws Exception {
        Mockito.doNothing().when(aiChatService).deactivateConversation("conv-123");

        mockMvc.perform(post("/v1/api/ai-chat/conversation/conv-123/deactivate"))
                .andExpect(status().isOk());

        Mockito.verify(aiChatService).deactivateConversation("conv-123");
    }

    @Test
    @DisplayName("POST /conversation/{conversationId}/deactivate - service throws returns 400")
    void deactivateConversation_serviceThrows_returns400() throws Exception {
        Mockito.doThrow(new RuntimeException("Conversation not found"))
                .when(aiChatService).deactivateConversation(anyString());

        mockMvc.perform(post("/v1/api/ai-chat/conversation/bad-id/deactivate"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // GET /v1/api/ai-chat/config
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /config - with patientId returns 200 with AI config")
    void getUserAIConfig_withPatientId_returns200() throws Exception {
        Mockito.when(userAIConfigService.getUserAIConfig(1L, 2L)).thenReturn(sampleConfig);

        mockMvc.perform(get("/v1/api/ai-chat/config")
                        .param("userId", "1")
                        .param("patientId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.patientId", is(2)));

        Mockito.verify(userAIConfigService).getUserAIConfig(1L, 2L);
    }

    @Test
    @DisplayName("GET /config - without patientId passes null to service")
    void getUserAIConfig_withoutPatientId_passesNullToService() throws Exception {
        Mockito.when(userAIConfigService.getUserAIConfig(eq(1L), isNull()))
                .thenReturn(sampleConfig);

        mockMvc.perform(get("/v1/api/ai-chat/config")
                        .param("userId", "1"))
                .andExpect(status().isOk());

        Mockito.verify(userAIConfigService).getUserAIConfig(eq(1L), isNull());
    }

    @Test
    @DisplayName("GET /config - service throws returns 400")
    void getUserAIConfig_serviceThrows_returns400() throws Exception {
        Mockito.when(userAIConfigService.getUserAIConfig(anyLong(), any()))
                .thenThrow(new RuntimeException("Config not found"));

        mockMvc.perform(get("/v1/api/ai-chat/config")
                        .param("userId", "1"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // POST /v1/api/ai-chat/config
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /config - new config (no id) returns 201 Created")
    void saveUserAIConfig_newConfig_returns201() throws Exception {
        UserAIConfigDTO newConfig = UserAIConfigDTO.builder()
                .userId(1L)
                .patientId(2L)
                .aiProvider(AIProvider.OPENAI)
                .build(); // id is null → treated as new

        Mockito.when(userAIConfigService.saveUserAIConfig(any(UserAIConfigDTO.class)))
                .thenReturn(sampleConfig);

        mockMvc.perform(post("/v1/api/ai-chat/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConfig)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @DisplayName("POST /config - existing config (with id) returns 200 OK")
    void saveUserAIConfig_existingConfig_returns200() throws Exception {
        Mockito.when(userAIConfigService.saveUserAIConfig(any(UserAIConfigDTO.class)))
                .thenReturn(sampleConfig);

        mockMvc.perform(post("/v1/api/ai-chat/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @DisplayName("POST /config - service throws returns 400")
    void saveUserAIConfig_serviceThrows_returns400() throws Exception {
        Mockito.when(userAIConfigService.saveUserAIConfig(any(UserAIConfigDTO.class)))
                .thenThrow(new RuntimeException("Validation error"));

        mockMvc.perform(post("/v1/api/ai-chat/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleConfig)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // DELETE /v1/api/ai-chat/config
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /config - with patientId returns 200")
    void deactivateUserAIConfig_withPatientId_returns200() throws Exception {
        Mockito.doNothing().when(userAIConfigService).deactivateUserAIConfig(1L, 2L);

        mockMvc.perform(delete("/v1/api/ai-chat/config")
                        .param("userId", "1")
                        .param("patientId", "2"))
                .andExpect(status().isOk());

        Mockito.verify(userAIConfigService).deactivateUserAIConfig(1L, 2L);
    }

    @Test
    @DisplayName("DELETE /config - without patientId passes null to service")
    void deactivateUserAIConfig_withoutPatientId_passesNullToService() throws Exception {
        Mockito.doNothing().when(userAIConfigService).deactivateUserAIConfig(eq(1L), isNull());

        mockMvc.perform(delete("/v1/api/ai-chat/config")
                        .param("userId", "1"))
                .andExpect(status().isOk());

        Mockito.verify(userAIConfigService).deactivateUserAIConfig(eq(1L), isNull());
    }

    @Test
    @DisplayName("DELETE /config - service throws returns 400")
    void deactivateUserAIConfig_serviceThrows_returns400() throws Exception {
        Mockito.doThrow(new RuntimeException("Config not found"))
                .when(userAIConfigService).deactivateUserAIConfig(anyLong(), any());

        mockMvc.perform(delete("/v1/api/ai-chat/config")
                        .param("userId", "1"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // GET /v1/api/ai-chat/retention-policy
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /retention-policy - success returns 200 with policy info")
    void getRetentionPolicy_success_returns200() throws Exception {
        Mockito.when(chatCleanupService.getRetentionPolicyInfo())
                .thenReturn("Conversations are retained for 90 days.");

        mockMvc.perform(get("/v1/api/ai-chat/retention-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retentionPolicy",
                        is("Conversations are retained for 90 days.")));
    }

    @Test
    @DisplayName("GET /retention-policy - service throws returns 500 with error message")
    void getRetentionPolicy_serviceThrows_returns500() throws Exception {
        Mockito.when(chatCleanupService.getRetentionPolicyInfo())
                .thenThrow(new RuntimeException("Policy unavailable"));

        mockMvc.perform(get("/v1/api/ai-chat/retention-policy"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error",
                        is("Unable to retrieve retention policy information")));
    }
}
