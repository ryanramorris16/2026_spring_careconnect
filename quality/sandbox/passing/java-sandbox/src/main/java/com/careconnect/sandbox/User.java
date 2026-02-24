package com.careconnect.sandbox;

import java.util.Objects;

/**
 * Immutable user domain model.
 */
public final class User {

    private final String email;
    private final String displayName;

    public User(String email, String displayName) {
        this.email = validateEmail(email);
        this.displayName = validateDisplayName(displayName);
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    private String validateEmail(String value) {
        Objects.requireNonNull(value, "email must not be null");
        if (!value.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return value;
    }

    private String validateDisplayName(String value) {
        Objects.requireNonNull(value, "displayName must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        return value;
    }
}
