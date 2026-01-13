package com.molla.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll() // Allow OPTIONS for CORS preflight
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
                
                // Ensure CORS preflight OPTIONS requests are allowed
                .headers(headers -> headers.frameOptions().disable())

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
        Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
        
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {

                CorsConfiguration config = new CorsConfiguration();

                // Allow frontend URLs (development and production)
                String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
                String origin = request.getHeader("Origin");
                
                List<String> origins;
                       if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
                           // Production: Use environment variable (comma-separated)
                           // Trim whitespace and strip any accidental leading '=' from each origin
                           origins = Arrays.stream(allowedOrigins.split(","))
                                   .map(String::trim)
                                   .map(s -> s.startsWith("=") ? s.substring(1) : s)
                                   .filter(s -> !s.isEmpty())
                                   .collect(Collectors.toList());
                       } else {
                           // Development: Allow any localhost port
                           // List all common localhost ports explicitly for better compatibility
                           origins = List.of(
                               "http://localhost:3000",
                               "http://localhost:5173",
                               "http://localhost:5174",
                               "http://localhost:5175",
                               "http://localhost:8080",
                               "http://127.0.0.1:3000",
                               "http://127.0.0.1:5173",
                               "http://127.0.0.1:5174"
                           );
                           logger.warn("ALLOWED_ORIGINS not set, using default localhost origins: {}", origins);
                       }

                       // Use origin *patterns* instead of exact origins so Spring will match dynamically.
                       // This is more robust across proxies and scheme/port nuances.
                       config.setAllowedOriginPatterns(origins);

                logger.info("CORS Configuration - Allowed Origin Patterns: {}", origins);
                logger.info("CORS Configuration - Request Origin: {}", origin);

                // Allow HTTP methods
                config.setAllowedMethods(List.of(
                        "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
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