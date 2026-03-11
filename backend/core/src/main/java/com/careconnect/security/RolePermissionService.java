package com.careconnect.security;

import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service that manages the mapping between roles and permissions.
 * This is the single source of truth for what each role can do.
 */
@Service
public class RolePermissionService {
    
    private final Map<Role, Set<Permission>> rolePermissions;
    
    public RolePermissionService() {
        rolePermissions = new HashMap<>();
        initializePermissions();
    }
    
    private void initializePermissions() {
        // ADMIN: Full access to everything (26 permissions)
        rolePermissions.put(Role.ADMIN, EnumSet.allOf(Permission.class));
        
        // CAREGIVER: Patient care and management (19 permissions)
        rolePermissions.put(Role.CAREGIVER, EnumSet.of(
            // Patient Management
            Permission.VIEW_ASSIGNED_PATIENTS,
            Permission.CREATE_PATIENTS,
            Permission.UPDATE_PATIENTS,
            
            // Health Data
            Permission.VIEW_HEALTH_DATA,
            Permission.RECORD_HEALTH_DATA,
            Permission.EXPORT_HEALTH_DATA,
            
            // Tasks
            Permission.VIEW_TASKS,
            Permission.CREATE_TASKS,
            Permission.UPDATE_TASKS,
            Permission.DELETE_TASKS,
            Permission.COMPLETE_TASKS,
            
            // Medications
            Permission.VIEW_MEDICATIONS,
            Permission.MANAGE_MEDICATIONS,
            
            // Analytics & Reports
            Permission.VIEW_ANALYTICS,
            Permission.EXPORT_REPORTS,
            
            // Messaging
            Permission.VIEW_MESSAGES,
            Permission.SEND_MESSAGES,
            
            // Billing (view only)
            Permission.VIEW_BILLING,
            Permission.MANAGE_SUBSCRIPTIONS
        ));
        
        // PATIENT: Self-service and view own data (6 permissions)
        rolePermissions.put(Role.PATIENT, EnumSet.of(
            Permission.VIEW_ASSIGNED_PATIENTS,  // View own profile
            Permission.VIEW_HEALTH_DATA,        // View own health data
            Permission.RECORD_HEALTH_DATA,      // Record own health data (mood, pain logs)
            Permission.VIEW_TASKS,              // View own tasks
            Permission.COMPLETE_TASKS,          // Complete own tasks
            Permission.VIEW_MESSAGES            // View messages
        ));
        
        // FAMILY_MEMBER: Read-only access to assigned patient (3 permissions)
        rolePermissions.put(Role.FAMILY_MEMBER, EnumSet.of(
            Permission.VIEW_ASSIGNED_PATIENTS,
            Permission.VIEW_HEALTH_DATA,
            Permission.VIEW_TASKS
        ));
    }
    
    /**
     * Get all permissions for a given role
     */
    public Set<Permission> getPermissionsForRole(Role role) {
        return rolePermissions.getOrDefault(role, EnumSet.noneOf(Permission.class));
    }
    
    /**
     * Check if a role has a specific permission
     */
    public boolean hasPermission(Role role, Permission permission) {
        Set<Permission> permissions = rolePermissions.get(role);
        return permissions != null && permissions.contains(permission);
    }
}