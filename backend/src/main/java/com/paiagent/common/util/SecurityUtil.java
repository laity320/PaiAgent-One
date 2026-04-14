package com.paiagent.common.util;

import com.paiagent.auth.entity.User;

public class SecurityUtil {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    public static void setCurrentUser(User user) {
        CURRENT_USER.set(user);
    }

    public static User getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static Long getCurrentUserId() {
        User user = CURRENT_USER.get();
        return user != null ? user.getId() : null;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
