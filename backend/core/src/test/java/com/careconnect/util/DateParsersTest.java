package com.careconnect.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DateParsersTest {

    // ─── Private constructor ──────────────────────────────────────────────────

    @Test
    void privateConstructor_isInstantiableViaReflection() throws Exception {
        Constructor<DateParsers> ctor = DateParsers.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isNotNull();
    }

    // ─── parseOffsetOrLocalToUtc() ───────────────────────────────────────────

    @Test
    void parseOffsetOrLocalToUtc_null_returnsNow() {
        OffsetDateTime result = DateParsers.parseOffsetOrLocalToUtc(null);
        assertThat(result).isNotNull();
    }

    @Test
    void parseOffsetOrLocalToUtc_empty_returnsNow() {
        OffsetDateTime result = DateParsers.parseOffsetOrLocalToUtc("");
        assertThat(result).isNotNull();
    }

    @Test
    void parseOffsetOrLocalToUtc_blank_returnsNow() {
        OffsetDateTime result = DateParsers.parseOffsetOrLocalToUtc("   ");
        assertThat(result).isNotNull();
    }

    @Test
    void parseOffsetOrLocalToUtc_isoOffsetWithZ_parsesCorrectly() {
        OffsetDateTime result = DateParsers.parseOffsetOrLocalToUtc("2025-10-05T10:43:21.990Z");
        assertThat(result.getYear()).isEqualTo(2025);
        assertThat(result.getMonthValue()).isEqualTo(10);
        assertThat(result.getDayOfMonth()).isEqualTo(5);
        assertThat(result.getHour()).isEqualTo(10);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void parseOffsetOrLocalToUtc_isoOffsetWithPositiveOffset_parsesCorrectly() {
        OffsetDateTime result = DateParsers.parseOffsetOrLocalToUtc("2025-10-05T10:43:21.990+05:00");
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2025);
    }

    @Test
    void parseOffsetOrLocalToUtc_isoLocalDateTime_parsesAsUtc() {
        OffsetDateTime result = DateParsers.parseOffsetOrLocalToUtc("2025-10-05T10:43:21.000");
        assertThat(result.getYear()).isEqualTo(2025);
        assertThat(result.getMonthValue()).isEqualTo(10);
        assertThat(result.getDayOfMonth()).isEqualTo(5);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void parseOffsetOrLocalToUtc_isoDateOnly_parsesAsStartOfDayUtc() {
        OffsetDateTime result = DateParsers.parseOffsetOrLocalToUtc("2025-10-05");
        assertThat(result.getYear()).isEqualTo(2025);
        assertThat(result.getMonthValue()).isEqualTo(10);
        assertThat(result.getDayOfMonth()).isEqualTo(5);
        assertThat(result.getHour()).isEqualTo(0);
        assertThat(result.getMinute()).isEqualTo(0);
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    // ─── parseNullableOffsetOrLocalToUtc() ───────────────────────────────────

    @Test
    void parseNullable_null_returnsNull() {
        assertThat(DateParsers.parseNullableOffsetOrLocalToUtc(null)).isNull();
    }

    @Test
    void parseNullable_empty_returnsNull() {
        assertThat(DateParsers.parseNullableOffsetOrLocalToUtc("")).isNull();
    }

    @Test
    void parseNullable_blank_returnsNull() {
        assertThat(DateParsers.parseNullableOffsetOrLocalToUtc("   ")).isNull();
    }

    @Test
    void parseNullable_validIsoOffset_returnsValue() {
        OffsetDateTime result = DateParsers.parseNullableOffsetOrLocalToUtc("2025-10-05T10:43:21.990Z");
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2025);
    }

    // ─── format() ────────────────────────────────────────────────────────────

    @Test
    void format_null_returnsNull() {
        assertThat(DateParsers.format(null)).isNull();
    }

    @Test
    void format_utcDateTime_returnsIsoString() {
        OffsetDateTime odt = OffsetDateTime.of(2025, 10, 5, 10, 43, 21, 0, ZoneOffset.UTC);
        String result = DateParsers.format(odt);
        assertThat(result).isNotNull();
        assertThat(result).contains("2025-10-05");
        assertThat(result).contains("10:43:21");
    }

    @Test
    void format_nonUtcOffset_convertsToUtc() {
        // 15:43 +05:00 = 10:43 UTC
        OffsetDateTime odt = OffsetDateTime.of(2025, 10, 5, 15, 43, 21, 0, ZoneOffset.ofHours(5));
        String result = DateParsers.format(odt);
        assertThat(result).isNotNull();
        assertThat(result).contains("2025-10-05T10:43:21");
    }

    // ─── formatNullable() ────────────────────────────────────────────────────

    @Test
    void formatNullable_null_returnsNull() {
        assertThat(DateParsers.formatNullable(null)).isNull();
    }

    @Test
    void formatNullable_validDateTime_returnsString() {
        OffsetDateTime odt = OffsetDateTime.of(2025, 10, 5, 10, 43, 21, 0, ZoneOffset.UTC);
        String result = DateParsers.formatNullable(odt);
        assertThat(result).isNotNull().contains("2025-10-05");
    }
}
