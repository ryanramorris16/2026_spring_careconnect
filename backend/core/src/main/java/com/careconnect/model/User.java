package com.careconnect.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.sql.Timestamp;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    // Split name into first and last name for better usability

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;

    @Builder.Default
    @Column(name = "login_streak")
    private Integer loginStreak = 0;

    @Builder.Default
    @Column(name = "leaderboard_opt_in", nullable = true)
    private Boolean leaderboardOptIn = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.careconnect.security.Role role;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private Boolean isVerified = false;

    private String verificationToken;
    
    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    // Billing address fields (geocoded + standardized)
    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "state", length = 2)
    private String state; // 2-letter state code (e.g., "CA", "NY")

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country", length = 2)
    private String country; // 2-letter country code (default "US")

    @Column(name = "address_place_id")
    private String addressPlaceId; // Google Places place_id or equivalent provider ID

    @Column(name = "address_formatted")
    private String addressFormatted; // Full formatted address string from provider

    @Column(name = "address_latitude")
    private Double addressLatitude;

    @Column(name = "address_longitude")
    private Double addressLongitude;

    private Timestamp createdAt;

    private Timestamp lastLogin;

    private String profileImageUrl;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    // Explicit getter and setter methods for password fields
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    // Additional getters for compatibility
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public com.careconnect.security.Role getRole() { return role; }
    public Boolean getIsVerified() { return isVerified; }
    public String getVerificationToken() { return verificationToken; }
    public String getStatus() { return status; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public LocalDate getLastLoginDate() {
        return lastLoginDate;
    }
    public Integer getLoginStreak() {
        return loginStreak;
    }
    public Boolean getLeaderboardOptIn() {
        return leaderboardOptIn;
    }

    // Additional setters for compatibility
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(com.careconnect.security.Role role) { this.role = role; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public void setStatus(String status) { this.status = status; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
    public void setLastLoginDate(LocalDate lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }
    public void setLoginStreak(Integer loginStreak) {
        this.loginStreak = loginStreak;
    }
    public void setLeaderboardOptIn(Boolean leaderboardOptIn) {
        this.leaderboardOptIn = leaderboardOptIn;
    }

    // Address getters
    public String getAddressLine1() { return addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getCountry() { return country; }
    public String getAddressPlaceId() { return addressPlaceId; }
    public String getAddressFormatted() { return addressFormatted; }
    public Double getAddressLatitude() { return addressLatitude; }
    public Double getAddressLongitude() { return addressLongitude; }

    // Address setters
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setCountry(String country) { this.country = country; }
    public void setAddressPlaceId(String addressPlaceId) { this.addressPlaceId = addressPlaceId; }
    public void setAddressFormatted(String addressFormatted) { this.addressFormatted = addressFormatted; }
    public void setAddressLatitude(Double addressLatitude) { this.addressLatitude = addressLatitude; }
    public void setAddressLongitude(Double addressLongitude) { this.addressLongitude = addressLongitude; }
}
