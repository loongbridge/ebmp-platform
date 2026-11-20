package com.ebmp.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new BizException("未登录", 401);
        }
        return user;
    }

    public static Long currentUserId() {
        return current().getId();
    }

    public static boolean isAdmin() {
        return current().getRoles().stream().anyMatch(r -> r.equals("ADMIN") || r.equals("GM"));
    }
}
