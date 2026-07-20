package com.syty.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.syty.entity.TenantInfo;

public interface TenantInfoService extends IService<TenantInfo> {
    
    /**
     * 创建租户专属数据库模式 (Schema Isolation)
     * 自动初始化 schema、业务表、种子数据
     *
     * @param tenantCode 租户编码，例如 "syty"
     */
    void createTenantSchema(String tenantCode);

    /**
     * 删除租户专属数据库模式
     *
     * @param tenantCode 租户编码
     */
    void dropTenantSchema(String tenantCode);
}
