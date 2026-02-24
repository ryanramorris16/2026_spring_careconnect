package com.careconnect.sandbox;

import java.util.UUID;

/**
 * Service layer for user management.
 */
public final class UserService {

    public User createUser(String email, String displayName) {
        User user = new User(email, displayName);
        logUserCreation(user);
        return user;
    }

    private void logUserCreation(User user) {
        String message = String.format(
            "User created [id=%s, email=%s]",
            UUID.randomUUID(),
            user.getEmail()
        );
        System.out.println(message);
    }
}
