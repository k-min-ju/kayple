package com.kayple.reservation;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** 2026-11-26(목) 12:30 KST 로 고정: 오늘 10·11시는 이미 지난 시간. */
@TestConfiguration
public class TestClockConfig {
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(ZonedDateTime.of(2026, 11, 26, 12, 30, 0, 0, KST).toInstant(), KST);
    }
}
