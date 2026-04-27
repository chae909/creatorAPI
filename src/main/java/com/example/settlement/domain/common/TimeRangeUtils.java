package com.example.settlement.domain.common;

import com.example.settlement.domain.common.exception.BusinessException;
import com.example.settlement.domain.common.exception.ErrorCode;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public final class TimeRangeUtils {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private TimeRangeUtils() {}

    public record InstantRange(Instant start, Instant end) {}

    public static InstantRange toKstMonthRange(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        ZonedDateTime startKst = ym.atDay(1).atStartOfDay(KST);
        ZonedDateTime endKst = ym.atEndOfMonth().atTime(23, 59, 59).atZone(KST);
        return new InstantRange(startKst.toInstant(), endKst.toInstant());
    }

    public static YearMonth parseYearMonth(String input) {
        try {
            return YearMonth.parse(input);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_YEAR_MONTH);
        }
    }
}
