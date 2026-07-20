package com.syty.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final TenantInterceptor tenantInterceptor;

    @Value("${print.assets.base-path:assets/print/}")
    private String assetBasePath;

    @Value("${print.assets.url-prefix:/assets/print/}")
    private String assetUrlPrefix;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射打印素材目录
        registry.addResourceHandler(assetUrlPrefix + "**")
                .addResourceLocations("file:" + assetBasePath);
    }
}
