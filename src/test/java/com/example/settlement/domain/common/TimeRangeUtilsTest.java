package com.example.settlement.domain.common;

import com.example.settlement.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeRangeUtilsTest {

    @Test
    void march2025_startsAtEndOfFebUTC_endsAtMarch31UTC() {
        var range = TimeRangeUtils.toKstMonthRange(2025, 3);
        assertThat(range.start()).isEqualTo(Instant.parse("2025-02-28T15:00:00Z"));
        assertThat(range.end()).isEqualTo(Instant.parse("2025-03-31T14:59:59Z"));
    }

    @Test
    void january2025_startIsLastDayOfDecemberUTC() {
        var range = TimeRangeUtils.toKstMonthRange(2025, 1);
        assertThat(range.start()).isEqualTo(Instant.parse("2024-12-31T15:00:00Z"));
    }

    @Test
    void february2024_leapYear_endDayIs29() {
        var range = TimeRangeUtils.toKstMonthRange(2024, 2);
        assertThat(range.end()).isEqualTo(Instant.parse("2024-02-29T14:59:59Z"));
    }

    @Test
    void parseYearMonth_validFormat_returnsYearMonth() {
        assertThat(TimeRangeUtils.parseYearMonth("2025-03")).isEqualTo(YearMonth.of(2025, 3));
    }

    @Test
    void parseYearMonth_singleDigitMonth_throwsBusinessException() {
        assertThatThrownBy(() -> TimeRangeUtils.parseYearMonth("2025-3"))
                .isInstanceOf(BusinessException.class);
    }
}
