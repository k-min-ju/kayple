package com.kayple.reservation.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation", indexes = @Index(name = "idx_reservation_phone", columnList = "phone"))
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserved_date", nullable = false)
    private LocalDate date;

    @Column(name = "reserved_hour", nullable = false)
    private int hour;

    @Column(nullable = false, length = 20)
    private String name;

    /** 숫자만 저장 (예: 01012345678) */
    @Column(nullable = false, length = 11)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReservationStatus status;

    /**
     * 중복 예약 방지 키. 예약 중일 때만 "yyyy-MM-dd-HH", 취소되면 NULL.
     * UNIQUE 제약은 NULL 을 중복으로 보지 않으므로 취소된 시간은 다시 예약할 수 있다.
     */
    @Column(name = "slot_key", unique = true, length = 13)
    private String slotKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Reservation() {}

    public Reservation(LocalDate date, int hour, String name, String phone, LocalDateTime createdAt) {
        this.date = date;
        this.hour = hour;
        this.name = name;
        this.phone = phone;
        this.status = ReservationStatus.BOOKED;
        this.slotKey = slotKeyOf(date, hour);
        this.createdAt = createdAt;
    }

    static String slotKeyOf(LocalDate date, int hour) {
        return "%s-%02d".formatted(date, hour);
    }

    void cancel() {
        this.status = ReservationStatus.CANCELLED;
        this.slotKey = null;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public int getHour() { return hour; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public ReservationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
