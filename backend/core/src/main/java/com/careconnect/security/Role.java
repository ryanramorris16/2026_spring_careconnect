package com.careconnect.security;

import java.util.Set;

public enum Role {

    PATIENT(Set.of(
            Permission.VIEW_ASSIGNED_PATIENTS,
            Permission.VIEW_HEALTH_DATA,
            Permission.VIEW_TASKS,
            Permission.COMPLETE_TASKS,
            Permission.VIEW_MEDICATIONS
    )),

    CAREGIVER(Set.of(
            Permission.VIEW_ASSIGNED_PATIENTS,
            Permission.CREATE_PATIENTS,
            Permission.UPDATE_PATIENTS,
            Permission.VIEW_HEALTH_DATA,
            Permission.RECORD_HEALTH_DATA,
            Permission.VIEW_TASKS,
            Permission.CREATE_TASKS,
            Permission.UPDATE_TASKS,
            Permission.DELETE_TASKS,
            Permission.VIEW_MEDICATIONS,
            Permission.MANAGE_MEDICATIONS,
            Permission.SEND_MESSAGES
    )),

    ADMIN(Set.of(Permission.values())); // Admin gets everything

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}