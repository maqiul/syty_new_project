-- 补丁：增加“打印配置”菜单
-- 运行: podman exec -i syty-postgres psql -U root -d syty < fix_menu.sql

BEGIN;

-- 1. 在“系统管理”(parent_id=2)下增加菜单 (ID: 207)
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, permission_code, sort_order, type) VALUES
(207, 2, '打印配置', '/print-config', 'PrintConfig', 'PrinterOutlined', 'print-config:manage', 7, 1)
ON CONFLICT (id) DO UPDATE SET name = '打印配置', path = '/print-config', permission_code = 'print-config:manage';

-- 2. 增加对应的权限记录
INSERT INTO sys_permission (code, name, menu_id, type, sort_order) VALUES
('print-config:manage', '打印配置管理', 207, 'menu', 70)
ON CONFLICT (code) DO UPDATE SET menu_id = 207;

-- 3. 给 SUPER_ADMIN 分配菜单权限 (如果之前没跑过全量同步)
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT 'SUPER_ADMIN', 207
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_code = 'SUPER_ADMIN' AND menu_id = 207);

-- 4. 给 TENANT_ADMIN 分配菜单权限
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT 'TENANT_ADMIN', 207
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_code = 'TENANT_ADMIN' AND menu_id = 207);

-- 5. 给 SUPER_ADMIN 分配权限
INSERT INTO sys_role_permission (role_code, permission_code)
SELECT 'SUPER_ADMIN', 'print-config:manage'
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_code = 'SUPER_ADMIN' AND permission_code = 'print-config:manage');

COMMIT;
