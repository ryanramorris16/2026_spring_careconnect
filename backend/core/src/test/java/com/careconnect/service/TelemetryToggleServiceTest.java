package com.careconnect.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryToggleServiceTest {

    @Test
    @DisplayName("constructor sets enabled to true when defaultEnabled is true")
    void constructor_setsEnabledTrue() {
        TelemetryToggleService service = new TelemetryToggleService(true);

        assertTrue(service.isEnabled());
    }

    @Test
    @DisplayName("constructor sets enabled to false when defaultEnabled is false")
    void constructor_setsEnabledFalse() {
        TelemetryToggleService service = new TelemetryToggleService(false);

        assertFalse(service.isEnabled());
    }

    @Test
    @DisplayName("setEnabled updates state from true to false")
    void setEnabled_updatesStateToFalse() {
        TelemetryToggleService service = new TelemetryToggleService(true);

        boolean result = service.setEnabled(false);

        assertFalse(result);
        assertFalse(service.isEnabled());
    }

    @Test
    @DisplayName("setEnabled updates state from false to true")
    void setEnabled_updatesStateToTrue() {
        TelemetryToggleService service = new TelemetryToggleService(false);

        boolean result = service.setEnabled(true);

        assertTrue(result);
        assertTrue(service.isEnabled());
    }

    @Test
    @DisplayName("setEnabled can be called repeatedly")
    void setEnabled_canBeCalledRepeatedly() {
        TelemetryToggleService service = new TelemetryToggleService(true);

        assertFalse(service.setEnabled(false));
        assertTrue(service.setEnabled(true));
        assertFalse(service.setEnabled(false));
        assertFalse(service.isEnabled());
    }
}