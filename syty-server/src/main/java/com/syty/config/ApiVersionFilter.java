package com.syty.config;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
/**
 * API 版本控制过滤器
 * 支持 /api/v1/xxx 自动映射到/api/xxx
 * 这样以后到/api/v2/ 时不会破坏现有客户端
 */
@Component
@Order(-200)
public class ApiVersionFilter implements Filter {
    private static final String V1_PREFIX = "/api/v1/";
    private static final String API_PREFIX = "/api/";
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        // /api/v1/xxx 去掉 v1/ 前缀，当作/api/xxx 处理
        if (uri.startsWith(V1_PREFIX)) {
            String newUri = API_PREFIX + uri.substring(V1_PREFIX.length());
            chain.doFilter(new VersionedRequestWrapper(httpRequest, newUri), response);
            return;
        }
        chain.doFilter(request, response);
    }
    /**
     * 重写请求 URI，让后续 Filter/Controller 看到去掉版本号的路径
     */
    private static class VersionedRequestWrapper extends HttpServletRequestWrapper {
        private final String newUri;
        VersionedRequestWrapper(HttpServletRequest request, String newUri) {
            super(request);
            this.newUri = newUri;
        }
        @Override
        public String getRequestURI() {
            return newUri;
        }
        @Override
        public String getServletPath() {
            // 去掉 /api/ 前缀后的部分
            String path = super.getServletPath();
            if (path.startsWith("/v1/")) {
                return path.substring(4);
            }
            return path;
        }
    }
}
