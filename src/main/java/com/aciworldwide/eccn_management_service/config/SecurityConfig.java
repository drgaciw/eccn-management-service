package com.aciworldwide.eccn_management_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Prefer Spring's delegating encoder so local/dev credentials can use {@code {noop}...}
     * while production-style bcrypt hashes use {@code {bcrypt}...}.
     * <p>
     * A bare {@link BCryptPasswordEncoder} bean rejects {@code {noop}admin} (the
     * application.properties default), which made every "plain" restart look broken:
     * the app came up but Basic auth always returned 401.
     * Unprefixed bcrypt hashes (legacy test props) still match via the default-for-matches
     * fallback.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        PasswordEncoder delegating = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        if (delegating instanceof DelegatingPasswordEncoder dpe) {
            dpe.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        }
        return delegating;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Integrate Spring MVC CORS (WebConfig) into the security filter chain so
            // OPTIONS preflight receives Access-Control-* headers instead of a 401
            // Basic challenge. Without this, browser POST/PUT/DELETE from the Angular
            // dev server fails with "Invalid CORS request" / blocked preflight.
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/prometheus",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api/health"
                ).permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
