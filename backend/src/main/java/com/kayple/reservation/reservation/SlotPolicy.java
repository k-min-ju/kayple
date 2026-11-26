package com.kayple.reservation.reservation;

import com.kayple.reservation.common.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** 예약 가능 날짜(오늘부터 7일)·시간(10~17시, 12시 점심 제외) 규칙. */
@Component
public class SlotPolicy {
    public static final List<Integer> HOURS = List.of(10, 11, 13, 14, 15, 16, 17);
    public static final int BOOKABLE_DAYS = 7;

    private final Clock clock;

    public SlotPolicy(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public List<LocalDate> bookableDates() {
        return Stream.iterate(today(), d -> d.plusDays(1)).limit(BOOKABLE_DAYS).toList();
    }

    public boolean isPast(LocalDate date, int hour) {
        return !date.atTime(hour, 0).isAfter(LocalDateTime.now(clock));
    }

    public void validateDate(LocalDate date) {
        if (date == null || !bookableDates().contains(date)) {
            throw ApiException.badRequest("예약할 수 없는 날짜입니다", "date");
        }
    }

    public void validate(LocalDate date, int hour) {
        validateDate(date);
        if (!HOURS.contains(hour) || isPast(date, hour)) {
            throw ApiException.badRequest("예약할 수 없는 시간입니다", "hour");
        }
    }
}
