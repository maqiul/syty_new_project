package com.syty.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.TenantInfo;
import com.syty.mapper.TenantInfoMapper;
import com.syty.service.TenantInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantInfoServiceImpl extends ServiceImpl<TenantInfoMapper, TenantInfo> implements TenantInfoService {

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void createTenantSchema(String tenantCode) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String schema = "tenant_" + sanitize(tenantCode);

        log.info("开始创建租户模式: schema={}", schema);

        // 1. 创建 schema (DDL 自动提交，不建议放在 @Transactional 中)
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + schema + " AUTHORIZATION CURRENT_USER");

        // 2. 创建 sys_user 表
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + schema + ".sys_user (\n" +
                "    id BIGSERIAL PRIMARY KEY,\n" +
                "    username VARCHAR(64) NOT NULL,\n" +
                "    password VARCHAR(256) NOT NULL DEFAULT '',\n" +
                "    real_name VARCHAR(128) NOT NULL DEFAULT '',\n" +
                "    role VARCHAR(32) NOT NULL DEFAULT 'USER',\n" +
                "    department VARCHAR(128) NOT NULL DEFAULT '',\n" +
                "    status SMALLINT NOT NULL DEFAULT 1,\n" +
                "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                ")");

        // 3. 创建 sys_menu 表
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + schema + ".sys_menu (\n" +
                "    id BIGSERIAL PRIMARY KEY,\n" +
                "    parent_id BIGINT NOT NULL DEFAULT 0,\n" +
                "    name VARCHAR(100) NOT NULL,\n" +
                "    path VARCHAR(200),\n" +
                "    component VARCHAR(200),\n" +
                "    icon VARCHAR(50),\n" +
                "    permission_code VARCHAR(100),\n" +
                "    sort_order INT NOT NULL DEFAULT 0,\n" +
                "    type SMALLINT NOT NULL DEFAULT 1,\n" +
                "    hidden SMALLINT NOT NULL DEFAULT 0,\n" +
                "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP\n" +
                ")");

        // 4. 创建 sys_role_menu 表
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + schema + ".sys_role_menu (\n" +
                "    id BIGSERIAL PRIMARY KEY,\n" +
                "    role_code VARCHAR(100) NOT NULL,\n" +
                "    menu_id BIGINT NOT NULL,\n" +
                "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP\n" +
                ")");

        // 5. 插入 seed 数据
        String encodedPassword = passwordEncoder.encode("admin123");

        // 5.1 租户管理员账号
        jdbc.update("INSERT INTO " + schema + ".sys_user (username, password, real_name, role) VALUES (?, ?, ?, ?)",
                "admin", encodedPassword, "租户管理员", "TENANT_ADMIN");

        // 5.2 默认菜单（租户端基础菜单）
        // 工作台
        jdbc.update("INSERT INTO " + schema + ".sys_menu (id, parent_id, name, path, component, icon, sort_order, type, hidden) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                1L, 0L, "工作台", "/dashboard", "dashboard/Dashboard", "DashboardOutlined", 1, 1, 0);
        // 订单管理（父菜单）
        jdbc.update("INSERT INTO " + schema + ".sys_menu (id, parent_id, name, path, component, icon, sort_order, type, hidden) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                10L, 0L, "订单管理", "/order", "LAYOUT", "ShoppingCartOutlined", 10, 0, 0);
        // 开单
        jdbc.update("INSERT INTO " + schema + ".sys_menu (id, parent_id, name, path, component, icon, sort_order, type, hidden) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                11L, 10L, "新工单", "/order/create", "order/OrderCreate", "FileAddOutlined", 1, 1, 0);
        // 订单列表
        jdbc.update("INSERT INTO " + schema + ".sys_menu (id, parent_id, name, path, component, icon, sort_order, type, hidden) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                12L, 10L, "订单列表", "/order/list", "order/OrderList", "UnorderedListOutlined", 2, 1, 0);
        // 客户管理
        jdbc.update("INSERT INTO " + schema + ".sys_menu (id, parent_id, name, path, component, icon, sort_order, type, hidden) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                20L, 0L, "客户管理", "/customer", "customer/Customer", "TeamOutlined", 20, 1, 0);
        // 系统设置（父菜单）
        jdbc.update("INSERT INTO " + schema + ".sys_menu (id, parent_id, name, path, component, icon, sort_order, type, hidden) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                30L, 0L, "系统设置", "/system", "LAYOUT", "SettingOutlined", 30, 0, 0);
        // 用户管理
        jdbc.update("INSERT INTO " + schema + ".sys_menu (id, parent_id, name, path, component, icon, sort_order, type, hidden) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                31L, 30L, "用户管理", "/system/user", "system/User", "UserOutlined", 1, 1, 0);
        // 角色管理
        jdbc.update("INSERT INTO " + schema + ".sys_menu (id, parent_id, name, path, component, icon, sort_order, type, hidden) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                32L, 30L, "角色管理", "/system/role", "system/Role", "TeamOutlined", 2, 1, 0);

        // 5.3 角色菜单关联：TENANT_ADMIN 拥有所有菜单
        jdbc.update("INSERT INTO " + schema + ".sys_role_menu (role_code, menu_id) VALUES (?, ?)", "TENANT_ADMIN", 1L);
        jdbc.update("INSERT INTO " + schema + ".sys_role_menu (role_code, menu_id) VALUES (?, ?)", "TENANT_ADMIN", 10L);
        jdbc.update("INSERT INTO " + schema + ".sys_role_menu (role_code, menu_id) VALUES (?, ?)", "TENANT_ADMIN", 11L);
        jdbc.update("INSERT INTO " + schema + ".sys_role_menu (role_code, menu_id) VALUES (?, ?)", "TENANT_ADMIN", 12L);
        jdbc.update("INSERT INTO " + schema + ".sys_role_menu (role_code, menu_id) VALUES (?, ?)", "TENANT_ADMIN", 20L);
        jdbc.update("INSERT INTO " + schema + ".sys_role_menu (role_code, menu_id) VALUES (?, ?)", "TENANT_ADMIN", 30L);
        jdbc.update("INSERT INTO " + schema + ".sys_role_menu (role_code, menu_id) VALUES (?, ?)", "TENANT_ADMIN", 31L);
        jdbc.update("INSERT INTO " + schema + ".sys_role_menu (role_code, menu_id) VALUES (?, ?)", "TENANT_ADMIN", 32L);

        log.info("租户模式创建完成: schema={}, initUser=admin/admin123", schema);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dropTenantSchema(String tenantCode) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String schema = "tenant_" + sanitize(tenantCode);
        jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        log.info("租户模式已删除: schema={}", schema);
    }

    /**
     * 清理租户编码，仅保留安全字符
     */
    private String sanitize(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("租户编码不能为空");
        }
        // 只允许字母、数字、下划线
        return code.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
