package com.careconnect.controller;

import com.careconnect.dto.chat.AiRequest;
import com.careconnect.dto.invoice.InvoiceDto;
import com.careconnect.dto.invoice.InvoiceResponseDto;
import com.careconnect.dto.invoice.PaymentDto;
import com.careconnect.model.invoice.Invoice;
import com.careconnect.service.invoice.InvoiceService;
import com.careconnect.service.invoice.LlmExtractionService;
import com.careconnect.service.invoice.TextractService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock private TextractService textractService;
    @Mock private LlmExtractionService llmExtractionService;
    @Mock private InvoiceService invoiceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Helper: controller with all services wired
    private InvoiceController controller() {
        return new InvoiceController(textractService, llmExtractionService, invoiceService, objectMapper);
    }

    // Helper: controller with textract=null (AWS disabled)
    private InvoiceController controllerNoTextract() {
        return new InvoiceController(null, null, invoiceService, objectMapper);
    }

    // ─── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_nullSort_returnsPageBody() {
        InvoiceDto dto = new InvoiceDto();
        dto.id = "inv-1";
        Page<InvoiceDto> page = new PageImpl<>(List.of(dto));
        when(invoiceService.list(any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Map<String, Object>> response = controller().list(
                null, null, null, null, null, null, null, null, null, 0, 25
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("totalItems")).isEqualTo(1L);
        assertThat(body.get("page")).isEqualTo(0);
        assertThat(body.get("pageSize")).isEqualTo(1);
        assertThat(body.get("totalPages")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<InvoiceDto> items = (List<InvoiceDto>) body.get("items");
        assertThat(items).hasSize(1);
    }

    @Test
    void list_dueDescSort_delegatesCorrectly() {
        Page<InvoiceDto> page = new PageImpl<>(List.of());
        when(invoiceService.list(any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Map<String, Object>> response = controller().list(
                "search", "pending", "Provider", "Patient",
                "2024-01-01T00:00:00Z", "2024-12-31T00:00:00Z",
                "10.00", "500.00", "due_desc", 1, 10
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void list_dueAscSort_delegatesCorrectly() {
        Page<InvoiceDto> page = new PageImpl<>(List.of());
        when(invoiceService.list(any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Map<String, Object>> response = controller().list(
                null, null, null, null, null, null, null, null, "due_asc", 0, 25
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void list_amountDescSort_delegatesCorrectly() {
        Page<InvoiceDto> page = new PageImpl<>(List.of());
        when(invoiceService.list(any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Map<String, Object>> response = controller().list(
                null, null, null, null, null, null, null, null, "amount_desc", 0, 25
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void list_amountAscSort_delegatesCorrectly() {
        Page<InvoiceDto> page = new PageImpl<>(List.of());
        when(invoiceService.list(any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Map<String, Object>> response = controller().list(
                null, null, null, null, null, null, null, null, "amount_asc", 0, 25
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void list_unknownSort_usesDefaultSort() {
        Page<InvoiceDto> page = new PageImpl<>(List.of());
        when(invoiceService.list(any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Map<String, Object>> response = controller().list(
                null, null, null, null, null, null, null, null, "unknown_sort", 0, 25
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ─── get ──────────────────────────────────────────────────────────────────

    @Test
    void get_found_returnsDto() {
        InvoiceDto dto = new InvoiceDto();
        dto.id = "inv-42";
        when(invoiceService.get("inv-42")).thenReturn(Optional.of(dto));

        ResponseEntity<InvoiceDto> response = controller().get("inv-42");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
    }

    @Test
    void get_notFound_returns404() {
        when(invoiceService.get("missing")).thenReturn(Optional.empty());

        ResponseEntity<InvoiceDto> response = controller().get("missing");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    void create_returnsCreatedWithDto() {
        InvoiceDto input = new InvoiceDto();
        InvoiceDto created = new InvoiceDto();
        created.id = "new-id";
        when(invoiceService.create(input)).thenReturn(created);

        ResponseEntity<InvoiceDto> response = controller().create(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    void update_returnsOkWithUpdatedDto() {
        InvoiceDto input = new InvoiceDto();
        InvoiceDto updated = new InvoiceDto();
        updated.id = "inv-1";
        when(invoiceService.update("inv-1", input)).thenReturn(updated);

        ResponseEntity<InvoiceDto> response = controller().update("inv-1", input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(updated);
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_returnsNoContent() {
        doNothing().when(invoiceService).delete("inv-1");

        ResponseEntity<Void> response = controller().delete("inv-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(invoiceService).delete("inv-1");
    }

    // ─── addPayment ───────────────────────────────────────────────────────────

    @Test
    void addPayment_withPrincipal_usesNameAsActor() {
        PaymentDto dto = new PaymentDto();
        InvoiceDto updated = new InvoiceDto();
        Principal principal = () -> "user@example.com";
        when(invoiceService.recordPayment("inv-1", dto, "user@example.com")).thenReturn(updated);

        ResponseEntity<InvoiceDto> response = controller().addPayment("inv-1", dto, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(updated);
        verify(invoiceService).recordPayment("inv-1", dto, "user@example.com");
    }

    @Test
    void addPayment_nullPrincipal_usesSystemAsActor() {
        PaymentDto dto = new PaymentDto();
        InvoiceDto updated = new InvoiceDto();
        when(invoiceService.recordPayment("inv-1", dto, "system")).thenReturn(updated);

        ResponseEntity<InvoiceDto> response = controller().addPayment("inv-1", dto, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(invoiceService).recordPayment("inv-1", dto, "system");
    }

    // ─── removePayment ────────────────────────────────────────────────────────

    @Test
    void removePayment_returnsOkWithUpdatedDto() {
        InvoiceDto updated = new InvoiceDto();
        when(invoiceService.deletePayment("inv-1", "pay-1")).thenReturn(updated);

        ResponseEntity<InvoiceDto> response = controller().removePayment("inv-1", "pay-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(updated);
    }

    // ─── extractWithLlm ───────────────────────────────────────────────────────

    @Test
    void extractWithLlm_nullFiles_returnsBadRequest() {
        ResponseEntity<?> response = controller().extractWithLlm(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Please provide at least one valid file.");
    }

    @Test
    void extractWithLlm_emptyList_returnsBadRequest() {
        ResponseEntity<?> response = controller().extractWithLlm(List.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void extractWithLlm_allEmptyFiles_returnsBadRequest() {
        MockMultipartFile emptyFile = new MockMultipartFile("files", new byte[0]);

        ResponseEntity<?> response = controller().extractWithLlm(List.of(emptyFile));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void extractWithLlm_textractUnavailable_returns503() {
        MockMultipartFile file = new MockMultipartFile("files", "test.pdf", "application/pdf", new byte[]{1, 2, 3});

        ResponseEntity<?> response = controllerNoTextract().extractWithLlm(List.of(file));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().toString()).contains("Textract service is not available");
    }

    @Test
    void extractWithLlm_successNoDuplicate_returnsOkPayload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "invoice.pdf", "application/pdf", new byte[]{1, 2, 3});
        AiRequest.AnalysisResult analysisResult = new AiRequest.AnalysisResult("raw text", "s3://bucket/key");
        when(textractService.analyzeAndGetResult(anyList())).thenReturn(analysisResult);

        String json = "{\"invoiceNumber\":\"INV-001\"}";
        when(llmExtractionService.extractInvoiceData("raw text")).thenReturn(json);
        when(invoiceService.findDuplicateByProviderAndTotal(any(), any(), any())).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller().extractWithLlm(List.of(file));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        InvoiceResponseDto payload = (InvoiceResponseDto) response.getBody();
        assertThat(payload).isNotNull();
        assertThat(payload.duplicate).isFalse();
        assertThat(payload.message).isNull();
        assertThat(payload.duplicateId).isNull();
        assertThat(payload.duplicateInvoiceNumber).isNull();
        assertThat(payload.invoice.documentLink).isEqualTo("s3://bucket/key");
        assertThat(payload.invoice.invoiceNumber).isEqualTo("INV-001");
    }

    @Test
    void extractWithLlm_duplicateFound_flagsDuplicateInResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "invoice.pdf", "application/pdf", new byte[]{1, 2, 3});
        AiRequest.AnalysisResult analysisResult = new AiRequest.AnalysisResult("raw text", "s3://bucket/key");
        when(textractService.analyzeAndGetResult(anyList())).thenReturn(analysisResult);

        String json = "{\"invoiceNumber\":\"INV-001\",\"provider\":{\"name\":\"Acme\"},\"amounts\":{\"total\":100.0}}";
        when(llmExtractionService.extractInvoiceData("raw text")).thenReturn(json);

        Invoice existing = new Invoice();
        existing.setId("existing-id");
        existing.setInvoiceNumber("INV-001");
        when(invoiceService.findDuplicateByProviderAndTotal("Acme", 100.0, "INV-001"))
                .thenReturn(Optional.of(existing));

        ResponseEntity<?> response = controller().extractWithLlm(List.of(file));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        InvoiceResponseDto payload = (InvoiceResponseDto) response.getBody();
        assertThat(payload.duplicate).isTrue();
        assertThat(payload.duplicateId).isEqualTo("existing-id");
        assertThat(payload.duplicateInvoiceNumber).isEqualTo("INV-001");
        assertThat(payload.message).contains("Duplicate invoice detected");
        assertThat(payload.message).contains("Acme");
    }

    @Test
    void extractWithLlm_duplicateNullProviderAndTotal_messageUsesUnknown() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "invoice.pdf", "application/pdf", new byte[]{1, 2, 3});
        AiRequest.AnalysisResult analysisResult = new AiRequest.AnalysisResult("raw text", "s3://key");
        when(textractService.analyzeAndGetResult(anyList())).thenReturn(analysisResult);

        // JSON with no provider or amounts — they'll be null in InvoiceDto
        String json = "{\"invoiceNumber\":\"INV-002\"}";
        when(llmExtractionService.extractInvoiceData("raw text")).thenReturn(json);

        Invoice existing = new Invoice();
        existing.setId("dup-id");
        existing.setInvoiceNumber("INV-002");
        when(invoiceService.findDuplicateByProviderAndTotal(null, null, "INV-002"))
                .thenReturn(Optional.of(existing));

        ResponseEntity<?> response = controller().extractWithLlm(List.of(file));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        InvoiceResponseDto payload = (InvoiceResponseDto) response.getBody();
        assertThat(payload.duplicate).isTrue();
        assertThat(payload.message).contains("(unknown provider)");
    }

    @Test
    void extractWithLlm_exceptionDuringProcessing_returns500() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "invoice.pdf", "application/pdf", new byte[]{1, 2, 3});
        when(textractService.analyzeAndGetResult(anyList())).thenThrow(new RuntimeException("AWS error"));

        ResponseEntity<?> response = controller().extractWithLlm(List.of(file));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().toString()).contains("Failed to process with LLM");
        assertThat(response.getBody().toString()).contains("AWS error");
    }

    @Test
    void extractWithLlm_fencedJsonResponse_strippedAndParsed() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "invoice.pdf", "application/pdf", new byte[]{1, 2, 3});
        AiRequest.AnalysisResult analysisResult = new AiRequest.AnalysisResult("raw text", "s3://key");
        when(textractService.analyzeAndGetResult(anyList())).thenReturn(analysisResult);

        String fencedJson = "```json\n{\"invoiceNumber\":\"INV-FENCED\"}\n```";
        when(llmExtractionService.extractInvoiceData("raw text")).thenReturn(fencedJson);
        when(invoiceService.findDuplicateByProviderAndTotal(any(), any(), any())).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller().extractWithLlm(List.of(file));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        InvoiceResponseDto payload = (InvoiceResponseDto) response.getBody();
        assertThat(payload.invoice.invoiceNumber).isEqualTo("INV-FENCED");
    }

    // ─── JsonSanitizer ────────────────────────────────────────────────────────

    @Test
    void jsonSanitizer_nullInput_returnsNull() {
        assertThat(InvoiceController.JsonSanitizer.extractFirstJsonObject(null)).isNull();
    }

    @Test
    void jsonSanitizer_noOpenBrace_returnsNull() {
        assertThat(InvoiceController.JsonSanitizer.extractFirstJsonObject("no json here")).isNull();
    }

    @Test
    void jsonSanitizer_simpleObject_extractsCorrectly() {
        assertThat(InvoiceController.JsonSanitizer.extractFirstJsonObject("{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void jsonSanitizer_nestedObject_extractsOutermost() {
        String input = "{\"outer\":{\"inner\":true}}";
        assertThat(InvoiceController.JsonSanitizer.extractFirstJsonObject(input)).isEqualTo(input);
    }

    @Test
    void jsonSanitizer_escapedBackslash_handledCorrectly() {
        String input = "{\"path\":\"C:\\\\Users\\\\test\"}";
        assertThat(InvoiceController.JsonSanitizer.extractFirstJsonObject(input)).isEqualTo(input);
    }

    @Test
    void jsonSanitizer_fencedJsonWithNewline_stripsAndExtracts() {
        String input = "```json\n{\"key\":\"value\"}\n```";
        assertThat(InvoiceController.JsonSanitizer.extractFirstJsonObject(input)).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void jsonSanitizer_fencedJsonNoNewline_stripsAndExtracts() {
        // ``` immediately followed by { (no newline after fence marker)
        String input = "```{\"key\":\"value\"}```";
        // firstNewline < 0 branch: t stays as the remainder after "```" trim
        String result = InvoiceController.JsonSanitizer.extractFirstJsonObject(input);
        assertThat(result).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void jsonSanitizer_unclosedObject_returnsNull() {
        assertThat(InvoiceController.JsonSanitizer.extractFirstJsonObject("{\"a\":1")).isNull();
    }

    @Test
    void jsonSanitizer_escapedQuoteInsideString_handledCorrectly() {
        String input = "{\"msg\":\"say \\\"hello\\\"\"}";
        assertThat(InvoiceController.JsonSanitizer.extractFirstJsonObject(input)).isEqualTo(input);
    }
}
