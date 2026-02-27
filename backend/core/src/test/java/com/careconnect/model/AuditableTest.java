package com.careconnect.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditableTest {

    // Concrete subclass for testing the abstract Auditable
    static class ConcreteAuditable extends Auditable {
    }

    // ─── onCreate() via @PrePersist ───────────────────────────────────────────

    @Test
    void onCreate_setsCreatedAtAndUpdatedAt() throws Exception {
        ConcreteAuditable entity = new ConcreteAuditable();

        Method m = Auditable.class.getDeclaredMethod("onCreate");
        m.setAccessible(true);
        m.invoke(entity);

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    // ─── onUpdate() via @PreUpdate ────────────────────────────────────────────

    @Test
    void onUpdate_setsUpdatedAt() throws Exception {
        ConcreteAuditable entity = new ConcreteAuditable();

        Method m = Auditable.class.getDeclaredMethod("onUpdate");
        m.setAccessible(true);
        m.invoke(entity);

        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    void setters_updateFields() {
        ConcreteAuditable entity = new ConcreteAuditable();
        LocalDateTime created = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime updated = LocalDateTime.of(2025, 6, 1, 12, 0);

        entity.setCreatedAt(created);
        entity.setUpdatedAt(updated);

        assertThat(entity.getCreatedAt()).isEqualTo(created);
        assertThat(entity.getUpdatedAt()).isEqualTo(updated);
    }

    // ─── Default state ────────────────────────────────────────────────────────

    @Test
    void defaultConstructor_fieldsAreNull() {
        ConcreteAuditable entity = new ConcreteAuditable();

        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }
}
