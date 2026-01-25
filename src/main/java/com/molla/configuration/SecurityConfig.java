package com.molla.configuration;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔐 Security Configuration - Main entry point for Spring Security
 * 
 * 👉 Purpose: Configures authentication, authorization, filters, and CORS
 * 🔹 Key: Stateless JWT-based authentication (no server-side sessions)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 🔑 Password Encoder Bean
     * 👉 Purpose: BCrypt hashing for passwords (one-way encryption)
     * 🔹 Why: Never store plain passwords - always hash before saving to DB
     */
    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", Instant.now().toString());
            body.put("error", "FORBIDDEN");
            body.put("message", "Access denied. Super admin only.");
            body.put("status", 403);
            new ObjectMapper().writeValue(response.getOutputStream(), body);
        };
    }

    /**
     * 🔐 Authentication Manager Bean
     * 👉 Purpose: Handles username/password authentication (used in AuthController)
     * 🔹 Why: Spring Security needs this to validate credentials during login
     */
    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration c)
            throws Exception {
        return c.getAuthenticationManager();
    }

    /**
     * 🛡️ Security Filter Chain - Core Security Configuration
     * 
     * 👉 Purpose: Defines which endpoints are public/protected and filter order
     * 🔹 Flow: Request → RateLimitingFilter → JwtFilter → UsernamePasswordAuthenticationFilter → Controller
     * 
     * 📌 Filter Order Explanation:
     * 1. RateLimitingFilter runs FIRST → Blocks excessive requests before processing
     * 2. JwtFilter runs SECOND → Validates JWT token and sets authentication context
     * 3. UsernamePasswordAuthenticationFilter runs LAST → Spring's default auth (we skip it with JWT)
     */
    @Bean
    public SecurityFilterChain chain(HttpSecurity http, JwtFilter filter)
            throws Exception {

        // Disable CSRF (Cross-Site Request Forgery) - not needed for stateless JWT APIs
        http.csrf(cs -> cs.disable())
                .authorizeHttpRequests(auth -> auth
                        // ⚠️ Swagger paths MUST be first! (order matters in Spring Security)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html").permitAll()
                        .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                        // 🔓 Public endpoints - no authentication required
                        .requestMatchers("/auth/**").permitAll()  // Login/signup endpoints
                        .requestMatchers("/api/products/public/**").permitAll()  // Public product catalog
                        .requestMatchers("/api/payments/**").permitAll()  // Payment endpoints (Razorpay)
                        .requestMatchers("/api/payment/**").permitAll()  // Payment endpoints (Stripe)
                        // 🔒 Super-admin-only: view all stores, delete any store, moderate
                        .requestMatchers(HttpMethod.GET, "/api/stores").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/stores/*/moderate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/stores/*").hasRole("ADMIN")
                        // 🔒 PUT /api/stores/*: authenticated (service layer enforces: store admin = own store, super admin = any store)
                        // 🔒 Other protected endpoints
                        .requestMatchers("/api/super-admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())  // All other endpoints need valid JWT token
                // 📌 Stateless sessions - no server-side session storage (JWT handles state)
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()));

        // 🔗 Filter Chain Order (CRITICAL for viva):
        // 👉 JwtFilter runs BEFORE UsernamePasswordAuthenticationFilter
        // 🔹 Meaning: My JWT validation happens first, before Spring's default username/password auth
        http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        
        // 👉 RateLimitingFilter runs BEFORE JwtFilter
        // 🔹 Meaning: Rate limiting checks happen first, then JWT validation (saves resources)
        http.addFilterBefore(new RateLimitingFilter(), JwtFilter.class);
        
        // 🌐 Enable CORS (Cross-Origin Resource Sharing) for frontend communication
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    /**
     * 🌐 CORS Configuration Source
     * 
     * 👉 Purpose: Allows frontend (React) to call backend APIs from different origin
     * 🔹 Why: Browser blocks cross-origin requests by default (security feature)
     * 
     * 📌 How it works:
     * - Production: Reads ALLOWED_ORIGINS from environment variable
     * - Development: Uses default localhost ports (5173, 3000, etc.)
     * - Returns CORS headers (Access-Control-Allow-Origin) in response
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
        
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {

                CorsConfiguration config = new CorsConfiguration();

                // 🔍 Check environment variable for production origins
                String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
                String origin = request.getHeader("Origin");
                
                List<String> origins;
                if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
                    // 🏭 Production: Parse comma-separated origins from env variable
                    origins = Arrays.stream(allowedOrigins.split(","))
                            .map(String::trim)
                            .map(s -> s.startsWith("=") ? s.substring(1) : s)  // Fix accidental '=' prefix
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                } else {
                    // 💻 Development: Default localhost ports for local testing
                    origins = List.of(
                        "http://localhost:*",  // Allow all localhost ports (includes 63342, 3000, 5173, etc.)
                        "http://127.0.0.1:*"   // Allow all 127.0.0.1 ports
                    );
                    logger.warn("ALLOWED_ORIGINS not set, using default localhost patterns: {}", origins);
                }

                // ✅ Set allowed origins (patterns allow dynamic port matching with *)
                config.setAllowedOriginPatterns(origins);
                logger.info("CORS Configuration - Allowed Origin Patterns: {}", origins);
                logger.info("CORS Configuration - Request Origin: {}", origin);

                // ✅ Allow all HTTP methods (GET, POST, PUT, DELETE, PATCH, OPTIONS)
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

                // ✅ Allow all headers (including Authorization for JWT)
                config.setAllowedHeaders(List.of("*"));

                // ✅ Allow credentials (cookies, authorization headers)
                config.setAllowCredentials(true);

                return config;
            }
        };
    }
}
