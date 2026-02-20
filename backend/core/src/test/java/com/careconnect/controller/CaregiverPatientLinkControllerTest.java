package com.careconnect.controller;

import com.careconnect.dto.*;
import com.careconnect.model.User;
import com.careconnect.repository.UserRepository;
import com.careconnect.security.Role;
import com.careconnect.service.CaregiverPatientLinkService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CaregiverPatientLinkController.class)
@AutoConfigureMockMvc(addFilters = false)
class CaregiverPatientLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CaregiverPatientLinkService linkService;

    @MockBean
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ============================================================
    // HELPER METHODS
    // WHY: Simulates authenticated CareConnect user in RBAC tests
    // ============================================================
    private User buildUser(Long id, Role role, String email) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setEmail(email);
        return u;
    }

    private void mockSecurityContext(String email, User user) {
        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        SecurityContext secCtx = Mockito.mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }

    // ============================================================
    // TEST: Create Link - Admin Allowed
    // WHY: Verifies ADMIN bypasses caregiver ownership check
    // ============================================================
    @Test
    void adminShouldCreateLinkSuccessfully() throws Exception {

        User admin = buildUser(1L, Role.ADMIN, "admin@test.com");
        mockSecurityContext("admin@test.com", admin);

        mockMvc.perform(post("/v1/api/caregiver-patient-links/caregivers/2/patients")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
    }

    // ============================================================
    // TEST: Create Link - Caregiver Unauthorized
    // WHY: Prevent caregiver linking patients for other caregivers
    // ============================================================
    @Test
    void caregiverShouldNotCreateLinkForOtherCaregiver() throws Exception {

        User caregiver = buildUser(5L, Role.CAREGIVER, "cg@test.com");
        mockSecurityContext("cg@test.com", caregiver);

        mockMvc.perform(post("/v1/api/caregiver-patient-links/caregivers/2/patients")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // TEST: Update Link - Non Admin Forbidden
    // WHY: Only admins may modify links
    // ============================================================
    @Test
    void caregiverCannotUpdateLink() throws Exception {

        User caregiver = buildUser(2L, Role.CAREGIVER, "cg@test.com");
        mockSecurityContext("cg@test.com", caregiver);

        mockMvc.perform(put("/v1/api/caregiver-patient-links/1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // TEST: Suspend Link - Allowed for Caregiver
    // WHY: Caregivers are permitted to suspend links
    // ============================================================
    @Test
    void caregiverCanSuspendLink() throws Exception {

        User caregiver = buildUser(2L, Role.CAREGIVER, "cg@test.com");
        mockSecurityContext("cg@test.com", caregiver);

        mockMvc.perform(post("/v1/api/caregiver-patient-links/1/suspend"))
                .andExpect(status().isOk());
    }

    // ============================================================
    // TEST: Revoke Link - Non Admin Forbidden
    // WHY: Permanent deletion must be admin-only
    // ============================================================
    @Test
    void caregiverCannotRevokeLink() throws Exception {

        User caregiver = buildUser(2L, Role.CAREGIVER, "cg@test.com");
        mockSecurityContext("cg@test.com", caregiver);

        mockMvc.perform(delete("/v1/api/caregiver-patient-links/1"))
                .andExpect(status().isForbidden());
    }

    // ============================================================
    // TEST: Get Patients By Caregiver - Owner Allowed
    // WHY: Caregiver may only see own patients
    // ============================================================
    @Test
    void caregiverCanViewOwnPatients() throws Exception {

        User caregiver = buildUser(2L, Role.CAREGIVER, "cg@test.com");
        mockSecurityContext("cg@test.com", caregiver);

        when(linkService.getPatientsByCaregiver(2L))
                .thenReturn(List.of());

        mockMvc.perform(get("/v1/api/caregiver-patient-links/caregivers/2/patients"))
                .andExpect(status().isOk());
    }

    // ============================================================
    // TEST: Get All Links - Admin Only
    // WHY: Verifies system-wide link visibility is restricted
    // ============================================================
    @Test
    void adminCanViewAllLinks() throws Exception {

        User admin = buildUser(1L, Role.ADMIN, "admin@test.com");
        mockSecurityContext("admin@test.com", admin);

        when(linkService.getAllLinks()).thenReturn(List.of());

        mockMvc.perform(get("/v1/api/caregiver-patient-links"))
                .andExpect(status().isOk());
    }

    // ============================================================
    // TEST: Cleanup Expired Links - Non Admin Forbidden
    // WHY: Background maintenance must be restricted
    // ============================================================
    @Test
    void caregiverCannotCleanupLinks() throws Exception {

        User caregiver = buildUser(2L, Role.CAREGIVER, "cg@test.com");
        mockSecurityContext("cg@test.com", caregiver);

        mockMvc.perform(post("/v1/api/caregiver-patient-links/cleanup-expired"))
                .andExpect(status().isForbidden());
    }
}
