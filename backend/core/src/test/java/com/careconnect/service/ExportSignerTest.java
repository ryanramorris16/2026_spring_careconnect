package com.careconnect.service;

import com.careconnect.dto.ExportLinkDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ExportSigner}.
 */
class ExportSignerTest {

    private ExportSigner exportSigner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exportSigner = ExportSigner.builder().build();
    }

    // ──────────────────────────────────────────────
    //  generateSignedUrl
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("generateSignedUrl_validInputs_returnsUrlWithAllComponents")
    void generateSignedUrl_validInputs_returnsUrlWithAllComponents() {
        String fileName = "report.pdf";
        Long patientId = 42L;

        String url = exportSigner.generateSignedUrl(fileName, patientId);

        assertThat(url).startsWith("https://api.careconnect.com/exports/report.pdf");
        assertThat(url).contains("patientId=42");
        assertThat(url).contains("timestamp=");
        assertThat(url).contains("signature=");
    }

    @Test
    @DisplayName("generateSignedUrl_differentFileNames_producesDifferentUrls")
    void generateSignedUrl_differentFileNames_producesDifferentUrls() {
        String url1 = exportSigner.generateSignedUrl("file1.pdf", 1L);
        String url2 = exportSigner.generateSignedUrl("file2.pdf", 1L);

        assertThat(url1).contains("file1.pdf");
        assertThat(url2).contains("file2.pdf");
        assertThat(url1).isNotEqualTo(url2);
    }

    @Test
    @DisplayName("generateSignedUrl_differentPatientIds_producesDifferentPatientParams")
    void generateSignedUrl_differentPatientIds_producesDifferentPatientParams() {
        String url1 = exportSigner.generateSignedUrl("report.pdf", 1L);
        String url2 = exportSigner.generateSignedUrl("report.pdf", 2L);

        assertThat(url1).contains("patientId=1");
        assertThat(url2).contains("patientId=2");
    }

    @Test
    @DisplayName("generateSignedUrl_signatureIsHexString_containsValidHex")
    void generateSignedUrl_signatureIsHexString_containsValidHex() {
        String url = exportSigner.generateSignedUrl("test.pdf", 10L);

        // Extract the signature parameter value
        String signaturePart = url.substring(url.indexOf("signature=") + "signature=".length());
        // Hex string should only contain hex characters (and possible minus sign for negative hashCode)
        assertThat(signaturePart).matches("-?[0-9a-fA-F]+");
    }

    @Test
    @DisplayName("generateSignedUrl_urlFormatCorrect_matchesExpectedPattern")
    void generateSignedUrl_urlFormatCorrect_matchesExpectedPattern() {
        String url = exportSigner.generateSignedUrl("data.csv", 100L);

        // Verify the URL has the correct format:
        // baseUrl/fileName?patientId=X&timestamp=Y&signature=Z
        assertThat(url).matches(
                "https://api\\.careconnect\\.com/exports/data\\.csv\\?patientId=100&timestamp=\\d+&signature=-?[0-9a-fA-F]+");
    }

    @Test
    @DisplayName("generateSignedUrl_specialCharactersInFileName_includedInUrl")
    void generateSignedUrl_specialCharactersInFileName_includedInUrl() {
        String url = exportSigner.generateSignedUrl("my report (1).pdf", 5L);

        assertThat(url).contains("my report (1).pdf");
        assertThat(url).contains("patientId=5");
    }

    // ──────────────────────────────────────────────
    //  sign
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("sign_validRelativePath_returnsExportLinkDTOWithUrl")
    void sign_validRelativePath_returnsExportLinkDTOWithUrl() {
        String relativePath = "/exports/patient-42/report.pdf";

        ExportLinkDTO result = exportSigner.sign(relativePath);

        assertThat(result).isNotNull();
        assertThat(result.getUrl())
                .isEqualTo("https://files.careconnect.ai/exports/patient-42/report.pdf?sig=mock123");
    }

    @Test
    @DisplayName("sign_validRelativePath_returnsExportLinkDTOWithExpirationInFuture")
    void sign_validRelativePath_returnsExportLinkDTOWithExpirationInFuture() {
        Instant before = Instant.now();

        ExportLinkDTO result = exportSigner.sign("/exports/file.csv");

        Instant after = Instant.now();

        assertThat(result.getInstantExpiresAt()).isNotNull();
        // The expiration should be approximately 1 hour from now
        assertThat(result.getInstantExpiresAt()).isAfter(before.plus(Duration.ofMinutes(59)));
        assertThat(result.getInstantExpiresAt()).isBefore(after.plus(Duration.ofMinutes(61)));
    }

    @Test
    @DisplayName("sign_differentPaths_producesDifferentUrls")
    void sign_differentPaths_producesDifferentUrls() {
        ExportLinkDTO result1 = exportSigner.sign("/exports/file1.pdf");
        ExportLinkDTO result2 = exportSigner.sign("/exports/file2.pdf");

        assertThat(result1.getUrl()).contains("file1.pdf");
        assertThat(result2.getUrl()).contains("file2.pdf");
        assertThat(result1.getUrl()).isNotEqualTo(result2.getUrl());
    }

    @Test
    @DisplayName("sign_rootPath_returnsUrlWithBaseAndSig")
    void sign_rootPath_returnsUrlWithBaseAndSig() {
        ExportLinkDTO result = exportSigner.sign("/");

        assertThat(result.getUrl()).isEqualTo("https://files.careconnect.ai/?sig=mock123");
    }

    @Test
    @DisplayName("sign_emptyPath_returnsUrlWithBaseAndSig")
    void sign_emptyPath_returnsUrlWithBaseAndSig() {
        ExportLinkDTO result = exportSigner.sign("");

        assertThat(result.getUrl()).isEqualTo("https://files.careconnect.ai?sig=mock123");
    }

    // ──────────────────────────────────────────────
    //  constructor / builder
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("builder_createsInstance_nonNull")
    void builder_createsInstance_nonNull() {
        ExportSigner signer = ExportSigner.builder().build();
        assertThat(signer).isNotNull();
    }
}
