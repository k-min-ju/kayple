package com.kayple.reservation.reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByDateAndStatus(LocalDate date, ReservationStatus status);

    List<Reservation> findByDateOrderByHourAscCreatedAtAsc(LocalDate date);

    List<Reservation> findByPhoneOrderByDateDescHourDesc(String phone);

    Optional<Reservation> findByIdAndPhone(Long id, String phone);
}
