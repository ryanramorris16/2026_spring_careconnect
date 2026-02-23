package com.careconnect.controller;

import com.careconnect.dto.evv.EvvLocationRequest;
import com.careconnect.dto.evv.EvvLocationResponse;
import com.careconnect.model.evv.EvvLocationRole;
import com.careconnect.model.evv.EvvLocationType;
import com.careconnect.service.evv.EvvLocationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvvLocationControllerTest {

    @Mock
    private EvvLocationService locationService;

    @InjectMocks
    private EvvLocationController controller;

    // ── shared constants ──────────────────────────────────────────────────────

    private static final Long EVV_RECORD_ID = 10L;

    // ── shared helpers ────────────────────────────────────────────────────────

    /** PATIENT_ADDRESS type — validate() passes without coords. */
    private EvvLocationRequest patientAddressRequest() {
        return EvvLocationRequest.builder()
                .evvRecordId(EVV_RECORD_ID)
                .role(EvvLocationRole.CHECK_IN)
                .type(EvvLocationType.PATIENT_ADDRESS)
                .build();
    }

    /** GPS type with fully-populated coords — validate() passes. */
    private EvvLocationRequest gpsRequestWithValidCoords() {
        EvvLocationRequest.CoordinatesDto coords = EvvLocationRequest.CoordinatesDto.builder()
                .lat(new BigDecimal("38.9072"))
                .lng(new BigDecimal("-77.0369"))
                .accuracyM(new BigDecimal("5.0"))
                .build();
        return EvvLocationRequest.builder()
                .evvRecordId(EVV_RECORD_ID)
                .role(EvvLocationRole.CHECK_OUT)
                .type(EvvLocationType.GPS)
                .coords(coords)
                .build();
    }

    // ── POST /v1/api/evv/locations ────────────────────────────────────────────

    @Nested
    class SaveLocation {

        // ── happy path: PATIENT_ADDRESS (no coords required) ─────────────────

        @Test
        void returns200_whenPatientAddressType() {
            EvvLocationRequest request = patientAddressRequest();
            when(locationService.saveLocation(request)).thenReturn(new EvvLocationResponse());

            ResponseEntity<EvvLocationResponse> result = controller.saveLocation(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void returnsServiceResult_whenPatientAddressType() {
            EvvLocationRequest request = patientAddressRequest();
            EvvLocationResponse locationResponse = EvvLocationResponse.builder()
                    .evvRecordId(EVV_RECORD_ID)
                    .role(EvvLocationRole.CHECK_IN)
                    .build();
            when(locationService.saveLocation(request)).thenReturn(locationResponse);

            ResponseEntity<EvvLocationResponse> result = controller.saveLocation(request);

            assertThat(result.getBody()).isSameAs(locationResponse);
        }

        @Test
        void callsServiceWithRequest_whenPatientAddressType() {
            EvvLocationRequest request = patientAddressRequest();
            when(locationService.saveLocation(request)).thenReturn(new EvvLocationResponse());

            controller.saveLocation(request);

            verify(locationService).saveLocation(request);
        }

        // ── happy path: GPS with valid coords ────────────────────────────────

        @Test
        void returns200_whenGpsTypeWithValidCoords() {
            EvvLocationRequest request = gpsRequestWithValidCoords();
            when(locationService.saveLocation(request)).thenReturn(new EvvLocationResponse());

            ResponseEntity<EvvLocationResponse> result = controller.saveLocation(request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void callsServiceWithRequest_whenGpsTypeWithValidCoords() {
            EvvLocationRequest request = gpsRequestWithValidCoords();
            when(locationService.saveLocation(request)).thenReturn(new EvvLocationResponse());

            controller.saveLocation(request);

            verify(locationService).saveLocation(request);
        }

        // ── validation failures: GPS type without sufficient coords ───────────
        // validate() is called directly on the request before the service;
        // there is no try-catch in the controller, so the exception propagates.

        @Test
        void throwsIllegalArgumentException_whenGpsTypeAndCoordsIsNull() {
            EvvLocationRequest request = EvvLocationRequest.builder()
                    .evvRecordId(EVV_RECORD_ID)
                    .role(EvvLocationRole.CHECK_IN)
                    .type(EvvLocationType.GPS)
                    .coords(null)
                    .build();

            assertThatThrownBy(() -> controller.saveLocation(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("GPS location requires coordinates");
        }

        @Test
        void throwsIllegalArgumentException_whenGpsTypeAndLatIsNull() {
            EvvLocationRequest.CoordinatesDto noLat = EvvLocationRequest.CoordinatesDto.builder()
                    .lat(null)
                    .lng(new BigDecimal("-77.0369"))
                    .build();
            EvvLocationRequest request = EvvLocationRequest.builder()
                    .evvRecordId(EVV_RECORD_ID)
                    .role(EvvLocationRole.CHECK_IN)
                    .type(EvvLocationType.GPS)
                    .coords(noLat)
                    .build();

            assertThatThrownBy(() -> controller.saveLocation(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("GPS location requires coordinates");
        }

        @Test
        void throwsIllegalArgumentException_whenGpsTypeAndLngIsNull() {
            EvvLocationRequest.CoordinatesDto noLng = EvvLocationRequest.CoordinatesDto.builder()
                    .lat(new BigDecimal("38.9072"))
                    .lng(null)
                    .build();
            EvvLocationRequest request = EvvLocationRequest.builder()
                    .evvRecordId(EVV_RECORD_ID)
                    .role(EvvLocationRole.CHECK_IN)
                    .type(EvvLocationType.GPS)
                    .coords(noLng)
                    .build();

            assertThatThrownBy(() -> controller.saveLocation(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("GPS location requires coordinates");
        }

        @Test
        void doesNotCallService_whenValidationFails() {
            EvvLocationRequest request = EvvLocationRequest.builder()
                    .evvRecordId(EVV_RECORD_ID)
                    .role(EvvLocationRole.CHECK_IN)
                    .type(EvvLocationType.GPS)
                    .coords(null)
                    .build();

            try { controller.saveLocation(request); } catch (IllegalArgumentException ignored) {}

            verifyNoInteractions(locationService);
        }
    }

    // ── GET /v1/api/evv/locations/records/{evvRecordId} ───────────────────────

    @Nested
    class GetLocationsForRecord {

        @Test
        void returns200() {
            when(locationService.getLocationsForRecord(EVV_RECORD_ID)).thenReturn(List.of());

            ResponseEntity<List<EvvLocationResponse>> response =
                    controller.getLocationsForRecord(EVV_RECORD_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void returnsListFromService() {
            EvvLocationResponse loc = new EvvLocationResponse();
            when(locationService.getLocationsForRecord(EVV_RECORD_ID)).thenReturn(List.of(loc));

            ResponseEntity<List<EvvLocationResponse>> response =
                    controller.getLocationsForRecord(EVV_RECORD_ID);

            assertThat(response.getBody()).containsExactly(loc);
        }

        @Test
        void returnsEmptyList_whenNoLocationsExist() {
            when(locationService.getLocationsForRecord(EVV_RECORD_ID)).thenReturn(List.of());

            ResponseEntity<List<EvvLocationResponse>> response =
                    controller.getLocationsForRecord(EVV_RECORD_ID);

            assertThat(response.getBody()).isEmpty();
        }

        @Test
        void callsServiceWithEvvRecordId() {
            when(locationService.getLocationsForRecord(EVV_RECORD_ID)).thenReturn(List.of());

            controller.getLocationsForRecord(EVV_RECORD_ID);

            verify(locationService).getLocationsForRecord(EVV_RECORD_ID);
        }
    }

    // ── GET /v1/api/evv/locations/records/{evvRecordId}/{role} ────────────────

    @Nested
    class GetLocationByRole {

        @Test
        void returns200_forCheckIn() {
            when(locationService.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_IN))
                    .thenReturn(new EvvLocationResponse());

            ResponseEntity<EvvLocationResponse> response =
                    controller.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_IN);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void returns200_forCheckOut() {
            when(locationService.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_OUT))
                    .thenReturn(new EvvLocationResponse());

            ResponseEntity<EvvLocationResponse> response =
                    controller.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_OUT);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void returnsServiceResult_forCheckIn() {
            EvvLocationResponse locationResponse = EvvLocationResponse.builder()
                    .evvRecordId(EVV_RECORD_ID)
                    .role(EvvLocationRole.CHECK_IN)
                    .build();
            when(locationService.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_IN))
                    .thenReturn(locationResponse);

            ResponseEntity<EvvLocationResponse> response =
                    controller.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_IN);

            assertThat(response.getBody()).isSameAs(locationResponse);
        }

        @Test
        void returnsServiceResult_forCheckOut() {
            EvvLocationResponse locationResponse = EvvLocationResponse.builder()
                    .evvRecordId(EVV_RECORD_ID)
                    .role(EvvLocationRole.CHECK_OUT)
                    .build();
            when(locationService.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_OUT))
                    .thenReturn(locationResponse);

            ResponseEntity<EvvLocationResponse> response =
                    controller.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_OUT);

            assertThat(response.getBody()).isSameAs(locationResponse);
        }

        @Test
        void callsServiceWithCorrectArguments_forCheckIn() {
            when(locationService.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_IN))
                    .thenReturn(new EvvLocationResponse());

            controller.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_IN);

            verify(locationService).getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_IN);
        }

        @Test
        void callsServiceWithCorrectArguments_forCheckOut() {
            when(locationService.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_OUT))
                    .thenReturn(new EvvLocationResponse());

            controller.getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_OUT);

            verify(locationService).getLocationByRole(EVV_RECORD_ID, EvvLocationRole.CHECK_OUT);
        }
    }

    // ── DELETE /v1/api/evv/locations/records/{evvRecordId}/{role} ─────────────

    @Nested
    class DeleteLocation {

        @Test
        void returns204NoContent_forCheckIn() {
            ResponseEntity<Void> response =
                    controller.deleteLocation(EVV_RECORD_ID, EvvLocationRole.CHECK_IN);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        void returns204NoContent_forCheckOut() {
            ResponseEntity<Void> response =
                    controller.deleteLocation(EVV_RECORD_ID, EvvLocationRole.CHECK_OUT);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        void bodyIsNull() {
            ResponseEntity<Void> response =
                    controller.deleteLocation(EVV_RECORD_ID, EvvLocationRole.CHECK_IN);

            assertThat(response.getBody()).isNull();
        }

        @Test
        void callsServiceWithCorrectArguments_forCheckIn() {
            controller.deleteLocation(EVV_RECORD_ID, EvvLocationRole.CHECK_IN);

            verify(locationService).deleteLocation(EVV_RECORD_ID, EvvLocationRole.CHECK_IN);
        }

        @Test
        void callsServiceWithCorrectArguments_forCheckOut() {
            controller.deleteLocation(EVV_RECORD_ID, EvvLocationRole.CHECK_OUT);

            verify(locationService).deleteLocation(EVV_RECORD_ID, EvvLocationRole.CHECK_OUT);
        }

        @Test
        void callsServiceExactlyOnce() {
            controller.deleteLocation(EVV_RECORD_ID, EvvLocationRole.CHECK_IN);

            verify(locationService, times(1)).deleteLocation(any(), any());
        }
    }
}
