package com.hrassistant.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Validates "Authorization: Bearer <jwt>" on every /api/** route except login,
 * and stores the authenticated employee's id and role as request attributes for
 * controllers to read. This is a lightweight, dependency-free stand-in for the
 * full Spring Security filter chain - fine for a project this size.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthFilter(JwtService jwtService) { this.jwtService = jwtService; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(response, "missing bearer token");
            return;
        }
        Claims claims = jwtService.parse(header.substring(7));
        if (claims == null) {
            unauthorized(response, "invalid or expired token");
            return;
        }
        request.setAttribute("employeeId", Long.valueOf(claims.getSubject()));
        request.setAttribute("role", claims.get("role", String.class));

        if (request.getRequestURI().startsWith("/api/hr/") && !"HR".equals(claims.get("role", String.class))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            mapper.writeValue(response.getWriter(), Map.of("error", "HR role required"));
            return;
        }
        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        mapper.writeValue(response.getWriter(), Map.of("error", message));
    }
}
