package com.syty.config;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
/**
 * MyBatis-Plus 自动填充处理器
 *
 * V1.2 变更: 新增 tenantId 自动填充
 * - INSERT 时自动设置 tenantId (从 TenantContextHolder 获取)
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        // 时间字段自动填充
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
        // tenantId 自动填充 (从当前请求上下文获取)
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            this.strictInsertFill(metaObject, "tenantId", () -> tenantId, Long.class);
        }
    }
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
    }
}
