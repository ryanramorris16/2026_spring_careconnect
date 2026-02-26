package com.careconnect.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FamilyMemberLinkTest {

    // ─── Default constructor ──────────────────────────────────────────────────

    @Test
    void defaultConstructor_setsDefaults() {
        FamilyMemberLink link = new FamilyMemberLink();

        assertThat(link).isNotNull();
        assertThat(link.getStatus()).isEqualTo(FamilyMemberLink.LinkStatus.ACTIVE);
        assertThat(link.getLinkType()).isEqualTo(FamilyMemberLink.LinkType.PERMANENT);
    }

    // ─── 4-arg constructor ────────────────────────────────────────────────────

    @Test
    void fourArgConstructor_setsFields() {
        User familyUser = new User();
        User patientUser = new User();
        User grantedBy = new User();

        FamilyMemberLink link = new FamilyMemberLink(familyUser, patientUser, grantedBy, "Son");

        assertThat(link.getFamilyUser()).isSameAs(familyUser);
        assertThat(link.getPatientUser()).isSameAs(patientUser);
        assertThat(link.getGrantedBy()).isSameAs(grantedBy);
        assertThat(link.getRelationship()).isEqualTo("Son");
    }

    // ─── 5-arg constructor ────────────────────────────────────────────────────

    @Test
    void fiveArgConstructor_setsFields() {
        User familyUser = new User();
        User patientUser = new User();
        User grantedBy = new User();

        FamilyMemberLink link = new FamilyMemberLink(
                familyUser, patientUser, grantedBy, "Daughter", FamilyMemberLink.LinkType.TEMPORARY);

        assertThat(link.getRelationship()).isEqualTo("Daughter");
        assertThat(link.getLinkType()).isEqualTo(FamilyMemberLink.LinkType.TEMPORARY);
    }

    // ─── isActive() ──────────────────────────────────────────────────────────

    @Test
    void isActive_statusActive_noExpiry_returnsTrue() {
        FamilyMemberLink link = new FamilyMemberLink();
        link.setStatus(FamilyMemberLink.LinkStatus.ACTIVE);
        link.setExpiresAt(null);

        assertThat(link.isActive()).isTrue();
    }

    @Test
    void isActive_statusSuspended_returnsFalse() {
        FamilyMemberLink link = new FamilyMemberLink();
        link.setStatus(FamilyMemberLink.LinkStatus.SUSPENDED);

        assertThat(link.isActive()).isFalse();
    }

    @Test
    void isActive_statusActive_pastExpiry_returnsFalse() {
        FamilyMemberLink link = new FamilyMemberLink();
        link.setStatus(FamilyMemberLink.LinkStatus.ACTIVE);
        link.setExpiresAt(LocalDateTime.now().minusDays(1));

        assertThat(link.isActive()).isFalse();
    }

    @Test
    void isActive_statusActive_futureExpiry_returnsTrue() {
        FamilyMemberLink link = new FamilyMemberLink();
        link.setStatus(FamilyMemberLink.LinkStatus.ACTIVE);
        link.setExpiresAt(LocalDateTime.now().plusDays(5));

        assertThat(link.isActive()).isTrue();
    }

    // ─── isExpired() ─────────────────────────────────────────────────────────

    @Test
    void isExpired_nullExpiry_returnsFalse() {
        FamilyMemberLink link = new FamilyMemberLink();
        assertThat(link.isExpired()).isFalse();
    }

    @Test
    void isExpired_pastExpiry_returnsTrue() {
        FamilyMemberLink link = new FamilyMemberLink();
        link.setExpiresAt(LocalDateTime.now().minusDays(2));
        assertThat(link.isExpired()).isTrue();
    }

    @Test
    void isExpired_futureExpiry_returnsFalse() {
        FamilyMemberLink link = new FamilyMemberLink();
        link.setExpiresAt(LocalDateTime.now().plusDays(3));
        assertThat(link.isExpired()).isFalse();
    }

    // ─── setStatus() updates updatedAt ───────────────────────────────────────

    @Test
    void setStatus_updatesUpdatedAt() {
        FamilyMemberLink link = new FamilyMemberLink();
        link.setStatus(FamilyMemberLink.LinkStatus.REVOKED);

        assertThat(link.getStatus()).isEqualTo(FamilyMemberLink.LinkStatus.REVOKED);
        assertThat(link.getUpdatedAt()).isNotNull();
    }

    // ─── setPatientUser() ────────────────────────────────────────────────────

    @Test
    void setPatientUser_updatesUser() {
        FamilyMemberLink link = new FamilyMemberLink();
        User patient = new User();
        link.setPatientUser(patient);
        assertThat(link.getPatientUser()).isSameAs(patient);
    }

    // ─── @PrePersist: onCreate() ──────────────────────────────────────────────

    @Test
    void onCreate_setsCreatedAt() throws Exception {
        FamilyMemberLink link = new FamilyMemberLink();

        Method m = FamilyMemberLink.class.getDeclaredMethod("onCreate");
        m.setAccessible(true);
        m.invoke(link);

        assertThat(link.getCreatedAt()).isNotNull();
    }

    // ─── LinkStatus enum ─────────────────────────────────────────────────────

    @Test
    void linkStatusEnum_containsAllValues() {
        assertThat(FamilyMemberLink.LinkStatus.values()).containsExactly(
                FamilyMemberLink.LinkStatus.ACTIVE,
                FamilyMemberLink.LinkStatus.SUSPENDED,
                FamilyMemberLink.LinkStatus.REVOKED,
                FamilyMemberLink.LinkStatus.EXPIRED
        );
    }

    // ─── LinkType enum ────────────────────────────────────────────────────────

    @Test
    void linkTypeEnum_containsAllValues() {
        assertThat(FamilyMemberLink.LinkType.values()).containsExactly(
                FamilyMemberLink.LinkType.PERMANENT,
                FamilyMemberLink.LinkType.TEMPORARY,
                FamilyMemberLink.LinkType.EMERGENCY
        );
    }

    // ─── Remaining setters ────────────────────────────────────────────────────

    @Test
    void remainingSetters_updateFields() {
        FamilyMemberLink link = new FamilyMemberLink();
        User familyUser = new User();
        User grantedBy = new User();
        LocalDateTime now = LocalDateTime.now();

        link.setId(1L);
        link.setFamilyUser(familyUser);
        link.setGrantedBy(grantedBy);
        link.setPatientId(10L);
        link.setCreatedAt(now);
        link.setUpdatedAt(now);
        link.setLinkType(FamilyMemberLink.LinkType.EMERGENCY);
        link.setExpiresAt(now.plusDays(1));
        link.setNotes("Emergency access");
        link.setRelationship("Spouse");

        assertThat(link.getId()).isEqualTo(1L);
        assertThat(link.getFamilyUser()).isSameAs(familyUser);
        assertThat(link.getGrantedBy()).isSameAs(grantedBy);
        assertThat(link.getPatientId()).isEqualTo(10L);
        assertThat(link.getLinkType()).isEqualTo(FamilyMemberLink.LinkType.EMERGENCY);
        assertThat(link.getNotes()).isEqualTo("Emergency access");
        assertThat(link.getRelationship()).isEqualTo("Spouse");
    }
}
