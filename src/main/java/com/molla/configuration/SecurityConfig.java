package com.molla.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                // 1. Stateless session (JWT based)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 2. Authorization rules (order matters - more specific first)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()  // Allow auth endpoints (login, signup)
                        .requestMatchers("/api/super-admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/products/public/**").permitAll()  // Public products endpoints
                        .requestMatchers("/api/customers/**").permitAll()  // Allow customers endpoint for testing
                        .requestMatchers("/api/orders/**").permitAll()    // Allow orders endpoint for testing
                        .requestMatchers("/api/products/**").permitAll()  // Allow products endpoint for testing
                        .requestMatchers("/api/branches/**").permitAll()  // Allow branches endpoint for testing
                        .requestMatchers("/api/users/profile").permitAll()  // Allow user profile endpoint for testing
                        .requestMatchers("/api/shift-reports/**").permitAll()  // Allow shift reports endpoint for testing
                        .requestMatchers("/api/refunds/**").permitAll()  // Allow refunds endpoint for testing
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )

                // 3. JWT filter
                .addFilterBefore(new JwtValidator(), BasicAuthenticationFilter.class)

                // 4. Disable CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 5. Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 6. Build the filter chain
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {

                CorsConfiguration config = new CorsConfiguration();

                // Allow frontend URLs (development and production)
                String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
                if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
                    // Production: Use environment variable (comma-separated)
                    config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
                } else {
                    // Development: Default localhost URLs
                    config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
                }

                // Allow HTTP methods
                config.setAllowedMethods(List.of(
                        "GET", "POST", "PUT", "DELETE", "OPTIONS"
                ));

                // Allow headers
                config.setAllowedHeaders(List.of("*"));

                // Allow JWT token
                config.setAllowCredentials(true);

                return config;
            }
        };
    }
}