package com.kayple.reservation.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ReserveCommand(
        @NotNull(message = "날짜를 선택해 주세요") LocalDate date,
        @NotNull(message = "시간을 선택해 주세요") Integer hour,
        @NotBlank(message = "이름을 입력해 주세요") @Size(max = 20, message = "이름은 20자 이하로 입력해 주세요") String name,
        @NotBlank(message = "연락처를 입력해 주세요") @Pattern(regexp = Phone.REGEX, message = Phone.INVALID_MESSAGE) String phone) {}
