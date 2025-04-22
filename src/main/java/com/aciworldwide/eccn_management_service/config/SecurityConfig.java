package com.aciworldwide.eccn_management_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.time.Duration;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("*")); // In production, restrict to specific origins
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("X-Total-Count", "Content-Disposition"));
        configuration.setMaxAge(Duration.ofSeconds(3600L)); // 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Profile("prod")
    public SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
        return configureHttp(http)
            // Enable CSRF protection in production
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/**")) // Exclude API endpoints if they're stateless
            .build();
    }

    @Bean
    @Profile("!prod")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        return configureHttp(http)
            // Disable CSRF for development only
            .csrf(csrf -> csrf.disable())
            .build();
    }

    private HttpSecurity configureHttp(HttpSecurity http) throws Exception {
        return http
            // Configure security headers
            .headers(headers -> headers
                // Add X-Content-Type-Options to prevent MIME type sniffing
                .contentTypeOptions(Customizer.withDefaults())
                // Add X-Frame-Options to prevent clickjacking attacks
                .frameOptions(config -> config.deny())
                // Add Content-Security-Policy header for XSS protection
                .addHeaderWriter((_, response) ->
                    response.setHeader("Content-Security-Policy",
                        "default-src 'self'; script-src 'self'; img-src 'self'; style-src 'self'; font-src 'self'; connect-src 'self'"))
                // Add Referrer Policy header
                .addHeaderWriter((_, response) ->
                    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin"))
                // Add Permissions Policy header
                .addHeaderWriter((_, response) ->
                    response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
                // Add Cache-Control header to prevent caching of sensitive information
                .cacheControl(cache -> cache.disable())
            )
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Configure session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Configure authorization rules
            .authorizeHttpRequests(authorize -> authorize
                // Permit all requests to documentation and management endpoints
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                // Secure all other endpoints
                .anyRequest().authenticated()
            )
            // Configure HTTP Basic authentication
            .httpBasic(Customizer.withDefaults());
    }
}
