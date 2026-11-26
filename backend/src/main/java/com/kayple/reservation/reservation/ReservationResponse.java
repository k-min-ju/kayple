package com.kayple.reservation.reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 사용자용 응답: 연락처는 마스킹. */
public record ReservationResponse(Long id, LocalDate date, int hour, String name, String phone,
                                  ReservationStatus status, LocalDateTime createdAt, boolean cancellable) {
    public static ReservationResponse from(Reservation r, boolean cancellable) {
        return new ReservationResponse(r.getId(), r.getDate(), r.getHour(), r.getName(),
                Phone.mask(r.getPhone()), r.getStatus(), r.getCreatedAt(), cancellable);
    }

    /** 관리자용: 연락처 전체 노출. */
    public static ReservationResponse forAdmin(Reservation r) {
        return new ReservationResponse(r.getId(), r.getDate(), r.getHour(), r.getName(),
                Phone.format(r.getPhone()), r.getStatus(), r.getCreatedAt(), false);
    }
}
