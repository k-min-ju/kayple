package com.kayple.reservation.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kayple.reservation.TestClockConfig;
import com.kayple.reservation.common.ApiException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

@SpringBootTest
@Import(TestClockConfig.class)
class ReservationServiceTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 26);

    @Autowired ReservationService service;
    @Autowired ReservationRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private ReserveCommand cmd(LocalDate date, int hour, String phone) {
        return new ReserveCommand(date, hour, "홍길동", phone);
    }

    @Test
    void duplicateBookingRejected() {
        service.reserve(cmd(TODAY.plusDays(1), 14, "010-1234-5678"));

        assertThatThrownBy(() -> service.reserve(cmd(TODAY.plusDays(1), 14, "010-9999-0000")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).isEqualTo("이미 예약된 시간입니다. 다른 시간을 선택해 주세요.");
                });
    }

    @Test
    void cancelFreesSlot() {
        Reservation first = service.reserve(cmd(TODAY.plusDays(1), 14, "010-1234-5678"));
        service.cancel(first.getId(), "01012345678");

        Reservation second = service.reserve(cmd(TODAY.plusDays(1), 14, "010-9999-0000"));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(repository.findById(first.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void concurrentBookingOnlyOneSucceeds() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String phone = "010-1111-000" + i;
            tasks.add(() -> {
                try {
                    service.reserve(cmd(TODAY.plusDays(2), 15, phone));
                    return true;
                } catch (ApiException e) {
                    return false;
                }
            });
        }
        long success = 0;
        for (Future<Boolean> f : pool.invokeAll(tasks)) {
            if (f.get()) success++;
        }
        pool.shutdown();

        assertThat(success).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void rejectsOutOfRangeSlot() {
        assertBadRequest(cmd(TODAY.plusDays(7), 14, "010-1234-5678")); // 8일째
        assertBadRequest(cmd(TODAY.minusDays(1), 14, "010-1234-5678")); // 어제
        assertBadRequest(cmd(TODAY.plusDays(1), 12, "010-1234-5678")); // 점심
        assertBadRequest(cmd(TODAY, 11, "010-1234-5678"));             // 오늘 이미 지난 시간
        assertThat(service.reserve(cmd(TODAY.plusDays(6), 17, "010-1234-5678")).getId()).isNotNull();
    }

    @Test
    void cancelRequiresMatchingPhone() {
        Reservation r = service.reserve(cmd(TODAY.plusDays(1), 10, "010-1234-5678"));

        assertThatThrownBy(() -> service.cancel(r.getId(), "010-0000-0000"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(repository.findById(r.getId()).orElseThrow().getStatus()).isEqualTo(ReservationStatus.BOOKED);
    }

    @Test
    void lookupReturnsBothStatusesAndSlotsShowBooked() {
        Reservation a = service.reserve(cmd(TODAY.plusDays(1), 10, "010-1234-5678"));
        service.reserve(cmd(TODAY.plusDays(1), 13, "010-1234-5678"));
        service.cancel(a.getId(), "010-1234-5678");

        assertThat(service.findByPhone("01012345678")).hasSize(2);
        assertThat(service.slots(TODAY.plusDays(1)))
                .filteredOn(s -> !s.available())
                .extracting(SlotResponse::hour)
                .containsExactly(13);
        assertThat(service.slots(TODAY))
                .filteredOn(s -> !s.available())
                .extracting(SlotResponse::hour)
                .containsExactly(10, 11);
    }

    private void assertBadRequest(ReserveCommand command) {
        assertThatThrownBy(() -> service.reserve(command))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
