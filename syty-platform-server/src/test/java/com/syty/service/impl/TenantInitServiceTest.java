package com.syty.service.impl;

import com.syty.service.TenantInitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TenantInitService 单元测试
 * 测试租户初始化的校验逻辑和常量配置
 * 
 * 注：schemaExists/cleanupTenant/initTenant 涉及 JdbcTemplate + DataSource，
 * 需要集成测试环境（H2/TestContainers），此处仅测试纯逻辑部分。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("租户初始化服务测试")
class TenantInitServiceTest {

    @InjectMocks
    private TenantInitServiceImpl tenantInitService;

    // ========================================================================
    // 租户编码校验
    // ========================================================================

    @Test
    @DisplayName("租户编码校验 - 合法编码通过（字母数字下划线）")
    void validateTenantCode_ValidCode_Pass() {
        assertDoesNotThrow(() -> invokeValidateTenantCode("tenant_001"));
        assertDoesNotThrow(() -> invokeValidateTenantCode("abc123"));
        assertDoesNotThrow(() -> invokeValidateTenantCode("ABC_DEF"));
    }

    @Test
    @DisplayName("租户编码校验 - 空编码抛出异常")
    void validateTenantCode_EmptyCode_ThrowsException() {
        Exception ex = assertThrows(IllegalArgumentException.class,
            () -> invokeValidateTenantCode(""));
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("租户编码校验 - null 编码抛出异常")
    void validateTenantCode_NullCode_ThrowsException() {
        Exception ex = assertThrows(IllegalArgumentException.class,
            () -> invokeValidateTenantCode(null));
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("租户编码校验 - 包含连字符抛出异常")
    void validateTenantCode_Hyphen_ThrowsException() {
        Exception ex = assertThrows(IllegalArgumentException.class,
            () -> invokeValidateTenantCode("tenant-001"));
        assertTrue(ex.getMessage().contains("格式非法"));
    }

    @Test
    @DisplayName("租户编码校验 - 包含空格抛出异常")
    void validateTenantCode_Space_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> invokeValidateTenantCode("tenant 001"));
    }

    @Test
    @DisplayName("租户编码校验 - 包含点号抛出异常")
    void validateTenantCode_Dot_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> invokeValidateTenantCode("tenant.001"));
    }

    // ========================================================================
    // 用户名校验
    // ========================================================================

    @Test
    @DisplayName("用户名校验 - 合法用户名通过（含 @ 和 .）")
    void validateUsername_ValidUsername_Pass() {
        assertDoesNotThrow(() -> invokeValidateUsername("admin@shop.com"));
        assertDoesNotThrow(() -> invokeValidateUsername("admin_01"));
        assertDoesNotThrow(() -> invokeValidateUsername("user.name"));
    }

    @Test
    @DisplayName("用户名校验 - null 用户名抛出异常")
    void validateUsername_NullUsername_ThrowsException() {
        Exception ex = assertThrows(IllegalArgumentException.class,
            () -> invokeValidateUsername(null));
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("用户名校验 - 空用户名抛出异常")
    void validateUsername_EmptyUsername_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
            () -> invokeValidateUsername(""));
    }

    // ========================================================================
    // 异常类测试
    // ========================================================================

    @Test
    @DisplayName("TenantInitException - 包含租户编码和回滚成功状态")
    void tenantInitException_RollbackSuccess() {
        TenantInitService.TenantInitException exception = new TenantInitService.TenantInitException(
            "test_tenant", "初始化失败", new RuntimeException("数据库错误"), true);

        assertEquals("test_tenant", exception.getTenantCode());
        assertTrue(exception.isRollbackSucceeded());
        assertTrue(exception.getMessage().contains("test_tenant"));
        assertTrue(exception.getMessage().contains("初始化失败"));
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("TenantInitException - 回滚失败标记")
    void tenantInitException_RollbackFailed() {
        TenantInitService.TenantInitException exception = new TenantInitService.TenantInitException(
            "t1", "失败", new RuntimeException(), false);

        assertFalse(exception.isRollbackSucceeded());
        assertEquals("t1", exception.getTenantCode());
    }

    // ========================================================================
    // 常量验证
    // ========================================================================

    @Test
    @DisplayName("DDL 模板路径 - 包含 V1.9 和 V2.0")
    void ddlTemplatePaths_ContainsRequiredFiles() throws Exception {
        java.lang.reflect.Field field = TenantInitServiceImpl.class.getDeclaredField("DDL_TEMPLATE_PATHS");
        field.setAccessible(true);
        String[] paths = (String[]) field.get(null);

        assertNotNull(paths);
        assertTrue(paths.length >= 2);
        assertEquals("sql/tenant/V1.9_tenant_template.sql", paths[0]);
        assertEquals("sql/tenant/V2.0_supplier.sql", paths[1]);
    }

    @Test
    @DisplayName("默认管理员密码 - 不为空且为 Admin@123")
    void defaultAdminPassword_NotEmpty() throws Exception {
        java.lang.reflect.Field field = TenantInitServiceImpl.class.getDeclaredField("DEFAULT_ADMIN_PASSWORD");
        field.setAccessible(true);
        String password = (String) field.get(null);

        assertNotNull(password);
        assertFalse(password.isEmpty());
        assertEquals("Admin@123", password);
    }

    @Test
    @DisplayName("租户编码正则 - 仅允许字母数字下划线")
    void tenantCodePattern_StrictWhitelist() throws Exception {
        java.lang.reflect.Field field = TenantInitServiceImpl.class.getDeclaredField("TENANT_CODE_PATTERN");
        field.setAccessible(true);
        java.util.regex.Pattern pattern = (java.util.regex.Pattern) field.get(null);

        assertTrue(pattern.matcher("abc").matches());
        assertTrue(pattern.matcher("ABC_123").matches());
        assertFalse(pattern.matcher("a-b").matches());
        assertFalse(pattern.matcher("a.b").matches());
        assertFalse(pattern.matcher("a b").matches());
        assertFalse(pattern.matcher("a;b").matches());
        assertFalse(pattern.matcher("").matches());
    }

    // ========================================================================
    // 辅助方法
    // ========================================================================

    private void invokeValidateTenantCode(String code) throws Throwable {
        java.lang.reflect.Method method = TenantInitServiceImpl.class.getDeclaredMethod(
            "validateTenantCode", String.class);
        method.setAccessible(true);
        try {
            method.invoke(tenantInitService, code);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private void invokeValidateUsername(String username) throws Throwable {
        java.lang.reflect.Method method = TenantInitServiceImpl.class.getDeclaredMethod(
            "validateUsername", String.class);
        method.setAccessible(true);
        try {
            method.invoke(tenantInitService, username);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
