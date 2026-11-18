package com.inventaya.ventas.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final RolInterceptor rolInterceptor;

    public WebConfig(RolInterceptor rolInterceptor) {
        this.rolInterceptor = rolInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rolInterceptor).addPathPatterns("/api/**");
    }
}
