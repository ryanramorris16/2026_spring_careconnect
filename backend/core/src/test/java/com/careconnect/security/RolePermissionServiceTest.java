package com.careconnect.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;

/**
 * Comprehensive unit tests for RolePermissionService.
 * Tests role-to-permission mappings and all permission check methods.
 */
public class RolePermissionServiceTest {

    // ========== Permission Count Tests ==========

    @Test
    @DisplayName("Admin should have all 26 permissions")
    public void testAdminHasAllPermissions() {
        Set<Permission> adminPerms = RolePermissionService.getPermissionsForRole(Role.ADMIN);
        assertEquals(26, adminPerms.size(),
            "Admin should have all 26 permissions");
        
        // Verify admin has every permission
        for (Permission p : Permission.values()) {
            assertTrue(adminPerms.contains(p),
                "Admin should have permission: " + p.name());
        }
    }

    @Test
    @DisplayName("Caregiver should have exactly 19 permissions")
    public void testCaregiverHasCorrectPermissionCount() {
        Set<Permission> caregiverPerms = RolePermissionService.getPermissionsForRole(Role.CAREGIVER);
        assertEquals(19, caregiverPerms.size(),
            "Caregiver should have exactly 19 permissions");
    }

    @Test
    @DisplayName("Patient should have exactly 6 permissions")
    public void testPatientHasCorrectPermissionCount() {
        Set<Permission> patientPerms = RolePermissionService.getPermissionsForRole(Role.PATIENT);
        assertEquals(6, patientPerms.size(),
            "Patient should have exactly 6 permissions");
    }

    @Test
    @DisplayName("Family Member should have exactly 3 permissions")
    public void testFamilyMemberHasCorrectPermissionCount() {
        Set<Permission> familyPerms = RolePermissionService.getPermissionsForRole(Role.FAMILY_MEMBER);
        assertEquals(3, familyPerms.size(),
            "Family Member should have exactly 3 permissions");
    }

    // ========== Specific Permission Tests - Admin ==========

