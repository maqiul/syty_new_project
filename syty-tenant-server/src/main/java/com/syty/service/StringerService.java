package com.syty.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.common.TenantContext;
import com.syty.dto.StringerVO;
import com.syty.entity.SysRoleEntity;
import com.syty.entity.SysUser;
import com.syty.entity.SysUserRoleEntity;
import com.syty.mapper.SysUserMapper;
import com.syty.mapper.SysRoleMapper;
import com.syty.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 穿线师用户服务
 * <p>
 * 查询拥有 STRINGER 角色的用户列表
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StringerService {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 获取所有穿线师用户列表
     *
     * @return 穿线师用户列表
     */
    public List<StringerVO> getStringers() {
        Long tenantId = TenantContext.getTenantId();

        // Step 1: 查询 STRINGER 角色的 ID
        LambdaQueryWrapper<SysRoleEntity> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysRoleEntity::getTenantId, tenantId)
                   .eq(SysRoleEntity::getRoleCode, "STRINGER")
                   .eq(SysRoleEntity::getStatus, 1);
        SysRoleEntity stringerRole = sysRoleMapper.selectOne(roleWrapper);

        if (stringerRole == null) {
            log.warn("未找到 STRINGER 角色, tenantId={}", tenantId);
            return List.of();
        }

        // Step 2: 查询拥有该角色的用户ID列表
        LambdaQueryWrapper<SysUserRoleEntity> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SysUserRoleEntity::getTenantId, tenantId)
                 .eq(SysUserRoleEntity::getRoleId, stringerRole.getId());
        List<SysUserRoleEntity> userRoles = sysUserRoleMapper.selectList(urWrapper);

        if (userRoles.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = userRoles.stream()
                .map(SysUserRoleEntity::getUserId)
                .collect(Collectors.toList());

        // Step 3: 查询用户详情
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(SysUser::getId, userIds)
                   .eq(SysUser::getStatus, 1);
        List<SysUser> users = sysUserMapper.selectList(userWrapper);

        return users.stream()
                .map(user -> StringerVO.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .phone(user.getPhone())
                        .build())
                .collect(Collectors.toList());
    }
}
