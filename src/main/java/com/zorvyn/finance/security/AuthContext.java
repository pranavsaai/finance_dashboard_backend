package com.zorvyn.finance.security;

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
