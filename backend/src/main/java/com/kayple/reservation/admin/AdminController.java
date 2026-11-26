package com.kayple.reservation.admin;

import com.kayple.reservation.reservation.ReservationResponse;
import com.kayple.reservation.reservation.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.kayple.reservation.common.ErrorResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminProperties properties;
    private final ReservationService service;

    public AdminController(AdminProperties properties, ReservationService service) {
        this.properties = properties;
        this.service = service;
    }

    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        if (!matches(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("아이디 또는 비밀번호가 올바르지 않습니다", "password"));
        }
        HttpSession old = http.getSession(false);
        if (old != null) old.invalidate(); // 세션 고정 방지
        http.getSession(true).setAttribute(AdminAuthInterceptor.SESSION_KEY, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        if (session != null) session.invalidate();
    }

    /** 프론트가 로그인 상태 확인용으로 호출 (인터셉터 통과 시 204) */
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void me() {}

    @GetMapping("/reservations")
    public List<ReservationResponse> reservations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.findByDate(date).stream().map(ReservationResponse::forAdmin).toList();
    }

    private boolean matches(LoginRequest request) {
        String expected = properties.password();
        if (expected == null || expected.isBlank() || request.username() == null || request.password() == null) {
            return false;
        }
        boolean user = constantTimeEquals(properties.username(), request.username());
        boolean pass = constantTimeEquals(expected, request.password());
        return user & pass;
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
