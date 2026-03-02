package com.careconnect.controller;

import com.careconnect.dto.MedicationDTO;
import com.careconnect.service.MedicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicationControllerTest {

    @Mock private MedicationService medicationService;

    @InjectMocks
    private MedicationController controller;

    private static final Long PATIENT_ID    = 1L;
    private static final Long MEDICATION_ID = 42L;
    private static final Long CAREGIVER_ID  = 7L;

    private MedicationDTO dto(String name) {
        MedicationDTO d = new MedicationDTO(null, null, name, null, null, null, null, null, null, null, null, null, null);
        return d;
    }

    // ─── getAllMedications ─────────────────────────────────────────────────────

    @Test
    void getAllMedications_returnsListFromService() {
        List<MedicationDTO> meds = List.of(dto("Med-A"), dto("Med-B"));
        when(medicationService.getAllMedicationsForPatient(PATIENT_ID)).thenReturn(meds);

        ResponseEntity<List<MedicationDTO>> response = controller.getAllMedications(PATIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(meds);
    }

    @Test
    void getAllMedications_emptyList_returnsOkWithEmptyBody() {
        when(medicationService.getAllMedicationsForPatient(PATIENT_ID)).thenReturn(List.of());

        ResponseEntity<List<MedicationDTO>> response = controller.getAllMedications(PATIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ─── getActiveMedications ─────────────────────────────────────────────────

    @Test
    void getActiveMedications_returnsActiveList() {
        List<MedicationDTO> active = List.of(dto("Active-Med"));
        when(medicationService.getActiveMedicationsForPatient(PATIENT_ID)).thenReturn(active);

        ResponseEntity<List<MedicationDTO>> response = controller.getActiveMedications(PATIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(active);
    }

    @Test
    void getActiveMedications_emptyList_returnsOk() {
        when(medicationService.getActiveMedicationsForPatient(PATIENT_ID)).thenReturn(List.of());

        ResponseEntity<List<MedicationDTO>> response = controller.getActiveMedications(PATIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ─── getPendingMedications ────────────────────────────────────────────────

    @Test
    void getPendingMedications_returnsPendingList() {
        List<MedicationDTO> pending = List.of(dto("Pending-Med"));
        when(medicationService.getPendingMedications(PATIENT_ID)).thenReturn(pending);

        ResponseEntity<List<MedicationDTO>> response = controller.getPendingMedications(PATIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(pending);
    }

    @Test
    void getPendingMedications_emptyList_returnsOk() {
        when(medicationService.getPendingMedications(PATIENT_ID)).thenReturn(List.of());

        ResponseEntity<List<MedicationDTO>> response = controller.getPendingMedications(PATIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ─── addMedication ────────────────────────────────────────────────────────

    @Test
    void addMedication_delegatesToServiceAndReturnsCreated() {
        MedicationDTO input = dto("New-Med");
        MedicationDTO created = dto("New-Med");
        when(medicationService.addMedication(PATIENT_ID, input)).thenReturn(created);

        ResponseEntity<MedicationDTO> response = controller.addMedication(PATIENT_ID, input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(created);
        verify(medicationService).addMedication(PATIENT_ID, input);
    }

    // ─── approveMedication ────────────────────────────────────────────────────

    @Test
    void approveMedication_returnsOkWithMessageAndApprovedDto() {
        MedicationDTO approved = dto("Approved-Med");
        when(medicationService.approveMedication(PATIENT_ID, MEDICATION_ID)).thenReturn(approved);

        ResponseEntity<?> response = controller.approveMedication(PATIENT_ID, MEDICATION_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo("Medication approved successfully");
        assertThat(body.get("approvedMedication")).isEqualTo(approved);
        verify(medicationService).approveMedication(PATIENT_ID, MEDICATION_ID);
    }

    // ─── deleteMedication ─────────────────────────────────────────────────────

    @Test
    void deleteMedication_deactivatesAndReturnsMessage() {
        doNothing().when(medicationService).deactivateMedication(PATIENT_ID, MEDICATION_ID);

        ResponseEntity<?> response = controller.deleteMedication(PATIENT_ID, MEDICATION_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo("Medication removed and notification sent");
        verify(medicationService).deactivateMedication(PATIENT_ID, MEDICATION_ID);
    }

    // ─── deleteMedicationByCaregiver ──────────────────────────────────────────

    @Test
    void deleteMedicationByCaregiver_hardDeletesAndReturnsMessage() {
        doNothing().when(medicationService).hardDeleteMedication(PATIENT_ID, MEDICATION_ID, CAREGIVER_ID);

        ResponseEntity<?> response = controller.deleteMedicationByCaregiver(PATIENT_ID, MEDICATION_ID, CAREGIVER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo("Medication deleted successfully");
        verify(medicationService).hardDeleteMedication(PATIENT_ID, MEDICATION_ID, CAREGIVER_ID);
    }
}
