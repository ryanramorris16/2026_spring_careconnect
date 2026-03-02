package com.careconnect.service;

import com.careconnect.model.CheckIn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckInServiceTest {

    private CheckInService checkInService;

    @BeforeEach
    void setUp() {
        checkInService = new CheckInService();
    }

    @Test
    @DisplayName("getAllCheckIns returns an empty list")
    void getAllCheckIns_returnsEmptyList() {
        List<CheckIn> result = checkInService.getAllCheckIns();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAllCheckIns returns an immutable List.of()")
    void getAllCheckIns_returnsImmutableList() {
        List<CheckIn> result = checkInService.getAllCheckIns();
        assertThrows(UnsupportedOperationException.class, () -> result.add(new CheckIn()));
    }

    @Test
    @DisplayName("getCheckInByID returns a non-null CheckIn instance")
    void getCheckInByID_returnsNonNullCheckIn() {
        CheckIn result = checkInService.getCheckInByID(1L);
        assertNotNull(result);
    }

    @Test
    @DisplayName("getCheckInByID returns a new CheckIn with null id")
    void getCheckInByID_returnsCheckInWithNullId() {
        CheckIn result = checkInService.getCheckInByID(1L);
        assertNull(result.getId());
    }

    @Test
    @DisplayName("getCheckInByID ignores the provided id parameter")
    void getCheckInByID_ignoresProvidedId() {
        CheckIn result1 = checkInService.getCheckInByID(42L);
        CheckIn result2 = checkInService.getCheckInByID(99L);
        assertNull(result1.getId());
        assertNull(result2.getId());
        assertNotSame(result1, result2);
    }

    @Test
    @DisplayName("getCheckInByID with null id still returns a new CheckIn")
    void getCheckInByID_withNullId_returnsCheckIn() {
        CheckIn result = checkInService.getCheckInByID(null);
        assertNotNull(result);
    }
}
