package com.careconnect.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class USPSDigestTest {

    @Test
    void constructor_setsAllFields() throws Exception {
        OffsetDateTime digestDate = OffsetDateTime.of(2025, 1, 15, 8, 0, 0, 0, ZoneOffset.UTC);
        List<MailPiece> mailpieces = List.of(new MailPiece());
        List<PackageItem> packages = List.of(new PackageItem());

        USPSDigest digest = new USPSDigest(digestDate, mailpieces, packages);

        assertThat(digest.digestDate()).isEqualTo(digestDate);
        assertThat(digest.mailpieces()).hasSize(1);
        assertThat(digest.packages()).hasSize(1);
    }

    @Test
    void constructor_emptyLists() throws Exception {
        OffsetDateTime digestDate = OffsetDateTime.now(ZoneOffset.UTC);

        USPSDigest digest = new USPSDigest(digestDate, List.of(), List.of());

        assertThat(digest.digestDate()).isEqualTo(digestDate);
        assertThat(digest.mailpieces()).isEmpty();
        assertThat(digest.packages()).isEmpty();
    }

    @Test
    void recordEquality_sameValues_equal() throws Exception {
        OffsetDateTime digestDate = OffsetDateTime.of(2025, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        USPSDigest d1 = new USPSDigest(digestDate, List.of(), List.of());
        USPSDigest d2 = new USPSDigest(digestDate, List.of(), List.of());

        assertThat(d1).isEqualTo(d2);
        assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
    }
}
