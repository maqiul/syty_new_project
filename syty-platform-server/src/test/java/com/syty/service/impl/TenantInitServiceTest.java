package com.syty.service.impl;

import com.syty.service.TenantInitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TenantInitService 单元测试
 * 测试租户初始化的核心逻辑：校验、Schema 创建、种子数据插入
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("租户初始化服务测试")
class TenantInitServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private TenantInitServiceImpl tenantInitService;

    @BeforeEach
    void setUp() throws Exception {
        // 手动注入 DataSource
        ReflectionTestUtils.setField(tenantInitService, "dataSource", dataSource);
    }

    @Test
    @DisplayName("租户编码校验 - 合法编码通过")
    void validateTenantCode_ValidCode_Pass() {
        // Given - 使用反射调用私有方法
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = TenantInitServiceImpl.class.getDeclaredMethod(
                "validateTenantCode", String.class);
            method.setAccessible(true);
            method.invoke(tenantInitService, "tenant_001");
        });
    }

    @Test
    @DisplayName("租户编码校验 - 空编码抛出异常")
    void validateTenantCode_EmptyCode_ThrowsException() {
        // When & Then
        assertThrows(Exception.class, () -> {
            java.lang.reflect.Method method = TenantInitServiceImpl.class.getDeclaredMethod(
                "validateTenantCode", String.class);
            method.setAccessible(true);
            try {
                method.invoke(tenantInitService, "");
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    @DisplayName("租户编码校验 - 包含特殊字符抛出异常")
    void validateTenantCode_InvalidChars_ThrowsException() {
        // When & Then
        assertThrows(Exception.class, () -> {
            java.lang.reflect.Method method = TenantInitServiceImpl.class.getDeclaredMethod(
                "validateTenantCode", String.class);
            method.setAccessible(true);
            try {
                method.invoke(tenantInitService, "tenant-001"); // 包含连字符
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    @DisplayName("用户名校验 - 合法用户名通过")
    void validateUsername_ValidUsername_Pass() {
        // Given
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = TenantInitServiceImpl.class.getDeclaredMethod(
                "validateUsername", String.class);
            method.setAccessible(true);
            method.invoke(tenantInitService, "admin@shop.com");
        });
    }

    @Test
    @DisplayName("用户名校验 - 空用户名抛出异常")
    void validateUsername_EmptyUsername_ThrowsException() {
        // When & Then
        assertThrows(Exception.class, () -> {
            java.lang.reflect.Method method = TenantInitServiceImpl.class.getDeclaredMethod(
                "validateUsername", String.class);
            method.setAccessible(true);
            try {
                method.invoke(tenantInitService, null);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    @DisplayName("Schema 存在性检查 - Schema 存在返回 true")
    void schemaExists_Exists_ReturnsTrue() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(
            mock(java.sql.PreparedStatement.class));
        
        java.sql.PreparedStatement ps = connection.prepareStatement(anyString());
        when(ps.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        // When
        boolean exists = tenantInitService.schemaExists("test_tenant");

        // Then
        assertTrue(exists);
    }

    @Test
    @DisplayName("Schema 存在性检查 - Schema 不存在返回 false")
    void schemaExists_NotExists_ReturnsFalse() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(
            mock(java.sql.PreparedStatement.class));
        
        java.sql.PreparedStatement ps = connection.prepareStatement(anyString());
        when(ps.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        // When
        boolean exists = tenantInitService.schemaExists("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    @DisplayName("清理租户 - Schema 不存在返回 false")
    void cleanupTenant_SchemaNotExists_ReturnsFalse() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(
            mock(java.sql.PreparedStatement.class));
        
        java.sql.PreparedStatement ps = connection.prepareStatement(anyString());
        when(ps.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        // When
        boolean cleaned = tenantInitService.cleanupTenant("nonexistent");

        // Then
        assertFalse(cleaned);
    }

    @Test
    @DisplayName("TenantInitException - 包含租户编码和回滚状态")
    void tenantInitException_ContainsInfo() {
        // Given
        String tenantCode = "test_tenant";
        String message = "初始化失败";
        Exception cause = new RuntimeException("数据库错误");
        boolean rollbackSucceeded = true;

        // When
        TenantInitService.TenantInitException exception = new TenantInitService.TenantInitException(
            tenantCode, message, cause, rollbackSucceeded);

        // Then
        assertEquals(tenantCode, exception.getTenantCode());
        assertTrue(exception.isRollbackSucceeded());
        assertTrue(exception.getMessage().contains(tenantCode));
        assertTrue(exception.getMessage().contains(message));
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("DDL 模板路径 - 包含 V1.9 和 V2.0")
    void ddlTemplatePaths_ContainsRequiredFiles() throws Exception {
        // Given - 使用反射获取常量
        java.lang.reflect.Field field = TenantInitServiceImpl.class.getDeclaredField("DDL_TEMPLATE_PATHS");
        field.setAccessible(true);
        String[] paths = (String[]) field.get(null);

        // Then
        assertNotNull(paths);
        assertTrue(paths.length >= 2);
        assertEquals("sql/tenant/V1.9_tenant_template.sql", paths[0]);
        assertEquals("sql/tenant/V2.0_supplier.sql", paths[1]);
    }

    @Test
    @DisplayName("默认管理员密码 - 不为空")
    void defaultAdminPassword_NotEmpty() throws Exception {
        // Given - 使用反射获取常量
        java.lang.reflect.Field field = TenantInitServiceImpl.class.getDeclaredField("DEFAULT_ADMIN_PASSWORD");
        field.setAccessible(true);
        String password = (String) field.get(null);

        // Then
        assertNotNull(password);
        assertFalse(password.isEmpty());
        assertEquals("Admin@123", password);
    }
}
