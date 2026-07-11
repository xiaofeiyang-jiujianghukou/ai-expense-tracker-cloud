package com.example.expense.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility to get current user ID from Spring Security context.
 * Use this instead of declaring {@code Authentication auth} in every controller method.
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("No authenticated user");
        }
        return (Long) auth.getPrincipal();
    }
}
