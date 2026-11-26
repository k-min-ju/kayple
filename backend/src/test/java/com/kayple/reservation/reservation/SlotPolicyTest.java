package com.kayple.reservation.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class SlotPolicyTest {
    static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private SlotPolicy policyAt(int month, int day, int hour, int minute) {
        return new SlotPolicy(Clock.fixed(ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, KST).toInstant(), KST));
    }

    @Test
    void bookableDatesStartFromTodayForSevenDays() {
        assertThat(policyAt(11, 27, 9, 0).bookableDates())
                .first().isEqualTo(LocalDate.of(2026, 11, 27));
        assertThat(policyAt(11, 27, 9, 0).bookableDates())
                .hasSize(7).last().isEqualTo(LocalDate.of(2026, 12, 3));
    }

    @Test
    void dateRollsOverAtKstMidnightNotUtc() {
        // 11/26 23:59 KST = 11/26 14:59 UTC, 11/27 00:00 KST = 11/26 15:00 UTC
        assertThat(policyAt(11, 26, 23, 59).today()).isEqualTo(LocalDate.of(2026, 11, 26));
        assertThat(policyAt(11, 27, 0, 0).today()).isEqualTo(LocalDate.of(2026, 11, 27));
    }
}
