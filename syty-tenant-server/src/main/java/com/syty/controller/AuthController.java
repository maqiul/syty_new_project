package com.syty.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.syty.common.BizException;
import com.syty.config.TenantContextHolder;
import com.syty.dto.ApiResult;
import com.syty.dto.LoginRequest;
import com.syty.dto.LoginResponse;
import com.syty.entity.SysUser;
import com.syty.entity.SysUserAuthIndex;
import com.syty.mapper.SysMenuMapper;
import com.syty.mapper.SysRoleMapper;
import com.syty.mapper.SysUserAuthIndexMapper;
import com.syty.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 认证控制器 (V1.9 多租户版)
 *
 * 逻辑：
 * 1. 若请求头无 X-Tenant-Code -> 视为管理员登录 (查 public 表)
 * 2. 若请求头有 X-Tenant-Code -> 视为员工登录 (查 tenant_xxx 表)
 */
@Slf4j
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
@Tag(name = "租户认证管理", description = "门店/租户登录、登出")
public class AuthController {

    private final SysUserMapper sysUserMapper;
    private final SysUserAuthIndexMapper sysUserAuthIndexMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "管理员或员工登录")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // 获取租户编码
        String tenantCode = httpRequest.getHeader("X-Tenant-Code");

        // 分支 A：管理员登录 (没有传 tenantCode)
        if (tenantCode == null || tenantCode.isBlank()) {
            return loginAsAdmin(request);
        }

        // 分支 B：员工登录 (传了 tenantCode)
        return loginAsStaff(request, tenantCode);
    }

    /**
     * 管理员登录 (查 public 表)
     */
    private ApiResult<LoginResponse> loginAsAdmin(LoginRequest request) {
        // 1. 确保上下文为空，默认查 public
        TenantContextHolder.clear();

        // 2. 查询认证索引
        SysUserAuthIndex adminIndex = sysUserAuthIndexMapper.selectByUsername(request.getUsername());
        if (adminIndex == null) {
            log.warn("管理员登录失败: 账号不存在, username={}", request.getUsername());
            throw new BizException(400, "用户名或密码错误");
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), adminIndex.getPassword())) {
            log.warn("管理员登录失败: 密码错误, username={}", request.getUsername());
            throw new BizException(400, "用户名或密码错误");
        }

        // 4. 校验状态
        if (adminIndex.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用");
        }

        // 5. 登录成功，设置上下文
        TenantContextHolder.setTenantCode(adminIndex.getTenantCode());
        StpUtil.login(adminIndex.getId());
        
        // V2.0: 异步更新登录时间
        String code = adminIndex.getTenantCode();
        CompletableFuture.runAsync(() -> {
            try {
                // 更新 public.tenant
                jdbcTemplate.execute("UPDATE public.tenant SET last_login_at = NOW() WHERE code = '" + code + "'");
                // 更新 public.tenant_metrics (Upsert)
                jdbcTemplate.execute("INSERT INTO public.tenant_metrics (tenant_id, last_login_at) " +
                        "SELECT id, NOW() FROM public.tenant WHERE code = '" + code + "' " +
                        "ON CONFLICT (tenant_id) DO UPDATE SET last_login_at = EXCLUDED.last_login_at, is_zombie = FALSE");
            } catch (Exception e) {
                log.error("Failed to update login metrics for {}", code, e);
            }
        });

        // 存入 Session，后续请求可复用
        StpUtil.getTokenSession().set("tenantCode", adminIndex.getTenantCode());
        StpUtil.getTokenSession().set("roleType", "TENANT_ADMIN");

        log.info("管理员登录成功: username={}, tenantCode={}", request.getUsername(), adminIndex.getTenantCode());
        
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return ApiResult.success(new LoginResponse(tokenInfo.getTokenValue(), tokenInfo.getTokenName(), adminIndex.getId(), adminIndex.getUsername()));
    }

    /**
     * 员工登录 (查 tenant_xxx 表)
     */
    private ApiResult<LoginResponse> loginAsStaff(LoginRequest request, String tenantCode) {
        // 1. 设置上下文，切换 search_path
        TenantContextHolder.setTenantCode(tenantCode);

        // 2. 查询员工
        // 使用 selectByUsernameInTenant，它会依赖 Context 路由到 tenant_xxx
        SysUser staff = sysUserMapper.selectByUsernameInTenant(request.getUsername());
        
        if (staff == null) {
            log.warn("员工登录失败: 账号不存在, username={}, tenantCode={}", request.getUsername(), tenantCode);
            throw new BizException(400, "用户名或密码错误");
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), staff.getPassword())) {
            log.warn("员工登录失败: 密码错误, username={}, tenantCode={}", request.getUsername(), tenantCode);
            throw new BizException(400, "用户名或密码错误");
        }

        // 4. 校验状态
        if (staff.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用");
        }

        // 5. 登录成功
        StpUtil.login(staff.getId());
        
        // V2.0: 异步更新登录时间 (复用 TenantCode)
        String code = tenantCode;
        CompletableFuture.runAsync(() -> {
            try {
                jdbcTemplate.execute("UPDATE public.tenant SET last_login_at = NOW() WHERE code = '" + code + "'");
                jdbcTemplate.execute("INSERT INTO public.tenant_metrics (tenant_id, last_login_at) " +
                        "SELECT id, NOW() FROM public.tenant WHERE code = '" + code + "' " +
                        "ON CONFLICT (tenant_id) DO UPDATE SET last_login_at = EXCLUDED.last_login_at, is_zombie = FALSE");
            } catch (Exception e) {
                log.error("Failed to update login metrics for {}", code, e);
            }
        });

        StpUtil.getTokenSession().set("tenantCode", tenantCode);
        StpUtil.getTokenSession().set("roleType", "STAFF");

        log.info("员工登录成功: username={}, tenantCode={}", request.getUsername(), tenantCode);
        
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return ApiResult.success(new LoginResponse(tokenInfo.getTokenValue(), tokenInfo.getTokenName(), staff.getId(), staff.getUsername()));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public ApiResult<Void> logout() {
        StpUtil.logout();
        TenantContextHolder.clear();
        return ApiResult.success(null);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public ApiResult<Map<String, Object>> getUserInfo() {
        Object loginId = StpUtil.getLoginId();
        if (loginId == null) {
            throw new BizException(401, "未登录");
        }
        
        // 从 Session 中获取角色和租户信息
        String roleType = (String) StpUtil.getTokenSession().get("roleType");
        String tenantCode = (String) StpUtil.getTokenSession().get("tenantCode");
        
        Map<String, Object> info = new HashMap<>();
        info.put("userId", loginId);
        info.put("roleType", roleType);
        info.put("tenantCode", tenantCode);
        
        // 查询详细信息...
        if ("TENANT_ADMIN".equals(roleType)) {
            SysUserAuthIndex admin = sysUserAuthIndexMapper.selectById(Long.valueOf(loginId.toString()));
            if (admin != null) {
                info.put("username", admin.getUsername());
                info.put("realName", admin.getRealName());
            }
        } else {
            // 员工查 tenant schema
            if (tenantCode != null) {
                TenantContextHolder.setTenantCode(tenantCode);
                SysUser staff = sysUserMapper.selectById(Long.valueOf(loginId.toString()));
                if (staff != null) {
                    info.put("username", staff.getUsername());
                    info.put("realName", staff.getRealName());
                }
            }
        }
        
        return ApiResult.success(info);
    }
}
