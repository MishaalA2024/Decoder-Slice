package com.decoder.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that extracts username from JWT token or Authorization header.
 * For this minimal implementation, we use a simple mock JWT format:
 * "Bearer username:role"
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            // Mock JWT: simple format "username:role"
            // In production, this would decode a real JWT
            try {
                String[] parts = token.split(":");
                if (parts.length >= 1) {
                    String username = parts[0];
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(username);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user: {}", username);
                }
            } catch (Exception e) {
                log.warn("Failed to parse JWT token: {}", e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
