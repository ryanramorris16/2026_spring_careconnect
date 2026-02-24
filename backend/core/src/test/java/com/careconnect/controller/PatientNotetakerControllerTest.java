package com.careconnect.controller;

import com.careconnect.dto.PatientNoteDTO;
import com.careconnect.dto.PatientNotetakerConfigDTO;
import com.careconnect.exception.AppException;
import com.careconnect.service.PatientNotetakerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientNotetakerControllerTest {

    @Mock
    private PatientNotetakerService patientNotetakerService;

    @InjectMocks
    private PatientNotetakerController controller;

    private static final Long PATIENT_ID = 1L;
    private static final Long NOTE_ID    = 10L;

    private PatientNotetakerConfigDTO configDTO() {
        PatientNotetakerConfigDTO dto = new PatientNotetakerConfigDTO();
        dto.setPatientId(PATIENT_ID);
        dto.setIsEnabled(true);
        return dto;
    }

    private PatientNoteDTO noteDTO() {
        PatientNoteDTO dto = new PatientNoteDTO();
        dto.setPatientId(PATIENT_ID);
        dto.setNote("Test note content");
        return dto;
    }

    // ─── getPatientNoteTakerConfig ────────────────────────────────────────────

    @Test
    void getPatientNoteTakerConfig_success_returnsOkWithConfig() {
        PatientNotetakerConfigDTO config = configDTO();
        when(patientNotetakerService.getNotetakerConfigByPatientId(PATIENT_ID)).thenReturn(config);

        ResponseEntity<PatientNotetakerConfigDTO> response = controller.getPatientNoteTakerConfig(PATIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(config);
        verify(patientNotetakerService).getNotetakerConfigByPatientId(PATIENT_ID);
    }

    @Test
    void getPatientNoteTakerConfig_appException_returnsInternalServerError() {
        when(patientNotetakerService.getNotetakerConfigByPatientId(PATIENT_ID))
                .thenThrow(new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));

        ResponseEntity<PatientNotetakerConfigDTO> response = controller.getPatientNoteTakerConfig(PATIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }

    // ─── updatePatientNoteTakerConfig ─────────────────────────────────────────

    @Test
    void updatePatientNoteTakerConfig_success_returnsOkWithUpdatedConfig() {
        PatientNotetakerConfigDTO input   = configDTO();
        PatientNotetakerConfigDTO updated = configDTO();
        when(patientNotetakerService.createOrUpdatePatientNotetakerConfig(PATIENT_ID, input)).thenReturn(updated);

        ResponseEntity<PatientNotetakerConfigDTO> response = controller.updatePatientNoteTakerConfig(PATIENT_ID, input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
        verify(patientNotetakerService).createOrUpdatePatientNotetakerConfig(PATIENT_ID, input);
    }

    @Test
    void updatePatientNoteTakerConfig_appException_returnsInternalServerError() {
        PatientNotetakerConfigDTO input = configDTO();
        when(patientNotetakerService.createOrUpdatePatientNotetakerConfig(PATIENT_ID, input))
                .thenThrow(new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));

        ResponseEntity<PatientNotetakerConfigDTO> response = controller.updatePatientNoteTakerConfig(PATIENT_ID, input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }

    // ─── createPatientNote ────────────────────────────────────────────────────

    @Test
    void createPatientNote_success_returnsCreatedWithNote() {
        PatientNoteDTO input   = noteDTO();
        PatientNoteDTO created = noteDTO();
        when(patientNotetakerService.createNoteForPatient(PATIENT_ID, input)).thenReturn(created);

        ResponseEntity<PatientNoteDTO> response = controller.createPatientNote(PATIENT_ID, input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(created);
        verify(patientNotetakerService).createNoteForPatient(PATIENT_ID, input);
    }

    @Test
    void createPatientNote_appException_returnsInternalServerError() {
        PatientNoteDTO input = noteDTO();
        when(patientNotetakerService.createNoteForPatient(PATIENT_ID, input))
                .thenThrow(new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));

        ResponseEntity<PatientNoteDTO> response = controller.createPatientNote(PATIENT_ID, input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }

    // ─── updatePatientNote ────────────────────────────────────────────────────

    @Test
    void updatePatientNote_success_returnsOkWithUpdatedNote() {
        PatientNoteDTO input   = noteDTO();
        PatientNoteDTO updated = noteDTO();
        when(patientNotetakerService.updateNoteForPatient(PATIENT_ID, NOTE_ID, input)).thenReturn(updated);

        ResponseEntity<PatientNoteDTO> response = controller.updatePatientNote(PATIENT_ID, NOTE_ID, input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
        verify(patientNotetakerService).updateNoteForPatient(PATIENT_ID, NOTE_ID, input);
    }

    @Test
    void updatePatientNote_appException_returnsInternalServerError() {
        PatientNoteDTO input = noteDTO();
        when(patientNotetakerService.updateNoteForPatient(PATIENT_ID, NOTE_ID, input))
                .thenThrow(new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));

        ResponseEntity<PatientNoteDTO> response = controller.updatePatientNote(PATIENT_ID, NOTE_ID, input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }

    // ─── getPatientNote ───────────────────────────────────────────────────────

    @Test
    void getPatientNote_success_returnsOkWithNote() {
        PatientNoteDTO note = noteDTO();
        when(patientNotetakerService.getNoteById(PATIENT_ID, NOTE_ID)).thenReturn(note);

        ResponseEntity<PatientNoteDTO> response = controller.getPatientNote(PATIENT_ID, NOTE_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(note);
        verify(patientNotetakerService).getNoteById(PATIENT_ID, NOTE_ID);
    }

    @Test
    void getPatientNote_appException_returnsInternalServerError() {
        when(patientNotetakerService.getNoteById(PATIENT_ID, NOTE_ID))
                .thenThrow(new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));

        ResponseEntity<PatientNoteDTO> response = controller.getPatientNote(PATIENT_ID, NOTE_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }

    // ─── getAllNotesForPatient ─────────────────────────────────────────────────

    @Test
    void getAllNotesForPatient_success_returnsOkWithNoteList() {
        List<PatientNoteDTO> notes = List.of(noteDTO(), noteDTO());
        when(patientNotetakerService.getAllNotesForPatient(PATIENT_ID)).thenReturn(notes);

        ResponseEntity<List<PatientNoteDTO>> response = controller.getAllNotesForPatient(PATIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(notes);
        verify(patientNotetakerService).getAllNotesForPatient(PATIENT_ID);
    }

    @Test
    void getAllNotesForPatient_emptyList_returnsOkWithEmptyBody() {
        when(patientNotetakerService.getAllNotesForPatient(PATIENT_ID)).thenReturn(List.of());

        ResponseEntity<List<PatientNoteDTO>> response = controller.getAllNotesForPatient(PATIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getAllNotesForPatient_appException_returnsInternalServerError() {
        when(patientNotetakerService.getAllNotesForPatient(PATIENT_ID))
                .thenThrow(new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "error"));

        ResponseEntity<List<PatientNoteDTO>> response = controller.getAllNotesForPatient(PATIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }

    // ─── deletePatientNote ────────────────────────────────────────────────────

    @Test
    void deletePatientNote_success_returnsNoContent() {
        doNothing().when(patientNotetakerService).deleteNoteById(NOTE_ID);

        ResponseEntity<Void> response = controller.deletePatientNote(PATIENT_ID, NOTE_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(patientNotetakerService).deleteNoteById(NOTE_ID);
    }

    @Test
    void deletePatientNote_appException_returnsInternalServerError() {
        doThrow(new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "error"))
                .when(patientNotetakerService).deleteNoteById(NOTE_ID);

        ResponseEntity<Void> response = controller.deletePatientNote(PATIENT_ID, NOTE_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
