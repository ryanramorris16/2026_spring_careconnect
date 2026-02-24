package com.careconnect.sandbox;

import java.time.LocalDateTime;

/**
 * Entry point for CareConnect sandbox backend.
 */
public final class Application {

    private Application() {
        // Prevent instantiation
    }

    public static void main(String[] args) {
        UserService userService = new UserService();
        User user = userService.createUser("james@example.com", "James");

        System.out.println("User created: " + user.getDisplayName());
        System.out.println("Timestamp: " + LocalDateTime.now());
    }
}