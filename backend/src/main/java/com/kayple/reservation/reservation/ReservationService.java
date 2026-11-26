package com.kayple.reservation.reservation;

import com.kayple.reservation.common.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    static final String DUPLICATE_MESSAGE = "이미 예약된 시간입니다. 다른 시간을 선택해 주세요.";

    private final ReservationRepository repository;
    private final SlotPolicy policy;
    private final Clock clock;

    public ReservationService(ReservationRepository repository, SlotPolicy policy, Clock clock) {
        this.repository = repository;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> slots(LocalDate date) {
        policy.validateDate(date);
        Set<Integer> booked = repository.findByDateAndStatus(date, ReservationStatus.BOOKED).stream()
                .map(Reservation::getHour)
                .collect(Collectors.toSet());
        return SlotPolicy.HOURS.stream()
                .map(h -> new SlotResponse(h, !booked.contains(h) && !policy.isPast(date, h)))
                .toList();
    }

    /** 중복은 선조회 대신 slot_key UNIQUE 제약으로 막는다: 동시 요청에서도 한 건만 성공. */
    @Transactional
    public Reservation reserve(ReserveCommand command) {
        policy.validate(command.date(), command.hour());
        String phone = Phone.normalize(command.phone());
        String name = command.name() == null ? "" : command.name().trim();
        if (name.isEmpty() || name.length() > 20) {
            throw ApiException.badRequest("이름을 입력해 주세요", "name");
        }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        try {
            return repository.saveAndFlush(new Reservation(command.date(), command.hour(), name, phone, now));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict(DUPLICATE_MESSAGE);
        }
    }

    @Transactional(readOnly = true)
    public List<Reservation> findByPhone(String rawPhone) {
        return repository.findByPhoneOrderByDateDescHourDesc(Phone.normalize(rawPhone));
    }

    /** 연락처가 일치하는 예약만 취소. 불일치도 404 로 응답해 예약 존재 여부를 숨긴다. */
    @Transactional
    public void cancel(Long id, String rawPhone) {
        Reservation reservation = repository.findByIdAndPhone(id, Phone.normalize(rawPhone))
                .filter(r -> r.getStatus() == ReservationStatus.BOOKED && isCancellable(r))
                .orElseThrow(() -> ApiException.notFound("취소할 예약을 찾을 수 없습니다"));
        reservation.cancel();
    }

    /** 이미 지난 상담은 이력 보존을 위해 취소 불가 */
    public boolean isCancellable(Reservation r) {
        return r.getStatus() == ReservationStatus.BOOKED && !policy.isPast(r.getDate(), r.getHour());
    }

    @Transactional(readOnly = true)
    public List<Reservation> findByDate(LocalDate date) {
        return repository.findByDateOrderByHourAscCreatedAtAsc(date);
    }
}
