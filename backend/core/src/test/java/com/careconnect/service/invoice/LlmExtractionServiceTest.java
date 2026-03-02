package com.careconnect.service.invoice;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmExtractionServiceTest {

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private LlmExtractionService llmExtractionService;

    @Test
    void extractInvoiceData_validResponse_returnsTrimedText() {
        ChatResponse chatResponse = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);
        when(chatResponse.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.text()).thenReturn("  {\"invoiceNumber\": \"INV-001\"}  ");
        when(chatModel.chat(anyList())).thenReturn(chatResponse);

        String result = llmExtractionService.extractInvoiceData("Raw invoice text");

        assertThat(result).isEqualTo("{\"invoiceNumber\": \"INV-001\"}");
    }

    @Test
    void extractInvoiceData_nullResponse_returnsEmpty() {
        when(chatModel.chat(anyList())).thenReturn(null);

        String result = llmExtractionService.extractInvoiceData("Raw invoice text");

        assertThat(result).isEmpty();
    }

    @Test
    void extractInvoiceData_nullAiMessage_returnsEmpty() {
        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.aiMessage()).thenReturn(null);
        when(chatModel.chat(anyList())).thenReturn(chatResponse);

        String result = llmExtractionService.extractInvoiceData("Raw invoice text");

        assertThat(result).isEmpty();
    }

    @Test
    void extractInvoiceData_nullText_returnsEmpty() {
        ChatResponse chatResponse = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);
        when(chatResponse.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.text()).thenReturn(null);
        when(chatModel.chat(anyList())).thenReturn(chatResponse);

        String result = llmExtractionService.extractInvoiceData("Raw invoice text");

        assertThat(result).isEmpty();
    }

    @Test
    void extractInvoiceData_emptyText_returnsEmpty() {
        ChatResponse chatResponse = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);
        when(chatResponse.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.text()).thenReturn("");
        when(chatModel.chat(anyList())).thenReturn(chatResponse);

        String result = llmExtractionService.extractInvoiceData("Raw invoice text");

        assertThat(result).isEmpty();
    }

    @Test
    void extractInvoiceData_whitespaceOnlyText_returnsEmpty() {
        ChatResponse chatResponse = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);
        when(chatResponse.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.text()).thenReturn("   ");
        when(chatModel.chat(anyList())).thenReturn(chatResponse);

        String result = llmExtractionService.extractInvoiceData("Raw invoice text");

        assertThat(result).isEmpty();
    }
}
