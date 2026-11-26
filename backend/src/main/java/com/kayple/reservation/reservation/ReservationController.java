package com.kayple.reservation.reservation;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService service;
    private final SlotPolicy policy;

    public ReservationController(ReservationService service, SlotPolicy policy) {
        this.service = service;
        this.policy = policy;
    }

    /** 예약 가능 날짜: 서버(KST) 기준 오늘부터 7일 */
    @GetMapping("/dates")
    public List<LocalDate> dates() {
        return policy.bookableDates();
    }

    @GetMapping("/slots")
    public List<SlotResponse> slots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.slots(date);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@Valid @RequestBody ReserveCommand command) {
        return ReservationResponse.from(service.reserve(command), true);
    }

    public record PhoneRequest(String phone) {}

    /** 연락처가 URL·접근 로그에 남지 않도록 POST 본문으로 받는다 */
    @PostMapping("/reservations/lookup")
    public List<ReservationResponse> findByPhone(@RequestBody PhoneRequest request) {
        return service.findByPhone(request.phone()).stream()
                .map(r -> ReservationResponse.from(r, service.isCancellable(r)))
                .toList();
    }

    @PostMapping("/reservations/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id, @RequestBody PhoneRequest request) {
        service.cancel(id, request.phone());
    }
}
