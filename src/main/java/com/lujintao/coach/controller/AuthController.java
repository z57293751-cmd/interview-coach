package com.lujintao.coach.controller;

import com.lujintao.coach.common.AccessInterceptor;
import com.lujintao.coach.common.ApiResult;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class AuthController {

    @Value("${coach.access.password:}")
    private String password;

    @PostMapping("/api/login")
    public ResponseEntity<ApiResult<Void>> login(@RequestBody LoginRequest request) {
        if (password == null || password.isBlank()) {
            return ResponseEntity.ok(ApiResult.ok());
        }
        String input = request.getPassword() == null ? "" : request.getPassword().trim();
        if (!password.equals(input)) {
            return ResponseEntity.status(401).body(ApiResult.fail(401, "口令不对，再试一次"));
        }
        ResponseCookie cookie = ResponseCookie
                .from(AccessInterceptor.COOKIE_NAME, AccessInterceptor.token(password))
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .maxAge(Duration.ofDays(30))
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResult.ok());
    }

    @PostMapping("/api/logout")
    public ResponseEntity<ApiResult<Void>> logout() {
        ResponseCookie cookie = ResponseCookie
                .from(AccessInterceptor.COOKIE_NAME, "")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResult.ok());
    }

    @Data
    public static class LoginRequest {
        private String password;
    }
}
