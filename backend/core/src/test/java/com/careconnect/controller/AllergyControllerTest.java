package com.careconnect.controller;

import com.careconnect.dto.AllergyDTO;
import com.careconnect.model.Allergy.AllergyType;
import com.careconnect.model.Allergy.AllergySeverity;
import com.careconnect.model.Patient;
import com.careconnect.model.User;
import com.careconnect.repository.PatientRepository;
import com.careconnect.repository.UserRepository;
import com.careconnect.security.Role;
import com.careconnect.service.AllergyService;
import com.careconnect.service.CaregiverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link AllergyController}.
 *
 * <p>
 * Uses {@link WebMvcTest} to test the controller layer in isolation with
 * mocked dependencies. Security filters are disabled so that the
 * {@link SecurityContextHolder} can be configured directly per test.
 * </p>
 */
@WebMvcTest(AllergyController.class)
@AutoConfigureMockMvc(addFilters = false)
class AllergyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AllergyService allergyService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PatientRepository patientRepository;

    @MockBean
    private CaregiverService caregiverService;

    @Autowired
    private ObjectMapper objectMapper;

    private AllergyDTO sampleAllergy;
    private User adminUser;
    private User patientUser;
    private Patient patient;

    @BeforeEach
    void setup() {
        sampleAllergy = AllergyDTO.builder()
                .id(1L)
                .patientId(10L)
                .allergen("Penicillin")
                .allergyType(AllergyType.MEDICATION)
                .severity(AllergySeverity.SEVERE)
                .reaction("Anaphylaxis")
                .notes("Avoid all penicillin-based antibiotics")
                .diagnosedDate("2023-01-15")
                .isActive(true)
                .build();

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(Role.ADMIN);

        patientUser = new User();
        patientUser.setId(10L);
        patientUser.setEmail("patient@test.com");
        patientUser.setRole(Role.PATIENT);

        patient = new Patient();
        patient.setId(10L);
        patient.setUser(patientUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------
    // Security context helpers
    // -----------------------------------------------------------------------

    /** Admin user — always passes hasAccessToPatient(). */
    private void mockAdminSecurityContext() {
        mockSecurityContext("admin@test.com", adminUser);
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
    }

    /** Patient user whose ID matches the patient's linked user — self-access allowed. */
    private void mockPatientSelfAccessContext() {
        mockSecurityContext("patient@test.com", patientUser);
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
    }

    /** Patient user whose ID does NOT match the patient — access denied (403). */
    private void mockForbiddenSecurityContext() {
        User other = new User();
        other.setId(99L);
        other.setEmail("other@test.com");
        other.setRole(Role.PATIENT);

        mockSecurityContext("other@test.com", other);
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
    }

    private void mockSecurityContext(String email, User user) {
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        SecurityContext secCtx = Mockito.mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }

    // -----------------------------------------------------------------------
    // POST /v1/api/allergies
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /v1/api/allergies - admin creates allergy, returns 201")
    void createAllergy_success() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.createAllergy(any(AllergyDTO.class))).thenReturn(sampleAllergy);

        mockMvc.perform(post("/v1/api/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAllergy)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("Allergy created successfully")))
                .andExpect(jsonPath("$.data.allergen", is("Penicillin")))
                .andExpect(jsonPath("$.data.severity", is("SEVERE")));

        Mockito.verify(allergyService).createAllergy(any(AllergyDTO.class));
    }

    @Test
    @DisplayName("POST /v1/api/allergies - patient creates own allergy, returns 201")
    void createAllergy_patientSelfAccess_success() throws Exception {
        mockPatientSelfAccessContext();
        when(allergyService.createAllergy(any(AllergyDTO.class))).thenReturn(sampleAllergy);

        mockMvc.perform(post("/v1/api/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAllergy)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("Allergy created successfully")));
    }

    @Test
    @DisplayName("POST /v1/api/allergies - unauthorized patient returns 403")
    void createAllergy_forbidden() throws Exception {
        mockForbiddenSecurityContext();

        mockMvc.perform(post("/v1/api/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAllergy)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Not authorized to manage allergies for this patient")));
    }

    @Test
    @DisplayName("POST /v1/api/allergies - duplicate allergy returns 400")
    void createAllergy_duplicateAllergy_badRequest() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.createAllergy(any(AllergyDTO.class)))
                .thenThrow(new IllegalArgumentException(
                        "Active allergy for 'Penicillin' already exists for this patient"));

        mockMvc.perform(post("/v1/api/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAllergy)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /v1/api/allergies - unexpected exception returns 500")
    void createAllergy_unexpectedError_returns500() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.createAllergy(any(AllergyDTO.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(post("/v1/api/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAllergy)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Failed to create allergy")));
    }

    // -----------------------------------------------------------------------
    // PUT /v1/api/allergies/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PUT /v1/api/allergies/{id} - updates allergy, returns 200")
    void updateAllergy_success() throws Exception {
        mockAdminSecurityContext();

        AllergyDTO updated = AllergyDTO.builder()
                .id(1L)
                .patientId(10L)
                .allergen("Penicillin")
                .severity(AllergySeverity.MODERATE)
                .isActive(true)
                .build();

        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));
        when(allergyService.updateAllergy(eq(1L), any(AllergyDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/v1/api/allergies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Allergy updated successfully")))
                .andExpect(jsonPath("$.data.allergen", is("Penicillin")))
                .andExpect(jsonPath("$.data.severity", is("MODERATE")));

        Mockito.verify(allergyService).updateAllergy(eq(1L), any(AllergyDTO.class));
    }

    @Test
    @DisplayName("PUT /v1/api/allergies/{id} - allergy not found returns 404")
    void updateAllergy_notFound() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergy(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/v1/api/allergies/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAllergy)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Allergy not found")));
    }

    @Test
    @DisplayName("PUT /v1/api/allergies/{id} - unauthorized user returns 403")
    void updateAllergy_forbidden() throws Exception {
        mockForbiddenSecurityContext();
        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));

        mockMvc.perform(put("/v1/api/allergies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAllergy)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Not authorized to manage allergies for this patient")));
    }

    @Test
    @DisplayName("PUT /v1/api/allergies/{id} - invalid data returns 400")
    void updateAllergy_badRequest() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));
        when(allergyService.updateAllergy(eq(1L), any(AllergyDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid allergy data"));

        mockMvc.perform(put("/v1/api/allergies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAllergy)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("PUT /v1/api/allergies/{id} - unexpected exception returns 500")
    void updateAllergy_unexpectedError_returns500() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));
        when(allergyService.updateAllergy(eq(1L), any(AllergyDTO.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(put("/v1/api/allergies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAllergy)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Failed to update allergy")));
    }

    // -----------------------------------------------------------------------
    // GET /v1/api/allergies/patient/{patientId}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /v1/api/allergies/patient/{patientId} - returns all allergies with 200")
    void getAllergiesForPatient_success() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergiesForPatient(10L)).thenReturn(List.of(sampleAllergy));

        mockMvc.perform(get("/v1/api/allergies/patient/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Allergies retrieved successfully")))
                .andExpect(jsonPath("$.data[0].allergen", is("Penicillin")))
                .andExpect(jsonPath("$.data[0].isActive", is(true)));

        Mockito.verify(allergyService).getAllergiesForPatient(10L);
    }

    @Test
    @DisplayName("GET /v1/api/allergies/patient/{patientId} - returns empty list when none exist")
    void getAllergiesForPatient_emptyList() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergiesForPatient(10L)).thenReturn(List.of());

        mockMvc.perform(get("/v1/api/allergies/patient/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("GET /v1/api/allergies/patient/{patientId} - unauthorized user returns 403")
    void getAllergiesForPatient_forbidden() throws Exception {
        mockForbiddenSecurityContext();

        mockMvc.perform(get("/v1/api/allergies/patient/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Not authorized to view allergies for this patient")));
    }

    @Test
    @DisplayName("GET /v1/api/allergies/patient/{patientId} - unexpected exception returns 500")
    void getAllergiesForPatient_unexpectedError_returns500() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergiesForPatient(10L))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/v1/api/allergies/patient/10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Failed to retrieve allergies")));
    }

    // -----------------------------------------------------------------------
    // GET /v1/api/allergies/patient/{patientId}/active
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /v1/api/allergies/patient/{patientId}/active - returns active allergies with 200")
    void getActiveAllergiesForPatient_success() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getActiveAllergiesForPatient(10L)).thenReturn(List.of(sampleAllergy));

        mockMvc.perform(get("/v1/api/allergies/patient/10/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Active allergies retrieved successfully")))
                .andExpect(jsonPath("$.data[0].allergen", is("Penicillin")));

        Mockito.verify(allergyService).getActiveAllergiesForPatient(10L);
    }

    @Test
    @DisplayName("GET /v1/api/allergies/patient/{patientId}/active - patient self-access succeeds")
    void getActiveAllergiesForPatient_patientSelfAccess() throws Exception {
        mockPatientSelfAccessContext();
        when(allergyService.getActiveAllergiesForPatient(10L)).thenReturn(List.of(sampleAllergy));

        mockMvc.perform(get("/v1/api/allergies/patient/10/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Active allergies retrieved successfully")));
    }

    @Test
    @DisplayName("GET /v1/api/allergies/patient/{patientId}/active - unauthorized user returns 403")
    void getActiveAllergiesForPatient_forbidden() throws Exception {
        mockForbiddenSecurityContext();

        mockMvc.perform(get("/v1/api/allergies/patient/10/active"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Not authorized to view allergies for this patient")));
    }

    @Test
    @DisplayName("GET /v1/api/allergies/patient/{patientId}/active - unexpected exception returns 500")
    void getActiveAllergiesForPatient_unexpectedError_returns500() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getActiveAllergiesForPatient(10L))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/v1/api/allergies/patient/10/active"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Failed to retrieve active allergies")));
    }

    // -----------------------------------------------------------------------
    // PATCH /v1/api/allergies/{id}/deactivate
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /v1/api/allergies/{id}/deactivate - deactivates allergy, returns 200")
    void deactivateAllergy_success() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));
        Mockito.doNothing().when(allergyService).deactivateAllergy(1L);

        mockMvc.perform(patch("/v1/api/allergies/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Allergy deactivated successfully")));

        Mockito.verify(allergyService).deactivateAllergy(1L);
    }

    @Test
    @DisplayName("PATCH /v1/api/allergies/{id}/deactivate - allergy not found returns 404")
    void deactivateAllergy_notFound() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergy(99L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/v1/api/allergies/99/deactivate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Allergy not found")));
    }

    @Test
    @DisplayName("PATCH /v1/api/allergies/{id}/deactivate - unauthorized user returns 403")
    void deactivateAllergy_forbidden() throws Exception {
        mockForbiddenSecurityContext();
        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));

        mockMvc.perform(patch("/v1/api/allergies/1/deactivate"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Not authorized to manage allergies for this patient")));
    }

    @Test
    @DisplayName("PATCH /v1/api/allergies/{id}/deactivate - service throws IllegalArgumentException returns 400")
    void deactivateAllergy_badRequest() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));
        Mockito.doThrow(new IllegalArgumentException("Allergy not found with id: 1"))
                .when(allergyService).deactivateAllergy(1L);

        mockMvc.perform(patch("/v1/api/allergies/1/deactivate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("PATCH /v1/api/allergies/{id}/deactivate - unexpected exception returns 500")
    void deactivateAllergy_unexpectedError_returns500() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));
        Mockito.doThrow(new RuntimeException("DB error"))
                .when(allergyService).deactivateAllergy(1L);

        mockMvc.perform(patch("/v1/api/allergies/1/deactivate"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Failed to deactivate allergy")));
    }

    // -----------------------------------------------------------------------
    // DELETE /v1/api/allergies/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /v1/api/allergies/{id} - deletes allergy, returns 200")
    void deleteAllergy_success() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));
        Mockito.doNothing().when(allergyService).deleteAllergy(1L);

        mockMvc.perform(delete("/v1/api/allergies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Allergy deleted successfully")));

        Mockito.verify(allergyService).deleteAllergy(1L);
    }

    @Test
    @DisplayName("DELETE /v1/api/allergies/{id} - allergy not found returns 404")
    void deleteAllergy_notFound() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergy(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/v1/api/allergies/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Allergy not found")));
    }

    @Test
    @DisplayName("DELETE /v1/api/allergies/{id} - unauthorized user returns 403")
    void deleteAllergy_forbidden() throws Exception {
        mockForbiddenSecurityContext();
        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));

        mockMvc.perform(delete("/v1/api/allergies/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Not authorized to manage allergies for this patient")));
    }

    @Test
    @DisplayName("DELETE /v1/api/allergies/{id} - service throws IllegalArgumentException returns 400")
    void deleteAllergy_badRequest() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));
        Mockito.doThrow(new IllegalArgumentException("Allergy not found with id: 1"))
                .when(allergyService).deleteAllergy(1L);

        mockMvc.perform(delete("/v1/api/allergies/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("DELETE /v1/api/allergies/{id} - unexpected exception returns 500")
    void deleteAllergy_unexpectedError_returns500() throws Exception {
        mockAdminSecurityContext();
        when(allergyService.getAllergy(1L)).thenReturn(Optional.of(sampleAllergy));
        Mockito.doThrow(new RuntimeException("DB error"))
                .when(allergyService).deleteAllergy(1L);

        mockMvc.perform(delete("/v1/api/allergies/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Failed to delete allergy")));
    }
}
