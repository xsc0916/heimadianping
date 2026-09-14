package org.xc.campus.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.xc.campus.utils.UserHolder;

/**
 * 拦截器②：真正的门卫。只拦需要登录的路径（在 WebMvcConfig 里配置）。
 * 自己不解析 token，只检查①号装进口袋里有没有人 —— 职责单一。
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (UserHolder.getUser() == null) {
            response.setStatus(401); // 未登录，请求不再往后走
            return false;
        }
        return true;
    }
}
