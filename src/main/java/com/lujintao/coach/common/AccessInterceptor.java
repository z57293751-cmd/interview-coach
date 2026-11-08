package com.lujintao.coach.common;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 访问口令拦截器。
 * 没配置口令（coach.access.password 为空）时直接放行，方便本地开发。
 */
@Component
public class AccessInterceptor implements HandlerInterceptor {

    public static final String COOKIE_NAME = "coach_pass";

    @Value("${coach.access.password:}")
    private String password;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (password == null || password.isBlank()) {
            return true;
        }
        String expected = token(password);
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName()) && expected.equals(cookie.getValue())) {
                    return true;
                }
            }
        }

        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"需要访问口令，请先登录\",\"data\":null}");
        } else {
            response.sendRedirect("/login.html");
        }
        return false;
    }

    /** 不把口令明文放进 Cookie，存的是加盐哈希。 */
    public static String token(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(("coach:" + rawPassword).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 32);
        } catch (Exception e) {
            throw new IllegalStateException("计算口令哈希失败", e);
        }
    }
}
