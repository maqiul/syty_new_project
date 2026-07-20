package com.syty.config;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/**
 * Sa-Token 配置
 *
 * 注册 Sa-Token 路由拦截器, 对所有 /api/** 路径进行鉴权:
 * - 健康检查等公开接口放行
 * - 需要登录的接口统一校验
 *
 * 注意: 拦截器注册顺序决定执行顺序.
 * WebMvcConfig 中的租户拦截器先注册, 所以先执行.
 * 本类的 SaInterceptor 后注册, 后执行 (此时租户上下文已就绪).
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Sa-Token 鉴权拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
                    // 所有请求都必须登录 (除公开接口)
                    SaRouter.match("/api/**")
                            // 公开接口放行
                            .notMatch("/api/health", "/api/auth/**", 
                                      "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**",
                                      "/doc.html", "/webjars/**")
                            .check(r -> StpUtil.checkLogin());
                }))
                .addPathPatterns("/api/**");
    }
}
