package com.syty.config;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * MyBatis-Plus 配置
 *
 * V1.2 安全修复: 启用租户隔离插件
 * - print_rule 和 print_resource 已从 PUBLIC_TABLES 白名单移除
 * - 所有涉及这两张表的查询/插入/更新自动注入 tenant_id 过滤
 */
@Configuration
public class MyBatisPlusConfig {
    /**
     * 不参与租户隔离的公共表白名单
     *
     * 【安全修复】2026-05-05:
     * 之前 print_rule 和 print_resource 被错误地放入此列表，
     * 导致所有租户共享规则和素材数据，已移除!
     */
    private static final String[] PUBLIC_TABLES = {
            "printer_config"
            // print_rule   ← 已移除! 必须租户隔离
            // print_resource ← 已移除! 必须租户隔离
    };
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // ✅ V1.9 架构修复：平台端禁用行级租户隔离插件。
        // 平台端管理全局数据 (public schema)，不应自动注入 tenant_id 过滤。
        // 租户端 (syty-tenant-server) 应保留此插件。
        
        // 1. 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
