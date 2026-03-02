package com.careconnect.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSettingTest {

    // ─── No-arg constructor ───────────────────────────────────────────────────

    @Test
    void noArgConstructor_createsInstance() {
        NotificationSetting ns = new NotificationSetting();

        assertThat(ns).isNotNull();
        assertThat(ns.getId()).isNull();
        assertThat(ns.getUserId()).isNull();
    }

    // ─── Builder defaults ─────────────────────────────────────────────────────

    @Test
    void builder_defaults_allTrue() {
        NotificationSetting ns = NotificationSetting.builder().userId(1L).build();

        assertThat(ns.isGamification()).isTrue();
        assertThat(ns.isEmergency()).isTrue();
        assertThat(ns.isVideoCall()).isTrue();
        assertThat(ns.isAudioCall()).isTrue();
        assertThat(ns.isSms()).isTrue();
        assertThat(ns.isSignificantVitals()).isTrue();
    }

    // ─── Builder all fields ───────────────────────────────────────────────────

    @Test
    void builder_allFields() {
        Instant now = Instant.now();

        NotificationSetting ns = NotificationSetting.builder()
                .id(1L)
                .userId(10L)
                .gamification(false)
                .emergency(true)
                .videoCall(false)
                .audioCall(true)
                .sms(false)
                .significantVitals(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(ns.getId()).isEqualTo(1L);
        assertThat(ns.getUserId()).isEqualTo(10L);
        assertThat(ns.isGamification()).isFalse();
        assertThat(ns.isEmergency()).isTrue();
        assertThat(ns.isVideoCall()).isFalse();
        assertThat(ns.isAudioCall()).isTrue();
        assertThat(ns.isSms()).isFalse();
        assertThat(ns.isSignificantVitals()).isTrue();
        assertThat(ns.getCreatedAt()).isEqualTo(now);
        assertThat(ns.getUpdatedAt()).isEqualTo(now);
    }

    // ─── @PrePersist: onCreate() ──────────────────────────────────────────────

    @Test
    void onCreate_setsTimestamps() throws Exception {
        NotificationSetting ns = new NotificationSetting();

        Method m = NotificationSetting.class.getDeclaredMethod("onCreate");
        m.setAccessible(true);
        m.invoke(ns);

        assertThat(ns.getCreatedAt()).isNotNull();
        assertThat(ns.getUpdatedAt()).isNotNull();
    }

    // ─── @PreUpdate: onUpdate() ───────────────────────────────────────────────

    @Test
    void onUpdate_setsUpdatedAt() throws Exception {
        NotificationSetting ns = new NotificationSetting();

        Method m = NotificationSetting.class.getDeclaredMethod("onUpdate");
        m.setAccessible(true);
        m.invoke(ns);

        assertThat(ns.getUpdatedAt()).isNotNull();
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    void setters_updateFields() {
        NotificationSetting ns = new NotificationSetting();
        Instant now = Instant.now();

        ns.setId(2L);
        ns.setUserId(20L);
        ns.setGamification(false);
        ns.setEmergency(false);
        ns.setVideoCall(false);
        ns.setAudioCall(false);
        ns.setSms(false);
        ns.setSignificantVitals(false);
        ns.setCreatedAt(now);
        ns.setUpdatedAt(now);

        assertThat(ns.getId()).isEqualTo(2L);
        assertThat(ns.getUserId()).isEqualTo(20L);
        assertThat(ns.isGamification()).isFalse();
        assertThat(ns.isEmergency()).isFalse();
        assertThat(ns.isVideoCall()).isFalse();
        assertThat(ns.isAudioCall()).isFalse();
        assertThat(ns.isSms()).isFalse();
        assertThat(ns.isSignificantVitals()).isFalse();
    }

    // ─── equals() and hashCode() ──────────────────────────────────────────────

    @Test
    void equals_sameFields_returnsTrue() {
        Instant now = Instant.now();
        NotificationSetting n1 = NotificationSetting.builder().id(1L).userId(5L).createdAt(now).updatedAt(now).build();
        NotificationSetting n2 = NotificationSetting.builder().id(1L).userId(5L).createdAt(now).updatedAt(now).build();

        assertThat(n1).isEqualTo(n2);
        assertThat(n1.hashCode()).isEqualTo(n2.hashCode());
    }
}
