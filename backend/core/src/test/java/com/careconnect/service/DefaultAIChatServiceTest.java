package com.careconnect.service;

import com.careconnect.dto.ChatRequest;
import com.careconnect.dto.ChatResponse;
import com.careconnect.model.Patient;
import com.careconnect.repository.*;
import com.careconnect.service.cache.AIChatCacheService;
import com.careconnect.service.security.InputSanitizationService;
import com.careconnect.service.security.LangChainGovernanceService;
import com.careconnect.service.security.ResponseSanitizationService;
import com.careconnect.service.security.SecurityAuditService;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAIChatServiceTest {

    @Mock private ChatModel chatModel;
    @Mock private UserAIConfigRepository userAIConfigRepository;
    @Mock private ChatConversationRepository chatConversationRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private MedicalContextService medicalContextService;
    @Mock private PatientContextRetrievalService patientContextRetrievalService;
    @Mock private ChatMemoryFactory chatMemoryFactory;
    @Mock private ChatAuditService chatAuditService;
    @Mock private CaregiverPatientLinkService caregiverPatientLinkService;
    @Mock private InputSanitizationService inputSanitizationService;
    @Mock private ResponseSanitizationService responseSanitizationService;
    @Mock private LangChainGovernanceService langChainGovernanceService;
    @Mock private AIChatCacheService cacheService;
    @Mock private SecurityAuditService securityAuditService;
    @Mock private DocumentProcessingService documentProcessingService;

    @InjectMocks
    private DefaultAIChatService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "provider", "deepseek");
        ReflectionTestUtils.setField(service, "configuredModelName", "deepseek-chat");
    }

    @Test
    void processChat_bothIdsNull_throwsIllegalArgumentException() {
        ChatRequest request = new ChatRequest();

        assertThatThrownBy(() -> service.processChat(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either Patient ID or User ID is required");
    }

    @Test
    void processChat_patientNotFound_throwsIllegalArgumentException() {
        ChatRequest request = new ChatRequest();
        request.setPatientId(99L);
        when(cacheService.findPatient(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processChat(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void processChat_nullUserId_returnsErrorResponse() {
        ChatRequest request = new ChatRequest();
        request.setPatientId(1L);
        when(cacheService.findPatient(1L)).thenReturn(Optional.of(new Patient()));

        ChatResponse response = service.processChat(request);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("User ID is missing");
    }

    @Test
    void processChat_blankMessageNoFiles_returnsErrorResponse() {
        ChatRequest request = new ChatRequest();
        request.setPatientId(1L);
        request.setUserId(10L);
        request.setMessage("   ");
        when(cacheService.findPatient(1L)).thenReturn(Optional.of(new Patient()));

        ChatResponse response = service.processChat(request);

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("Message content");
    }

    @Test
    void getPatientConversations_noConversations_returnsEmptyList() {
        when(chatConversationRepository.findByPatientIdAndIsActiveTrueOrderByUpdatedAtDesc(5L))
                .thenReturn(List.of());

        assertThat(service.getPatientConversations(5L)).isEmpty();
    }

    @Test
    void getConversationMessages_notFound_throwsIllegalArgumentException() {
        when(chatConversationRepository.findByConversationIdAndIsActiveTrue("missing-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getConversationMessages("missing-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conversation not found");
    }

    @Test
    void getRecentMessagesForUser_noConversations_returnsEmptyList() {
        when(chatConversationRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(7L))
                .thenReturn(List.of());

        assertThat(service.getRecentMessagesForUser(7L, 10)).isEmpty();
    }

    @Test
    void deactivateConversation_notFound_throwsIllegalArgumentException() {
        when(chatConversationRepository.findByConversationIdAndIsActiveTrue("missing-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateConversation("missing-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conversation not found");
    }
}
