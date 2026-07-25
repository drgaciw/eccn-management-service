package com.aciworldwide.eccn_management_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // Browsers attach an Origin header to every state-changing request
                // (POST/PUT/PATCH/DELETE), even when it is routed "same-origin" through a
                // dev-server proxy. localhost:4200 is the actual `ng serve` dev port for the
                // Angular frontend (see angular.json); without it here, every mutating
                // request from that frontend was silently rejected with 403 "Invalid CORS
                // request" before Spring Security or the controller ever ran, while GET
                // requests (which browsers don't tag with Origin) kept working — the
                // asymmetry that made this easy to miss.
                .allowedOrigins("http://localhost:3000", "http://localhost:4200", "http://eccn-management-ui:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}