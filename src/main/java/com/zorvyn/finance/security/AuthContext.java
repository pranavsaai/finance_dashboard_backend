package com.zorvyn.finance.security;

/**
 * Holds the authenticated user's ID for the duration of a request.
 * Populated by AuthFilter from the X-User-Id request header.
 */
public class AuthContext {

    private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();

    public static void set(String userId) {
        currentUserId.set(userId);
    }

    public static String get() {
        return currentUserId.get();
    }

    public static void clear() {
        currentUserId.remove();
    }
}
