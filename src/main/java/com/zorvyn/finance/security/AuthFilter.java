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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private static final Map<String, long[]> requestCount = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_MS = 60_000; // 1 minute
    private static final long STALE_THRESHOLD_MS = WINDOW_MS * 2;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        
        if (Math.random() < 0.001) {
            requestCount.entrySet().removeIf(e -> now - e.getValue()[1] > STALE_THRESHOLD_MS);
        }

        final long[][] result = new long[1][];
        requestCount.compute(ip, (key, existing) -> {
            if (existing == null || now - existing[1] > WINDOW_MS) {
                long[] fresh = new long[]{1, now};
                result[0] = fresh;
                return fresh;
            }
            existing[0]++;
            result[0] = existing;
            return existing;
        });

        if (result[0][0] > MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Limit is 100 requests per minute.\"}");
            return;
        }

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                // Reject refresh tokens presented as Bearer tokens.
                // Previously, a refresh token would pass signature validation,
                // extractRole() would return null, and the SecurityContext would be
                // set with "ROLE_null" — wrong behavior even though endpoints would
                // still reject it downstream via @PreAuthorize.
                if (jwtUtil.isRefreshToken(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Refresh token cannot be used for authentication\"}");
                    return;
                }

                String userId = jwtUtil.extractUserId(token);
                AuthContext.set(userId);

                String role = jwtUtil.extractRole(token);
                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
                return;
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }
}