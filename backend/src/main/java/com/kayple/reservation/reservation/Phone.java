package com.kayple.reservation.reservation;

import com.kayple.reservation.common.ApiException;
import java.util.regex.Pattern;

/** 연락처 정규화·검증·마스킹. 저장은 숫자만. */
public final class Phone {
    public static final String REGEX = "^01[016789]-?\\d{3,4}-?\\d{4}$";
    public static final String INVALID_MESSAGE = "연락처 형식이 올바르지 않습니다";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private Phone() {}

    public static String normalize(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw ApiException.badRequest(INVALID_MESSAGE, "phone");
        }
        return trimmed.replace("-", "");
    }

    /** 01012345678 → 010-1234-5678 */
    public static String format(String digits) {
        int len = digits.length();
        return digits.substring(0, 3) + "-" + digits.substring(3, len - 4) + "-" + digits.substring(len - 4);
    }

    /** 01012345678 → 010-****-5678 */
    public static String mask(String digits) {
        int len = digits.length();
        return digits.substring(0, 3) + "-" + "*".repeat(len - 7) + "-" + digits.substring(len - 4);
    }
}
