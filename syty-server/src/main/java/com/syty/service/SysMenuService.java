package com.syty.service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.SysMenu;
import com.syty.mapper.SysMenuMapper;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class SysMenuService extends ServiceImpl<SysMenuMapper, SysMenu> {
    /**
     * 获取菜单树（全部字
     */
    public List<Map<String, Object>> getMenuTree() {
        List<SysMenu> all = baseMapper.selectAllVisible();
        return buildTree(all, 0L);
    }
    /**
     * 按角色获取菜单树
     */
    public List<Map<String, Object>> getMenuTreeByRole(String roleCode) {
        List<Long> menuIds = baseMapper.selectMenuIdsByRole(roleCode);
        if (menuIds.isEmpty()) return Collections.emptyList();
        Set<Long> idSet = new HashSet<>(menuIds);
        // 需要包含所有祖先节字
        List<SysMenu> all = baseMapper.selectAllVisible();
        Set<Long> accessible = new HashSet<>();
        for (SysMenu menu : all) {
            if (idSet.contains(menu.getId())) {
                accessible.add(menu.getId());
                // 向上追溯祖先
                addAncestors(all, menu.getParentId(), accessible);
            }
        }
        List<SysMenu> filtered = all.stream()
            .filter(m -> accessible.contains(m.getId()))
            .collect(Collectors.toList());
        return buildTree(filtered, 0L);
    }
    private void addAncestors(List<SysMenu> all, Long parentId, Set<Long> result) {
        if (parentId == 0) return;
        result.add(parentId);
        for (SysMenu m : all) {
            if (m.getId().equals(parentId)) {
                addAncestors(all, m.getParentId(), result);
                break;
            }
        }
    }
    private List<Map<String, Object>> buildTree(List<SysMenu> menus, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (!menu.getParentId().equals(parentId)) continue;
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", menu.getId());
            node.put("name", menu.getName());
            node.put("path", menu.getPath());
            node.put("component", menu.getComponent());
            node.put("icon", menu.getIcon());
            node.put("permissionCode", menu.getPermissionCode());
            node.put("type", menu.getType());
            node.put("sortOrder", menu.getSortOrder());
            List<Map<String, Object>> children = buildTree(menus, menu.getId());
            if (!children.isEmpty()) {
                node.put("children", children);
            }
            result.add(node);
        }
        result.sort(Comparator.comparingInt(m -> (Integer) m.get("sortOrder")));
        return result;
    }
}
