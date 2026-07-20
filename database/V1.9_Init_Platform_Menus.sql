-- ============================================================
-- V1.9 平台端 (SuperAdmin) 初始化菜单脚本
-- 针对 public schema
-- ============================================================

BEGIN;

-- 1. 清理旧的关联 (防止重复插入报错)
DELETE FROM public.sys_role_menu WHERE role_code = 'SUPER_ADMIN';
-- 清理旧菜单 (假设 ID 1-99 为平台保留)
DELETE FROM public.sys_menu WHERE id < 100;

-- 2. 插入平台端菜单
-- 类型说明: type=0 (目录/文件夹), type=1 (页面/菜单)
-- 路径说明: 前端需要对应创建 src/views/platform/... 下的 Vue 文件
INSERT INTO public.sys_menu (id, parent_id, name, path, component, icon, type, sort_order, status, hidden) VALUES
-- [1] 工作台 (独立页面)
(1, 0, '工作台', '/dashboard', 'platform/Dashboard', 'DashboardOutlined', 1, 1, 1, 0),

-- [10] 租户管理 (目录)
(10, 0, '租户管理', '/tenant', 'LAYOUT', 'TeamOutlined', 0, 10, 1, 0),
-- [11] 租户列表
(11, 10, '租户列表', '/tenant/list', 'platform/tenant/List', 'ListOutlined', 1, 1, 1, 0),
-- [12] 租户套餐
(12, 10, '租户套餐', '/tenant/plans', 'platform/tenant/Plans', 'CreditCardOutlined', 1, 2, 1, 0),

-- [20] 系统设置 (目录)
(20, 0, '平台系统', '/system', 'LAYOUT', 'SettingOutlined', 0, 20, 1, 0),
-- [21] 平台用户
(21, 20, '平台用户', '/system/user', 'platform/system/User', 'UserOutlined', 1, 1, 1, 0),
-- [22] 平台角色
(22, 20, '平台角色', '/system/role', 'platform/system/Role', 'TeamOutlined', 1, 2, 1, 0);

-- 3. 授权给 SUPER_ADMIN 角色 (使用 role_code 关联)
INSERT INTO public.sys_role_menu (role_code, menu_id) 
SELECT 'SUPER_ADMIN', id FROM public.sys_menu;

COMMIT;
