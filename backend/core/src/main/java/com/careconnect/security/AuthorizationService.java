package com.careconnect.security;

import com.careconnect.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for checking user permissions and authorization.
 * This service is used throughout the application to enforce access control.
 */
@Service
public class AuthorizationService {
    
    @Autowired
    private RolePermissionService rolePermissionService;
    
    /**
     * Check if a user has a specific permission.
     * Returns true if the user has the permission, false otherwise.
     */
    public boolean hasPermission(User user, Permission permission) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return rolePermissionService.hasPermission(user.getRole(), permission);
    }
    
    /**
     * Require that a user has a specific permission.
     * Throws UnauthorizedException if the user lacks the permission.
     */
    public void requirePermission(User user, Permission permission) {
        if (!hasPermission(user, permission)) {
            throw new UnauthorizedException(
                "Access denied: Missing required permission",
                permission,
                user != null ? user.getRole() : null
            );
        }
    }
    
    /**
     * Check if a user has any of the specified permissions.
     */
    public boolean hasAnyPermission(User user, Permission... permissions) {
        if (user == null || permissions == null) {
            return false;
        }
        for (Permission permission : permissions) {
            if (hasPermission(user, permission)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a user has all of the specified permissions.
     */
    public boolean hasAllPermissions(User user, Permission... permissions) {
        if (user == null || permissions == null) {
            return false;
        }
        for (Permission permission : permissions) {
            if (!hasPermission(user, permission)) {
                return false;
            }
        }
        return true;
    }
}