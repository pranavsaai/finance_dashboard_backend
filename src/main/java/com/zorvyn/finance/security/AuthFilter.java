package com.zorvyn.finance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private static final Map<String, Integer> requestCount = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
                String ip = request.getRemoteAddr();

                requestCount.put(ip, requestCount.getOrDefault(ip, 0) + 1);

                if (requestCount.get(ip) > 100) {
                    response.setStatus(429);
                    response.getWriter().write("Too many requests");
                    return;
                }

                String header = request.getHeader("Authorization");

                if (header != null && header.startsWith("Bearer ")) {
                    String token = header.substring(7);

                    try {
                        String userId = jwtUtil.extractUserId(token);
                        AuthContext.set(userId);
                        String role = jwtUtil.extractRole(token);
                        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        System.out.println("Authenticated user: " + userId);

                    } catch (Exception e) {
                        System.out.println("JWT ERROR: " + e.getMessage());
                    }
                }

                try {
                    chain.doFilter(request, response);
                } finally {
                    AuthContext.clear();
                }
            }
}