package com.kayple.reservation.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 비밀번호는 환경변수 ADMIN_PASSWORD 로만 주입. 비어 있으면 로그인 불가. */
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(String username, String password) {}