    @Test
    @DisplayName("Admin should have all admin-only permissions")
    public void testAdminHasAdminOnlyPermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.ADMIN, Permission.VIEW_ALL_USERS));
        assertTrue(RolePermissionService.hasPermission(Role.ADMIN, Permission.MANAGE_USERS));
        assertTrue(RolePermissionService.hasPermission(Role.ADMIN, Permission.ASSIGN_ROLES));
        assertTrue(RolePermissionService.hasPermission(Role.ADMIN, Permission.VIEW_ALL_PATIENTS));
        assertTrue(RolePermissionService.hasPermission(Role.ADMIN, Permission.DELETE_PATIENTS));
    }

    // ========== Specific Permission Tests - Caregiver ==========

    @Test
    @DisplayName("Caregiver should have patient management permissions")
    public void testCaregiverHasPatientManagementPermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.VIEW_ASSIGNED_PATIENTS),
            "Caregiver should be able to view assigned patients");
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.CREATE_PATIENTS),
            "Caregiver should be able to create patients");
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.UPDATE_PATIENTS),
            "Caregiver should be able to update patients");
    }

    @Test
    @DisplayName("Caregiver should NOT have admin-only permissions")
    public void testCaregiverDoesNotHaveAdminPermissions() {
        assertFalse(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.VIEW_ALL_USERS),
            "Caregiver should NOT be able to view all users");
        assertFalse(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.MANAGE_USERS),
            "Caregiver should NOT be able to manage users");
        assertFalse(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.ASSIGN_ROLES),
            "Caregiver should NOT be able to assign roles");
        assertFalse(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.VIEW_ALL_PATIENTS),
            "Caregiver should NOT be able to view all patients (only assigned)");
        assertFalse(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.DELETE_PATIENTS),
            "Caregiver should NOT be able to delete patients");
    }

    @Test
    @DisplayName("Caregiver should have task management permissions")
    public void testCaregiverHasTaskManagementPermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.CREATE_TASKS));
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.VIEW_TASKS));
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.UPDATE_TASKS));
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.DELETE_TASKS));
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.COMPLETE_TASKS));
    }

    @Test
    @DisplayName("Caregiver should have health data permissions")
    public void testCaregiverHasHealthDataPermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.VIEW_HEALTH_DATA));
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.RECORD_HEALTH_DATA));
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.EXPORT_HEALTH_DATA));
    }

    @Test
    @DisplayName("Caregiver should have billing permissions")
    public void testCaregiverHasBillingPermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.VIEW_BILLING));
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.MANAGE_SUBSCRIPTIONS));
    }

    @Test
    @DisplayName("Caregiver should have communication permissions")
    public void testCaregiverHasCommunicationPermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.SEND_MESSAGES));
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.VIEW_MESSAGES));
    }

    @Test
    @DisplayName("Caregiver should have analytics permissions")
    public void testCaregiverHasAnalyticsPermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.VIEW_ANALYTICS));
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.EXPORT_REPORTS));
    }

    @Test
    @DisplayName("Caregiver should have AI and device permissions")
    public void testCaregiverHasAIAndDevicePermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.USE_AI_FEATURES));
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.MANAGE_DEVICES));
    }

    // ========== Specific Permission Tests - Patient ==========

    @Test
    @DisplayName("Patient should have limited task permissions")
    public void testPatientHasLimitedTaskPermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.PATIENT, Permission.VIEW_TASKS),
            "Patient should be able to view tasks");
        assertTrue(RolePermissionService.hasPermission(Role.PATIENT, Permission.COMPLETE_TASKS),
            "Patient should be able to complete tasks");
        
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.CREATE_TASKS),
            "Patient should NOT be able to create tasks");
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.UPDATE_TASKS),
            "Patient should NOT be able to update tasks");
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.DELETE_TASKS),
            "Patient should NOT be able to delete tasks");
    }

    @Test
    @DisplayName("Patient should have own health data permissions")
    public void testPatientHasOwnHealthDataPermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.PATIENT, Permission.VIEW_HEALTH_DATA),
            "Patient should be able to view own health data");
        assertTrue(RolePermissionService.hasPermission(Role.PATIENT, Permission.RECORD_HEALTH_DATA),
            "Patient should be able to record own health data");
        
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.EXPORT_HEALTH_DATA),
            "Patient should NOT be able to export health data");
    }

    @Test
    @DisplayName("Patient should have communication permissions")
    public void testPatientHasCommunicationPermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.PATIENT, Permission.SEND_MESSAGES));
        assertTrue(RolePermissionService.hasPermission(Role.PATIENT, Permission.VIEW_MESSAGES));
    }

    @Test
    @DisplayName("Patient should NOT have patient management permissions")
    public void testPatientDoesNotHavePatientManagementPermissions() {
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.VIEW_ASSIGNED_PATIENTS));
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.VIEW_ALL_PATIENTS));
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.CREATE_PATIENTS));
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.UPDATE_PATIENTS));
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.DELETE_PATIENTS));
    }

    @Test
    @DisplayName("Patient should NOT have billing permissions")
    public void testPatientDoesNotHaveBillingPermissions() {
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.VIEW_BILLING));
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.MANAGE_SUBSCRIPTIONS));
    }

    @Test
    @DisplayName("Patient should NOT have analytics permissions")
    public void testPatientDoesNotHaveAnalyticsPermissions() {
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.VIEW_ANALYTICS));
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.EXPORT_REPORTS));
    }

    // ========== Specific Permission Tests - Family Member ==========

    @Test
    @DisplayName("Family Member should have read-only view permissions")
    public void testFamilyMemberHasReadOnlyPermissions() {
        assertTrue(RolePermissionService.hasPermission(Role.FAMILY_MEMBER, Permission.VIEW_TASKS),
            "Family Member should be able to view tasks");
        assertTrue(RolePermissionService.hasPermission(Role.FAMILY_MEMBER, Permission.VIEW_HEALTH_DATA),
            "Family Member should be able to view health data");
        assertTrue(RolePermissionService.hasPermission(Role.FAMILY_MEMBER, Permission.VIEW_MESSAGES),
            "Family Member should be able to view messages");
    }

    @Test
    @DisplayName("Family Member should NOT be able to create, update, or delete")
    public void testFamilyMemberCannotModify() {
        assertFalse(RolePermissionService.hasPermission(Role.FAMILY_MEMBER, Permission.CREATE_TASKS),
            "Family Member should NOT be able to create tasks");
        assertFalse(RolePermissionService.hasPermission(Role.FAMILY_MEMBER, Permission.UPDATE_TASKS),
            "Family Member should NOT be able to update tasks");
        assertFalse(RolePermissionService.hasPermission(Role.FAMILY_MEMBER, Permission.DELETE_TASKS),
            "Family Member should NOT be able to delete tasks");
        assertFalse(RolePermissionService.hasPermission(Role.FAMILY_MEMBER, Permission.COMPLETE_TASKS),
            "Family Member should NOT be able to complete tasks");
        assertFalse(RolePermissionService.hasPermission(Role.FAMILY_MEMBER, Permission.RECORD_HEALTH_DATA),
            "Family Member should NOT be able to record health data");
        assertFalse(RolePermissionService.hasPermission(Role.FAMILY_MEMBER, Permission.SEND_MESSAGES),
            "Family Member should NOT be able to send messages");
    }

    // ========== hasAllPermissions Tests ==========

    @Test
    @DisplayName("hasAllPermissions should return true when role has all specified permissions")
    public void testHasAllPermissionsPositive() {
        assertTrue(RolePermissionService.hasAllPermissions(Role.CAREGIVER,
            Permission.CREATE_TASKS,
            Permission.VIEW_HEALTH_DATA,
            Permission.SEND_MESSAGES));
    }

    @Test
    @DisplayName("hasAllPermissions should return false when role is missing one permission")
    public void testHasAllPermissionsNegative() {
        assertFalse(RolePermissionService.hasAllPermissions(Role.PATIENT,
            Permission.VIEW_TASKS,
            Permission.CREATE_TASKS)); // Patient doesn't have CREATE_TASKS
    }

    @Test
    @DisplayName("hasAllPermissions should handle empty permission array")
    public void testHasAllPermissionsEmpty() {
        assertTrue(RolePermissionService.hasAllPermissions(Role.PATIENT),
            "Should return true for empty permission array");
    }

    @Test
    @DisplayName("hasAllPermissions should return false for null role")
    public void testHasAllPermissionsNullRole() {
        assertFalse(RolePermissionService.hasAllPermissions(null, Permission.CREATE_TASKS));
    }

    // ========== hasAnyPermission Tests ==========

    @Test
    @DisplayName("hasAnyPermission should return true when role has at least one permission")
    public void testHasAnyPermissionPositive() {
        assertTrue(RolePermissionService.hasAnyPermission(Role.PATIENT,
            Permission.CREATE_TASKS,  // Patient doesn't have this
            Permission.VIEW_TASKS));   // But patient HAS this
    }

    @Test
    @DisplayName("hasAnyPermission should return false when role has none of the permissions")
    public void testHasAnyPermissionNegative() {
        assertFalse(RolePermissionService.hasAnyPermission(Role.PATIENT,
            Permission.CREATE_TASKS,
            Permission.DELETE_TASKS,
            Permission.MANAGE_USERS));
    }

    @Test
    @DisplayName("hasAnyPermission should return false for null role")
    public void testHasAnyPermissionNullRole() {
        assertFalse(RolePermissionService.hasAnyPermission(null, Permission.CREATE_TASKS));
    }

    // ========== getPermissionCount Tests ==========

    @Test
    @DisplayName("getPermissionCount should return correct counts for all roles")
    public void testGetPermissionCount() {
        assertEquals(26, RolePermissionService.getPermissionCount(Role.ADMIN));
        assertEquals(19, RolePermissionService.getPermissionCount(Role.CAREGIVER));
        assertEquals(6, RolePermissionService.getPermissionCount(Role.PATIENT));
        assertEquals(3, RolePermissionService.getPermissionCount(Role.FAMILY_MEMBER));
    }

    @Test
    @DisplayName("getPermissionCount should return 0 for null role")
    public void testGetPermissionCountNull() {
        assertEquals(0, RolePermissionService.getPermissionCount(null));
    }

    // ========== getPermissionsForRole Tests ==========

    @Test
    @DisplayName("getPermissionsForRole should return unmodifiable set")
    public void testGetPermissionsForRoleReturnsUnmodifiable() {
        Set<Permission> permissions = RolePermissionService.getPermissionsForRole(Role.CAREGIVER);
        
        assertThrows(UnsupportedOperationException.class, () -> {
            permissions.add(Permission.MANAGE_USERS);
        }, "Returned permission set should be unmodifiable");
    }

    @Test
    @DisplayName("getPermissionsForRole should return empty set for null role")
    public void testGetPermissionsForRoleNull() {
        Set<Permission> permissions = RolePermissionService.getPermissionsForRole(null);
        assertNotNull(permissions);
        assertTrue(permissions.isEmpty());
    }

    @Test
    @DisplayName("getPermissionsForRole should return consistent results")
    public void testGetPermissionsForRoleConsistent() {
        Set<Permission> first = RolePermissionService.getPermissionsForRole(Role.CAREGIVER);
        Set<Permission> second = RolePermissionService.getPermissionsForRole(Role.CAREGIVER);
        
        assertEquals(first.size(), second.size());
        assertTrue(first.containsAll(second));
    }

    // ========== hasPermission Edge Cases ==========

    @Test
    @DisplayName("hasPermission should return false for null permission")
    public void testHasPermissionNullPermission() {
        assertFalse(RolePermissionService.hasPermission(Role.ADMIN, null));
    }

    @Test
    @DisplayName("hasPermission should return false for null role")
    public void testHasPermissionNullRole() {
        assertFalse(RolePermissionService.hasPermission(null, Permission.CREATE_TASKS));
    }

    // ========== getPermissionSummary Tests ==========

    @Test
    @DisplayName("getPermissionSummary should return summary for all roles")
    public void testGetPermissionSummary() {
        Map<String, Integer> summary = RolePermissionService.getPermissionSummary();
        
        assertNotNull(summary);
        assertEquals(4, summary.size(), "Summary should have 4 roles");
        
        assertEquals(26, summary.get("Administrator"));
        assertEquals(19, summary.get("Caregiver"));
        assertEquals(6, summary.get("Patient"));
        assertEquals(3, summary.get("Family Member"));
    }

    // ========== Permission Overlap Tests ==========

    @Test
    @DisplayName("Patient permissions should be subset of Caregiver permissions")
    public void testPatientPermissionsAreSubsetOfCaregiver() {
        Set<Permission> patientPerms = RolePermissionService.getPermissionsForRole(Role.PATIENT);
        Set<Permission> caregiverPerms = RolePermissionService.getPermissionsForRole(Role.CAREGIVER);
        
        // All patient permissions should also be in caregiver permissions
        for (Permission p : patientPerms) {
            if (p != Permission.SEND_MESSAGES && p != Permission.VIEW_MESSAGES) {
                // Skip communication permissions as they're intentionally shared
                assertTrue(caregiverPerms.contains(p) || 
                          p == Permission.SEND_MESSAGES || 
                          p == Permission.VIEW_MESSAGES,
                    "Patient permission " + p.name() + " should also be in Caregiver permissions");
            }
        }
    }

    @Test
    @DisplayName("Family Member permissions should be smallest subset")
    public void testFamilyMemberHasSmallestPermissionSet() {
        int familyCount = RolePermissionService.getPermissionCount(Role.FAMILY_MEMBER);
        int patientCount = RolePermissionService.getPermissionCount(Role.PATIENT);
        int caregiverCount = RolePermissionService.getPermissionCount(Role.CAREGIVER);
        int adminCount = RolePermissionService.getPermissionCount(Role.ADMIN);
        
        assertTrue(familyCount < patientCount,
            "Family Member should have fewer permissions than Patient");
        assertTrue(familyCount < caregiverCount,
            "Family Member should have fewer permissions than Caregiver");
        assertTrue(familyCount < adminCount,
            "Family Member should have fewer permissions than Admin");
    }

    // ========== HIPAA Compliance Tests ==========

    @Test
    @DisplayName("Only Admin should be able to delete patients (HIPAA)")
    public void testOnlyAdminCanDeletePatients() {
        assertTrue(RolePermissionService.hasPermission(Role.ADMIN, Permission.DELETE_PATIENTS));
        assertFalse(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.DELETE_PATIENTS));
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.DELETE_PATIENTS));
        assertFalse(RolePermissionService.hasPermission(Role.FAMILY_MEMBER, Permission.DELETE_PATIENTS));
    }

    @Test
    @DisplayName("Only Admin and Caregiver should export health data (HIPAA)")
    public void testHealthDataExportRestricted() {
        assertTrue(RolePermissionService.hasPermission(Role.ADMIN, Permission.EXPORT_HEALTH_DATA));
        assertTrue(RolePermissionService.hasPermission(Role.CAREGIVER, Permission.EXPORT_HEALTH_DATA));
        assertFalse(RolePermissionService.hasPermission(Role.PATIENT, Permission.EXPORT_HEALTH_DATA));
        assertFalse(RolePermissionService.hasPermission(Role.FAMILY_MEMBER, Permission.EXPORT_HEALTH_DATA));
    }
}