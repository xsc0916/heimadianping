package org.xc.campus.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.xc.campus.interceptor.LoginInterceptor;
import org.xc.campus.interceptor.RefreshTokenInterceptor;

/**
 * MVC 配置：注册两个拦截器。
 * addInterceptor 的顺序 = 执行顺序，①号必须先注册。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RefreshTokenInterceptor refreshTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // ①号：拦所有路径，只认识人不拦人
        registry.addInterceptor(refreshTokenInterceptor).addPathPatterns("/**");
        // ②号：默认拦所有，但把公开接口排除 —— 名单之外才需要登录
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/send-code",
                        "/api/category/**",
                        "/api/product/**",
                        "/api/seckill/list",
                        "/api/ai/**",
                        "/error");
    }
}
