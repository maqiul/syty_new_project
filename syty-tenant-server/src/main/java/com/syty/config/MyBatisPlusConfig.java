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
        // 1. 租户隔离插件 (优先级最高)
        interceptor.addInnerInterceptor(tenantLineInnerInterceptor());
        // 2. 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
    /**
     * 租户行级隔离
     *
     * 机制:
     * - INSERT: 自动注入 tenant_id = 当前租户
     * - SELECT/UPDATE/DELETE: 自动追加 WHERE tenant_id = 当前租户
     * - PUBLIC_TABLES 中的表不受此限制
     */
    @Bean
    public TenantLineInnerInterceptor tenantLineInnerInterceptor() {
        return new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                // 从 ThreadLocal 获取当前租户 ID
                Long tenantId = TenantContextHolder.getTenantId();
                if (tenantId == null) {
                    // 未登录/未设置租户时，使用默认租户 1
                    return new LongValue(1L);
                }
                return new LongValue(tenantId);
            }
            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }
            @Override
            public boolean ignoreTable(String tableName) {
                for (String publicTable : PUBLIC_TABLES) {
                    if (publicTable.equalsIgnoreCase(tableName)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
