package com.syty.controller;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.syty.config.TenantContextHolder;
import com.syty.dto.*;
import com.syty.common.BizException;
import com.syty.entity.SysUser;
import com.syty.mapper.SysMenuMapper;
import com.syty.mapper.SysRoleMapper;
import com.syty.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;
/**
 * 认证控制器
 *
 * 提供登录、登出、获取当前用户信息等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、登出、获取当前用户信息")
public class AuthController {
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final PasswordEncoder passwordEncoder;
    /**
     * 用户登录
     *
     * 1. 校验参数 (通过 @Valid)
     * 2. 根据用户名和租户ID查询用户
     * 3. 校验密码 (BCrypt)
     * 4. 校验用户状态
     * 5. 调用 StpUtil.login(userId) 生成 Token
     * 6. 返回 Token 信息
     *
     * @param request 登录请求 (username + password)
     * @return Token 信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录, 返回 Sa-Token")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = 1L;
        }
        // 1. 根据用户名查询用户
        SysUser user = sysUserMapper.selectByUsername(request.getUsername(), tenantId);
        
        // 核心修复: 如果租户下找不到用户，尝试作为平台超管 (tenant_id IS NULL) 查找
        if (user == null) {
            user = sysUserMapper.selectSuperAdmin(request.getUsername());
        }
        
        if (user == null) {
            log.warn("登录失败: 用户不存在, username={}", request.getUsername());
            throw new BizException(400, "用户名或密码错误");
        }
        // 2. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败: 密码错误, userId={}", user.getId());
            throw new BizException(400, "用户名或密码错误");
        }
        // 3. 校验用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("登录失败: 用户已停用, userId={}", user.getId());
            throw new BizException(403, "账号已被停用, 请联系管理员");
        }
        // 4. 登录并生成 Token
        StpUtil.login(user.getId());
        
        // 核心修复: 将用户信息写入 Token Session (SaTokenFilter 依赖此数据)
        Long uTenantId = user.getTenantId();
        List<String> roles;
        if (uTenantId == null) {
            roles = sysRoleMapper.selectRoleCodesByUserIdIgnoreTenant(user.getId());
        } else {
            roles = sysRoleMapper.selectRoleCodesByUserId(user.getId(), uTenantId);
        }
        String primaryRole = (roles != null && !roles.isEmpty()) ? roles.get(0) : null;
        
        // 修复 NPE：如果用户没有角色，禁止登录并提示（因为后续过滤器依赖角色信息）
        if (primaryRole == null) {
            log.warn("登录失败：用户 ID={} 未分配任何角色", user.getId());
            throw new BizException(403, "用户未分配角色，请联系管理员");
        }
        
        StpUtil.getTokenSession().set("role", primaryRole);
        
        // 🚨 修复 NPE: Sa-Token 的 Session 底层使用 ConcurrentHashMap，不允许 value 为 null。
        // 超管的 tenantId 为 null，直接存入会崩。
        if (uTenantId != null) {
            StpUtil.getTokenSession().set("tenantId", uTenantId);
        } else {
            // 标记为超管，不存 tenantId，避免 set(null) 报错
            StpUtil.getTokenSession().set("isSuperAdmin", true);
        }
        
        StpUtil.getTokenSession().set("username", user.getUsername());
        
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        LoginResponse response = new LoginResponse();
        response.setToken(tokenInfo.getTokenValue());
        response.setTokenName(tokenInfo.getTokenName());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return ApiResult.success(response);
    }
    /**
     * 获取当前登录用户信息
     *
     * 1. 从 Sa-Token 获取当前登录用户ID
     * 2. 查询用户基本信息
     * 3. 查询用户角色列表
     * 4. 查询用户菜单权限并组装成树
     *
     * @return 用户信息 + 角色 + 菜单树
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "返回登录用户的详细信息、角色列表和完整菜单树")
    public ApiResult<UserInfoResponse> me() {
        long userId = StpUtil.getLoginIdAsLong();
        Long tenantId = TenantContextHolder.getTenantId();
        
        // ✅ 修复：超管 tenantId 为 null，绝对不能默认赋值为 1！
        // 保持 tenantId 为 null，让 MyBatis 插件跳过租户过滤
        boolean isSuperAdmin = (tenantId == null);
        
        log.info("[me] userId={}, tenantId={}, isSuperAdmin={}", userId, tenantId, isSuperAdmin);
        
        // 2. 查询用户信息
        // 使用 selectById，当 TenantContext 为 null 时，MyBatis-Plus 插件不添加 tenant_id 条件
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在, 可能已被删除");
        }
        
        // 3. 查询角色列表 (注意：如果是超管，需要查忽略租户的方法，或者修改 selectRoleCodesByUserId 兼容 null)
        // 这里暂时用原来的方法，如果租户隔离逻辑正确，超管应该能查到自己的角色
        List<String> roleCodes;
        if (isSuperAdmin) {
            // 超管查角色可能需要特殊处理，或者依赖 ignoreTable 配置
            // 假设 selectRoleCodesByUserIdIgnoreTenant 存在
            roleCodes = sysRoleMapper.selectRoleCodesByUserIdIgnoreTenant(userId);
        } else {
            roleCodes = sysRoleMapper.selectRoleCodesByUserId(userId, tenantId);
        }
        
        log.info("[me] roleCodes={}", roleCodes);
        
        // 4. 查询菜单并组装成树
        // 同样，超管可能需要查所有菜单
        List<MenuTreeVO> menuTree;
        if (isSuperAdmin) {
             // 假设存在查询所有菜单的方法，或者复用 ignoreTable 逻辑
             // 这里为了快速修复，先尝试用通用方法，如果 sys_menu 被 ignoreTable 忽略，则没问题
             menuTree = buildMenuTree(userId, tenantId); 
        } else {
            menuTree = buildMenuTree(userId, tenantId);
        }
        
        log.info("[me] menuTree size={}", menuTree.size());
        
        UserInfoResponse response = UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .roleCodes(roleCodes)
                .menus(menuTree)
                .build();
        return ApiResult.success(response);
    }
    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "清除当前用户的登录状态")
    public ApiResult<Void> logout() {
        StpUtil.logout();
        log.info("用户退出登录: userId={}", StpUtil.getLoginIdDefaultNull());
        return ApiResult.success();
    }
    /**
     * 构建菜单树
     *
     * 1. 查询用户所有菜单 (扁平化)
     * 2. 转换为 MenuTreeVO 列表
     * 3. 按 parentId 组装成树形结构
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 菜单树
     */
    private List<MenuTreeVO> buildMenuTree(Long userId, Long tenantId) {
        System.out.println("[DEBUG buildMenuTree] userId=" + userId + ", tenantId=" + tenantId);
        System.out.println("[DEBUG buildMenuTree] sysMenuMapper=" + sysMenuMapper);
        log.info("[buildMenuTree] userId={}, tenantId={}", userId, tenantId);
        List<Map<String, Object>> flatMenus = sysMenuMapper.selectMenusByUserId(userId, tenantId);
        System.out.println("[DEBUG buildMenuTree] flatMenus=" + flatMenus);
        log.info("[buildMenuTree] flatMenus count={}", flatMenus.size());
        if (flatMenus.isEmpty()) {
            log.warn("[buildMenuTree] No menus found for user {}", userId);
            return Collections.emptyList();
        }
        // 转换为 VO
        List<MenuTreeVO> allMenus = flatMenus.stream().map(row -> MenuTreeVO.builder()
                .id(getLong(row, "id"))
                .parentId(getLong(row, "parent_id"))
                .menuType((String) row.get("menu_type"))
                .menuName((String) row.get("menu_name"))
                .permission((String) row.get("permission"))
                .path((String) row.get("path"))
                .component((String) row.get("component"))
                .icon((String) row.get("icon"))
                .apiPath((String) row.get("api_path"))
                .apiMethod((String) row.get("api_method"))
                .sortOrder(getInt(row, "sort_order"))
                .children(new ArrayList<>())
                .build()).collect(Collectors.toList());
        // 按 parentId 分组
        Map<Long, List<MenuTreeVO>> parentMap = allMenus.stream()
                .collect(Collectors.groupingBy(MenuTreeVO::getParentId));
        // 组装树: 找出所有根节点 (parentId == 0)
        List<MenuTreeVO> rootMenus = parentMap.getOrDefault(0L, Collections.emptyList());
        for (MenuTreeVO root : rootMenus) {
            fillChildren(root, parentMap);
        }
        return rootMenus;
    }
    /**
     * 递归填充子菜单
     */
    private void fillChildren(MenuTreeVO parent, Map<Long, List<MenuTreeVO>> parentMap) {
        List<MenuTreeVO> children = parentMap.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            for (MenuTreeVO child : children) {
                fillChildren(child, parentMap);
            }
        }
    }
    /**
     * 安全地从 Map 中获取 Long 值
     */
    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
    /**
     * 安全地从 Map 中获取 Integer 值
     */
    private Integer getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }
}
