-- RBAC 数据修复: sys_role_menu 已改为 role_id，需重新填充菜单数据
BEGIN;

-- 1. 确保 admin 用户有角色 (user_id=1, SUPER_ADMIN)
INSERT INTO sys_user_role (user_id, role_id, tenant_id)
SELECT 1, r.id, 1
FROM sys_role r
WHERE r.role_code = 'SUPER_ADMIN' AND r.tenant_id = 1
ON CONFLICT DO NOTHING;

-- 2. 确保各角色有菜单数据 (从 fix_menu.sql 迁移)
-- SUPER_ADMIN 全部菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'SUPER_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);

-- TENANT_ADMIN 除系统管理外的菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'TENANT_ADMIN'
  AND m.id != 202
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);

-- STAFF 基础菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r, sys_menu m
WHERE r.role_code = 'STAFF'
  AND m.id IN (1, 101, 3, 301, 302, 303, 4, 411, 412, 413, 421, 422, 423, 5, 511, 512, 513, 521, 522, 523)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);

COMMIT;
