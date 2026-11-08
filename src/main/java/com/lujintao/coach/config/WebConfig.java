package com.lujintao.coach.config;

import com.lujintao.coach.common.AccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccessInterceptor accessInterceptor;

    public WebConfig(AccessInterceptor accessInterceptor) {
        this.accessInterceptor = accessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login.html",
                        "/api/login",
                        "/api/ping",
                        "/manifest.json",
                        "/icon-192.png",
                        "/icon-512.png",
                        "/style.css",
                        "/favicon.ico");
    }
}
