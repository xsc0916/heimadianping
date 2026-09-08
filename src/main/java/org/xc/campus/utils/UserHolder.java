package org.xc.campus.utils;

import org.xc.campus.dto.UserDTO;

/**
 * 当前登录用户 ThreadLocal 持有器。
 * RefreshTokenInterceptor.afterCompletion 必须 remove，防止线程池复用导致数据串号。
 */
public final class UserHolder {

    private UserHolder() {
    }

    private static final ThreadLocal<UserDTO> TL = new ThreadLocal<>();

    public static void saveUser(UserDTO user) {
        TL.set(user);
    }

    public static UserDTO getUser() {
        return TL.get();
    }

    public static Long getUserId() {
        UserDTO user = TL.get();
        return user == null ? null : user.getId();
    }

    public static void remove() {
        TL.remove();
    }
}
