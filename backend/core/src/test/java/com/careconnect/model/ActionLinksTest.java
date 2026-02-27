package com.careconnect.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActionLinksTest {

    // ----- No-args constructor -----

    @Test
    void noArgsConstructor_createsInstanceWithNullFields() {
        ActionLinks links = new ActionLinks();

        assertThat(links.getTrack()).isNull();
        assertThat(links.getDeliveryInstructions()).isNull();
        assertThat(links.getScheduleRedelivery()).isNull();
        assertThat(links.getDashboard()).isNull();
    }

    // ----- All-args constructor -----

    @Test
    void allArgsConstructor_setsAllFields() {
        ActionLinks links = new ActionLinks(
                "https://track.example.com",
                "https://delivery.example.com",
                "https://redeliver.example.com",
                "https://dashboard.example.com"
        );

        assertThat(links.getTrack()).isEqualTo("https://track.example.com");
        assertThat(links.getDeliveryInstructions()).isEqualTo("https://delivery.example.com");
        assertThat(links.getScheduleRedelivery()).isEqualTo("https://redeliver.example.com");
        assertThat(links.getDashboard()).isEqualTo("https://dashboard.example.com");
    }

    // ----- Builder -----

    @Test
    void builder_setsAllFields() {
        ActionLinks links = ActionLinks.builder()
                .track("https://t.example.com")
                .deliveryInstructions("https://d.example.com")
                .scheduleRedelivery("https://r.example.com")
                .dashboard("https://dash.example.com")
                .build();

        assertThat(links.getTrack()).isEqualTo("https://t.example.com");
        assertThat(links.getDeliveryInstructions()).isEqualTo("https://d.example.com");
        assertThat(links.getScheduleRedelivery()).isEqualTo("https://r.example.com");
        assertThat(links.getDashboard()).isEqualTo("https://dash.example.com");
    }

    // ----- defaults() factory -----

    @Test
    void defaults_setsTrackUrlAndFixedUspsLinks() {
        ActionLinks links = ActionLinks.defaults("https://tools.usps.com/go/TrackConfirmAction?tRef=fullpage&tLc=1&tLabels=12345");

        assertThat(links.getTrack()).isEqualTo("https://tools.usps.com/go/TrackConfirmAction?tRef=fullpage&tLc=1&tLabels=12345");
        assertThat(links.getDeliveryInstructions()).isEqualTo("https://www.usps.com/manage/package-intercept.htm");
        assertThat(links.getScheduleRedelivery()).isEqualTo("https://tools.usps.com/redelivery.htm");
        assertThat(links.getDashboard()).isEqualTo("https://informeddelivery.usps.com/box/dashboard");
    }

    @Test
    void defaults_nullTrackUrl_setsNullTrack() {
        ActionLinks links = ActionLinks.defaults(null);

        assertThat(links.getTrack()).isNull();
        assertThat(links.getDeliveryInstructions()).isEqualTo("https://www.usps.com/manage/package-intercept.htm");
        assertThat(links.getScheduleRedelivery()).isEqualTo("https://tools.usps.com/redelivery.htm");
        assertThat(links.getDashboard()).isEqualTo("https://informeddelivery.usps.com/box/dashboard");
    }

    // ----- Setters (@Data) -----

    @Test
    void setters_updateFields() {
        ActionLinks links = new ActionLinks();
        links.setTrack("https://track.new");
        links.setDeliveryInstructions("https://delivery.new");
        links.setScheduleRedelivery("https://redeliver.new");
        links.setDashboard("https://dashboard.new");

        assertThat(links.getTrack()).isEqualTo("https://track.new");
        assertThat(links.getDeliveryInstructions()).isEqualTo("https://delivery.new");
        assertThat(links.getScheduleRedelivery()).isEqualTo("https://redeliver.new");
        assertThat(links.getDashboard()).isEqualTo("https://dashboard.new");
    }

    // ----- equals / hashCode (@Data) -----

    @Test
    void equals_sameFields_areEqual() {
        ActionLinks a = new ActionLinks("t", "d", "r", "dash");
        ActionLinks b = new ActionLinks("t", "d", "r", "dash");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_differentFields_areNotEqual() {
        ActionLinks a = new ActionLinks("t1", "d", "r", "dash");
        ActionLinks b = new ActionLinks("t2", "d", "r", "dash");

        assertThat(a).isNotEqualTo(b);
    }

    // ----- toString (@Data) -----

    @Test
    void toString_containsFieldValues() {
        ActionLinks links = new ActionLinks("https://track.url", "https://delivery.url", "https://redeliver.url", "https://dash.url");

        String str = links.toString();

        assertThat(str).contains("https://track.url");
        assertThat(str).contains("https://delivery.url");
        assertThat(str).contains("https://redeliver.url");
        assertThat(str).contains("https://dash.url");
    }
}
