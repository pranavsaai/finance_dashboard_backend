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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {

            String ip = request.getRemoteAddr();
            long now = System.currentTimeMillis();

            // Probabilistic stale-entry pruning (~0.1% of requests).
            // Keeps memory bounded without a dedicated cleanup thread.
            if (Math.random() < 0.001) {
                requestCount.entrySet().removeIf(e -> now - e.getValue()[1] > STALE_THRESHOLD_MS);
            }

            // Atomic read-modify-write via compute() prevents the race condition where two
            // concurrent requests from the same IP both read count=0, both increment to 1,
            // and both bypass the limit. long[]: [0] = request count, [1] = window start time.
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
                    // Reject refresh tokens presented as Bearer access tokens.
                    // Without this check, a refresh token passes signature validation,
                    // extractRole() returns null, and the SecurityContext is set with
                    // ROLE_null — wrong behavior even though @PreAuthorize would eventually
                    // reject the request downstream.
                    if (jwtUtil.isRefreshToken(token)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Refresh tokens cannot be used as access tokens\"}");
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

            chain.doFilter(request, response);

        } finally {
            // Always clear the ThreadLocal — prevents userId from leaking into the next
            // request if the thread is reused from the servlet container pool.
            AuthContext.clear();
        }
    }
}