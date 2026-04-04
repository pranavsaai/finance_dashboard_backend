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

    // Time-window based rate limiter (per IP, resets every WINDOW_MS).
    // This is an in-memory limiter — works for single-instance deployments.
    // For multi-instance setups, replace with a distributed solution (as i mentioned it in readme already)
    private static final Map<String, RequestInfo> requestCount = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_MS = 60_000; // 1 minute

    // Stale entries are pruned during each request to prevent unbounded memory growth.
    // Any IP whose last window started more than WINDOW_MS ago and is no longer rate-limited
    // gets removed from the map. This keeps memory bounded without a separate cleanup thread.
    private static final long STALE_THRESHOLD_MS = WINDOW_MS * 2;

    private static class RequestInfo {
        int count;
        long startTime;

        RequestInfo(int count, long startTime) {
            this.count = count;
            this.startTime = startTime;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        // Prune stale entries periodically to prevent map from growing indefinitely.
        // Only runs every 1000 requests (cheap probabilistic cleanup).
        if (Math.random() < 0.001) {
            requestCount.entrySet().removeIf(e -> now - e.getValue().startTime > STALE_THRESHOLD_MS);
        }

        RequestInfo info = requestCount.getOrDefault(ip, new RequestInfo(0, now));

        // Reset window after 1 minute
        if (now - info.startTime > WINDOW_MS) {
            info = new RequestInfo(0, now);
        }

        info.count++;
        requestCount.put(ip, info);

        if (info.count > MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Limit is 100 requests per minute.\"}");
            return;
        }

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
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