package com.molla.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

public class JwtValidator extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip JWT validation for auth endpoints (login, signup)
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Read Authorization header from request
        String authHeader = request.getHeader(JwtConstant.JWT_HEADERS);

        // 2. Check if header exists and starts with "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            // 3. Extract actual JWT token (remove "Bearer ")
            String token = authHeader.substring(7);

            try {
                // 4. Create secret key using the same secret used while generating token
                SecretKey key = Keys.hmacShaKeyFor(
                        JwtConstant.JWT_SECRET.getBytes()
                );

                // 5. Parse and validate JWT token
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // 6. Extract user details from token
                String email = claims.getSubject(); // usually email or username
                String role = claims.get("role", String.class);

                // 7. Convert role into Spring Security authority
                // Role already includes "ROLE_" prefix from UserRole enum
                String authorityString = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority(authorityString);

                // 8. Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(authority)
                        );

                // 9. Store authentication in Spring Security context
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (Exception e) {
                // Invalid token → user remains unauthenticated
                System.out.println("Invalid JWT Token: " + e.getMessage());
            }
        }

        // 10. Continue filter chain (VERY IMPORTANT)
        filterChain.doFilter(request, response);
    }
}