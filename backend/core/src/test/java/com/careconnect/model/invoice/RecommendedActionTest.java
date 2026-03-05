package com.careconnect.model.invoice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendedActionTest {

    // ─── No-arg constructor ───────────────────────────────────────────────────

    @Test
    void noArgConstructor_createsInstance() throws Exception {
        RecommendedAction action = new RecommendedAction();

        assertThat(action).isNotNull();
        assertThat(action.getId()).isNull();
        assertThat(action.getInvoice()).isNull();
        assertThat(action.getActionText()).isNull();
    }

    // ─── All-arg constructor ──────────────────────────────────────────────────

    @Test
    void allArgConstructor_setsAllFields() throws Exception {
        Invoice invoice = Invoice.builder().id("INV-001").build();

        RecommendedAction action = new RecommendedAction(1L, invoice, "Review billing codes");

        assertThat(action.getId()).isEqualTo(1L);
        assertThat(action.getInvoice()).isSameAs(invoice);
        assertThat(action.getActionText()).isEqualTo("Review billing codes");
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

    @Test
    void setters_updateFields() throws Exception {
        RecommendedAction action = new RecommendedAction();
        Invoice invoice = Invoice.builder().id("INV-002").build();

        action.setId(5L);
        action.setInvoice(invoice);
        action.setActionText("Contact insurance provider");

        assertThat(action.getId()).isEqualTo(5L);
        assertThat(action.getInvoice()).isSameAs(invoice);
        assertThat(action.getActionText()).isEqualTo("Contact insurance provider");
    }

    // ─── equals() and hashCode() ──────────────────────────────────────────────

    @Test
    void equals_sameFields_returnsTrue() throws Exception {
        RecommendedAction a1 = new RecommendedAction(1L, null, "Action A");
        RecommendedAction a2 = new RecommendedAction(1L, null, "Action A");

        assertThat(a1).isEqualTo(a2);
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
    }

    @Test
    void equals_differentFields_returnsFalse() throws Exception {
        RecommendedAction a1 = new RecommendedAction(1L, null, "Action A");
        RecommendedAction a2 = new RecommendedAction(2L, null, "Action B");

        assertThat(a1).isNotEqualTo(a2);
    }

    @Test
    void equals_null_returnsFalse() throws Exception {
        RecommendedAction action = new RecommendedAction();
        assertThat(action).isNotEqualTo(null);
    }

    @Test
    void equals_differentType_returnsFalse() throws Exception {
        RecommendedAction action = new RecommendedAction();
        assertThat(action).isNotEqualTo("a string");
    }
}
