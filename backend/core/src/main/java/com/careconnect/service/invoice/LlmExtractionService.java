
package com.careconnect.service.invoice;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "careconnect.llm.enabled", havingValue = "true", matchIfMissing = false)
public class LlmExtractionService {

    private final @Qualifier("chatModel") ChatModel chatModel;

    /**
     * Returns the raw JSON string produced by the LLM.
     * You can persist this or map it to Invoice using Jackson.
     */
    public String extractInvoiceData(String rawInvoiceText) {
        String systemMessageText = "You are an expert data extraction assistant. Extract invoice data from the provided text.\n"
                + "\n"
                + "CRITICAL INSTRUCTIONS:\n"
                + "- Output must be a single valid JSON object exactly matching the schema below.\n"
                + "- Return empty strings (\"\") for fields where no data is found.\n"
                + "- If a field can have multiple values, include all relevant items.\n"
                + "- Numeric fields must be valid numbers. Do not include currency symbols.\n"
                + "- Dates must be in ISO 8601 format.\n"
                + "- Do not include any explanation or text outside the JSON.\n"
                + "- If something is unknown, use an empty string or empty array as appropriate.\n"
                + "\n"
                + "FIELD LOGIC:\n"
                + "1. payment_status:\n"
                + "   - \"pending\": amountDue > 0 and no payment date mentioned\n"
                + "   - \"paid\": amountDue = 0 and payment date exists\n"
                + "   - \"partial\": partial payment mentioned but balance remains\n"
                + "   - \"overdue\": past due date mentioned\n"
                + "   - \"cancelled\": invoice explicitly cancelled\n"
                + "2. billed_to_insurance:\n"
                + "   - true if insurance companies, adjustments, or insurance payments are mentioned\n"
                + "   - false otherwise\n"
                + "3. supported_methods mapping:\n"
                + "   - \"Visa\", \"MasterCard\", \"American Express\", \"Discover\" -> \"CreditCard\"\n"
                + "   - \"Apple Pay\" -> \"ApplePay\"\n"
                + "   - \"Google Pay\" -> \"GooglePay\"\n"
                + "   - \"eCheck\", \"bank transfer\" -> \"ACH\"\n"
                + "   - \"check\" -> \"Check\"\n"
                + "   - \"cash\" -> \"Cash\"\n"
                + "   - \"PayPal\", \"Venmo\" -> keep as is\n"
                + "4. patient.accountNumber: patient ID or account number if present\n"
                + "5. patient.billingAddress: address where payments should be sent\n"
                + "6. checkPayableTo.reference: any reference number for check payments\n"
                + "7. dates: include relevant dates including serviceDate if available\n"
                + "8. services: include all items with description, date, charges, adjustments\n"
                + "9. aiSummary: REQUIRED 1-2 sentence summary of provider, patient, services, amounts, due date, status\n"
                + "10. recommendedActions: REQUIRED 3-5 next steps for the patient based on content\n"
                + "\n"
                + "ADDITIONAL FIELDS:\n"
                + "- createdAt: empty string unless creation date is explicitly mentioned\n"
                + "- updatedAt: empty string unless update date is explicitly mentioned\n"
                + "- history: empty array unless transaction history exists\n"
                + "\n"
                + "EXTRACTION SCHEMA:\n"
                + "{\n"
                + "  \"id\": \"\",\n"
                + "  \"invoiceNumber\": \"\",\n"
                + "  \"provider\": {\n"
                + "    \"name\": \"\",\n"
                + "    \"address\": \"\",\n"
                + "    \"phone\": \"\",\n"
                + "    \"email\": \"\"\n"
                + "  },\n"
                + "  \"patient\": {\n"
                + "    \"name\": \"\",\n"
                + "    \"address\": \"\",\n"
                + "    \"accountNumber\": \"\",\n"
                + "    \"billingAddress\": \"\"\n"
                + "  },\n"
                + "  \"dates\": {\n"
                + "    \"statementDate\": \"\",\n"
                + "    \"dueDate\": \"\",\n"
                + "    \"paidDate\": \"\"\n"
                + "  },\n"
                + "  \"services\": [\n"
                + "    {\n"
                + "      \"description\": \"\",\n"
                + "      \"serviceCode\": \"\",\n"
                + "      \"serviceDate\": \"\",\n"
                + "      \"charge\": 0.0,\n"
                + "      \"patientBalance\": 0.0,\n"
                + "      \"insuranceAdjustments\": 0.0\n"
                + "    }\n"
                + "  ],\n"
                + "  \"paymentStatus\": \"pending\",\n"
                + "  \"billedToInsurance\": false,\n"
                + "  \"amounts\": {\n"
                + "    \"totalCharges\": 0.0,\n"
                + "    \"totalAdjustments\": 0.0,\n"
                + "    \"total\": 0.0,\n"
                + "    \"amountDue\": 0.0\n"
                + "  },\n"
                + "  \"paymentReferences\": {\n"
                + "    \"paymentLink\": \"\",\n"
                + "    \"qrCodeUrl\": \"\",\n"
                + "    \"notes\": \"\",\n"
                + "    \"supportedMethods\": []\n"
                + "  },\n"
                + "  \"checkPayableTo\": {\n"
                + "    \"name\": \"\",\n"
                + "    \"address\": \"\",\n"
                + "    \"reference\": \"\"\n"
                + "  },\n"
                + "  \"aiSummary\": \"\",\n"
                + "  \"recommendedActions\": []\n"
                + "}\n";


        final var messages = List.of(
                SystemMessage.from(systemMessageText),
                new UserMessage(rawInvoiceText)
        );
        ChatResponse response = chatModel.chat(messages);
        String text = (response != null && response.aiMessage() != null)
                ? response.aiMessage().text()
                : "";
        return text == null ? "" : text.trim();
    }
}
