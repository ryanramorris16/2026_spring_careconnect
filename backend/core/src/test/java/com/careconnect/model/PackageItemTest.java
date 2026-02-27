package com.careconnect.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PackageItemTest {

    // ─── No-arg constructor ───────────────────────────────────────────────────

    @Test
    void noArgConstructor_createsInstance() {
        PackageItem item = new PackageItem();

        assertThat(item).isNotNull();
        assertThat(item.getTrackingNumber()).isNull();
        assertThat(item.getSender()).isNull();
        assertThat(item.getExpectedDeliveryDate()).isNull();
        assertThat(item.getActionLinks()).isNull();
    }

    // ─── All-arg constructor ──────────────────────────────────────────────────

    @Test
    void allArgConstructor_setsAllFields() {
        OffsetDateTime deliveryDate = OffsetDateTime.of(2025, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        ActionLinks links = ActionLinks.builder().build();

        PackageItem item = new PackageItem("TRACK123", "Amazon", deliveryDate, links);

        assertThat(item.getTrackingNumber()).isEqualTo("TRACK123");
        assertThat(item.getSender()).isEqualTo("Amazon");
        assertThat(item.getExpectedDeliveryDate()).isEqualTo(deliveryDate);
        assertThat(item.getActionLinks()).isSameAs(links);
    }

    // ─── Builder ──────────────────────────────────────────────────────────────

    @Test
    void builder_setsFields() {
        OffsetDateTime deliveryDate = OffsetDateTime.now(ZoneOffset.UTC);

        PackageItem item = PackageItem.builder()
                .trackingNumber("TRACK456")
                .sender("Best Buy")
                .expectedDeliveryDate(deliveryDate)
                .build();

        assertThat(item.getTrackingNumber()).isEqualTo("TRACK456");
        assertThat(item.getSender()).isEqualTo("Best Buy");
        assertThat(item.getExpectedDeliveryDate()).isEqualTo(deliveryDate);
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    void setters_updateFields() {
        PackageItem item = new PackageItem();
        OffsetDateTime deliveryDate = OffsetDateTime.now(ZoneOffset.UTC);
        ActionLinks links = new ActionLinks();

        item.setTrackingNumber("TRACK789");
        item.setSender("Walmart");
        item.setExpectedDeliveryDate(deliveryDate);
        item.setActionLinks(links);

        assertThat(item.getTrackingNumber()).isEqualTo("TRACK789");
        assertThat(item.getSender()).isEqualTo("Walmart");
        assertThat(item.getExpectedDeliveryDate()).isEqualTo(deliveryDate);
        assertThat(item.getActionLinks()).isSameAs(links);
    }
}
