package com.careconnect.security;

/**
 * Enumeration of all permissions in the CareConnect system.
 * These permissions control access to specific features and actions.
 */
public enum Permission {
    // User Management (Admin only)
    VIEW_ALL_USERS("View all users in the system"),
    MANAGE_USERS("Create, update, and delete users"),
    ASSIGN_ROLES("Assign and modify user roles"),
    
    // Patient Management
    VIEW_ASSIGNED_PATIENTS("View patients assigned to the user"),
    CREATE_PATIENTS("Create new patient records"),
    UPDATE_PATIENTS("Update patient information"),
    DELETE_PATIENTS("Delete patient records (Admin only)"),
    
    // Health Data
    VIEW_HEALTH_DATA("View patient health data"),
    RECORD_HEALTH_DATA("Record and update health data"),
    EXPORT_HEALTH_DATA("Export health data reports"),
    
    // Tasks
    VIEW_TASKS("View tasks"),
    CREATE_TASKS("Create new tasks"),
    UPDATE_TASKS("Update existing tasks"),
    DELETE_TASKS("Delete tasks"),
    COMPLETE_TASKS("Mark tasks as complete"),
    
    // Medications
    VIEW_MEDICATIONS("View medication lists"),
    MANAGE_MEDICATIONS("Add, update, or remove medications"),
    
    // Analytics & Reports
    VIEW_ANALYTICS("View analytics and dashboards"),
    EXPORT_REPORTS("Export reports and data"),
    
    // Messaging
    VIEW_MESSAGES("View messages"),
    SEND_MESSAGES("Send messages to other users"),
    
    // Billing (Admin only)
    VIEW_BILLING("View billing information"),
    MANAGE_SUBSCRIPTIONS("Manage subscription plans"),
    
    // System Configuration (Admin only)
    MANAGE_SYSTEM_SETTINGS("Configure system settings"),
    VIEW_AUDIT_LOGS("View system audit logs");
    
    private final String description;
    
    Permission(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}