package com.zorvyn.finance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the X-User-Id header from every incoming request and stores
 * it in AuthContext for downstream use.
 *
 * This is a mock authentication mechanism but not real jwt authentication as doing locally right now and its actually suitable for local development.
 * In production this would be replaced by a proper JWT or session filter.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        AuthContext.set(userId);

        try {
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear(); // always clean up ThreadLocal after request
        }
    }
}
