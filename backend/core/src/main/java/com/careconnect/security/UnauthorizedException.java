package com.careconnect.security;

/**
 * Exception thrown when a user attempts to perform an action 
 * they don't have permission for.
 * 
 * This exception should be caught by the GlobalExceptionHandler
 * and returned as a 403 Forbidden response.
 */
public class UnauthorizedException extends RuntimeException {
    
    private final Permission requiredPermission;
    private final Role userRole;
    
    public UnauthorizedException(String message, Permission requiredPermission, Role userRole) {
        super(message);
        this.requiredPermission = requiredPermission;
        this.userRole = userRole;
    }
    
    public Permission getRequiredPermission() {
        return requiredPermission;
    }
    
    public Role getUserRole() {
        return userRole;
    }
    
    @Override
    public String getMessage() {
        return String.format("%s. Required permission: %s, User role: %s",
            super.getMessage(),
            requiredPermission != null ? requiredPermission.name() : "UNKNOWN",
            userRole != null ? userRole.name() : "UNKNOWN"
        );
    }
}