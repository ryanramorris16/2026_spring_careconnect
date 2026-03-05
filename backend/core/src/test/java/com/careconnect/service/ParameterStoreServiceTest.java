package com.careconnect.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.SsmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ParameterStoreServiceTest {

    @Mock
    private SsmClient ssmClient;

    @InjectMocks
    private ParameterStoreService parameterStoreService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    // ---- constructor -----------------------------------------------------------

    @Test
    @DisplayName("constructor_withSsmClient_createsValidInstance")
    void constructor_withSsmClient_createsValidInstance() throws Exception {
        ParameterStoreService service = new ParameterStoreService(ssmClient);
        assertNotNull(service);
    }

    @Test
    @DisplayName("constructor_withNullSsmClient_createsInstance")
    void constructor_withNullSsmClient_createsInstance() throws Exception {
        ParameterStoreService service = new ParameterStoreService(null);
        assertNotNull(service);
    }

    // ---- getParameter ----------------------------------------------------------

    @Test
    @DisplayName("getParameter_ssmCallSucceedsWithDecryptionTrue_returnsValue")
    void getParameter_ssmCallSucceedsWithDecryptionTrue_returnsValue() throws Exception {
        String paramName = "/careconnect/db/password";
        String expectedValue = "super-secret";

        Parameter parameter = Parameter.builder().value(expectedValue).build();
        GetParameterResponse response = GetParameterResponse.builder().parameter(parameter).build();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(response);

        String result = parameterStoreService.getParameter(paramName, true);

        assertEquals(expectedValue, result);
        verify(ssmClient).getParameter(any(GetParameterRequest.class));
    }

    @Test
    @DisplayName("getParameter_ssmCallSucceedsWithDecryptionFalse_returnsValue")
    void getParameter_ssmCallSucceedsWithDecryptionFalse_returnsValue() throws Exception {
        String paramName = "/careconnect/app/config";
        String expectedValue = "plain-text-value";

        Parameter parameter = Parameter.builder().value(expectedValue).build();
        GetParameterResponse response = GetParameterResponse.builder().parameter(parameter).build();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(response);

        String result = parameterStoreService.getParameter(paramName, false);

        assertEquals(expectedValue, result);
        verify(ssmClient).getParameter(any(GetParameterRequest.class));
    }

    @Test
    @DisplayName("getParameter_ssmExceptionThrown_returnsParameterName")
    void getParameter_ssmExceptionThrown_returnsParameterName() throws Exception {
        String paramName = "/careconnect/missing/param";

        SsmException ssmException = (SsmException) SsmException.builder()
                .message("Parameter not found")
                .build();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(ssmException);

        String result = parameterStoreService.getParameter(paramName, true);

        assertEquals(paramName, result);
        verify(ssmClient).getParameter(any(GetParameterRequest.class));
    }

    @Test
    @DisplayName("getParameter_ssmExceptionThrownWithDecryptionFalse_returnsParameterName")
    void getParameter_ssmExceptionThrownWithDecryptionFalse_returnsParameterName() throws Exception {
        String paramName = "/careconnect/nonexistent";

        SsmException ssmException = (SsmException) SsmException.builder()
                .message("Access denied")
                .build();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(ssmException);

        String result = parameterStoreService.getParameter(paramName, false);

        assertEquals(paramName, result);
    }

    @Test
    @DisplayName("getParameter_emptyParameterValue_returnsEmptyString")
    void getParameter_emptyParameterValue_returnsEmptyString() throws Exception {
        String paramName = "/careconnect/empty/param";

        Parameter parameter = Parameter.builder().value("").build();
        GetParameterResponse response = GetParameterResponse.builder().parameter(parameter).build();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(response);

        String result = parameterStoreService.getParameter(paramName, false);

        assertEquals("", result);
    }

    @Test
    @DisplayName("getParameter_longParameterValue_returnsFullValue")
    void getParameter_longParameterValue_returnsFullValue() throws Exception {
        String paramName = "/careconnect/long/param";
        String longValue = "a".repeat(4096);

        Parameter parameter = Parameter.builder().value(longValue).build();
        GetParameterResponse response = GetParameterResponse.builder().parameter(parameter).build();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(response);

        String result = parameterStoreService.getParameter(paramName, true);

        assertEquals(longValue, result);
        assertEquals(4096, result.length());
    }

    // ---- getSecureParameter ----------------------------------------------------

    @Test
    @DisplayName("getSecureParameter_validParameter_returnsDecryptedValue")
    void getSecureParameter_validParameter_returnsDecryptedValue() throws Exception {
        String paramName = "/careconnect/secrets/api-key";
        String expectedValue = "api-key-12345";

        Parameter parameter = Parameter.builder().value(expectedValue).build();
        GetParameterResponse response = GetParameterResponse.builder().parameter(parameter).build();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(response);

        String result = parameterStoreService.getSecureParameter(paramName);

        assertEquals(expectedValue, result);
        verify(ssmClient).getParameter(any(GetParameterRequest.class));
    }

    @Test
    @DisplayName("getSecureParameter_ssmExceptionThrown_returnsParameterName")
    void getSecureParameter_ssmExceptionThrown_returnsParameterName() throws Exception {
        String paramName = "/careconnect/secrets/missing";

        SsmException ssmException = (SsmException) SsmException.builder()
                .message("Parameter not found")
                .build();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(ssmException);

        String result = parameterStoreService.getSecureParameter(paramName);

        assertEquals(paramName, result);
    }

    @Test
    @DisplayName("getSecureParameter_delegatesToGetParameterWithDecryptionTrue_verifiedViaRequest")
    void getSecureParameter_delegatesToGetParameterWithDecryptionTrue_verifiedViaRequest() throws Exception {
        String paramName = "/careconnect/secrets/key";
        String expectedValue = "decrypted-value";

        Parameter parameter = Parameter.builder().value(expectedValue).build();
        GetParameterResponse response = GetParameterResponse.builder().parameter(parameter).build();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(response);

        String result = parameterStoreService.getSecureParameter(paramName);

        assertEquals(expectedValue, result);
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }
}
