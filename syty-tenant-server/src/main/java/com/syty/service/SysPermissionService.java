package com.syty.service;
import com.syty.mapper.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
@Service
@RequiredArgsConstructor
public class SysPermissionService {
    private final SysPermissionMapper sysPermissionMapper;
    /**
     * 获取某个角色拥有的所有权限编字
     */
    public List<String> getPermissionCodesByRole(String roleCode) {
        if (roleCode == null || roleCode.isEmpty()) {
            return Collections.emptyList();
        }
        return sysPermissionMapper.selectCodesByRole(roleCode);
    }
    /**
     * 检查某个角色是否拥有指定权字
     */
    public boolean hasPermission(String roleCode, String permissionCode) {
        List<String> codes = getPermissionCodesByRole(roleCode);
        return codes.contains(permissionCode);
    }
}
