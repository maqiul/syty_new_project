-- ============================================================
-- 修正菜单结构：店铺独立，羽毛球/网球只含运动数据
-- ============================================================

BEGIN;

-- 清空旧菜单数据
DELETE FROM sys_role_menu;
DELETE FROM sys_menu;

-- ===== 根目录 =====
INSERT INTO sys_menu (id, parent_id, name, icon, sort_order, type) VALUES
(1,  0, '工作台',     'DashboardOutlined',    1,  0),
(2,  0, '系统管理',   'SettingOutlined',      10, 0),
(3,  0, '🏪 店铺',    'ShoppingOutlined',     20, 0),
(4,  0, '🏸 羽毛球',  'AppstoreOutlined',     30, 0),
(5,  0, '🎾 网球',    'TrophyOutlined',       40, 0);

-- ===== 工作台 =====
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, permission_code, sort_order, type) VALUES
(101, 1, '工作台首页', '/dashboard', 'Dashboard', 'DashboardOutlined', 'dashboard:view', 1, 1);

-- ===== 系统管理 =====
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, permission_code, sort_order, type) VALUES
(201, 2, '用户管理',   '/user',           'User',           'TeamOutlined',          'user:page',              1, 1),
(202, 2, '租户管理',   '/tenant',         'Tenant',         'SafetyOutlined',        'tenant:page',            2, 1),
(203, 2, '操作日志',   '/operation-log',  'OperationLog',   'AuditOutlined',         'operate-log:view',       3, 1),
(204, 2, '打印模板',   '/print-template', 'PrintTemplate',  'PrinterOutlined',       'print-template:manage',  4, 1),
(205, 2, '菜单管理',   '/system/menu',    'MenuManage',     'MenuOutlined',          'system:menu:manage',     5, 1),
(206, 2, '权限管理',   '/system/perm',    'PermManage',     'LockOutlined',          'system:perm:manage',     6, 1);

-- ===== 🏪 店铺（跨运动共用） =====
INSERT INTO sys_menu (id, parent_id, name, path, component, permission_code, sort_order, type) VALUES
(301, 3, '店铺管理',     '/shop',              'Shop',              'shop:page',              1, 1),
(302, 3, '🏸 店铺球线',  '/shop-string',       'ShopString',        'shop-string:page',       2, 1),
(303, 3, '🎾 店铺球线',  '/tennis/shop-string', 'TennisShopString',  'tennis-shop-string:page', 3, 1);

-- ===== 🏸 羽毛球 =====
-- 基础数据
INSERT INTO sys_menu (id, parent_id, name, path, component, permission_code, sort_order, type) VALUES
(411, 4, '球员管理', '/player',      'Player',         'player:page',      1, 1),
(412, 4, '球拍管理', '/racket',      'Racket',         'racket:page',      2, 1),
(413, 4, '球线管理', '/string-info', 'StringInfo',     'string:page',      3, 1);

-- 订单大赛
INSERT INTO sys_menu (id, parent_id, name, path, component, permission_code, sort_order, type) VALUES
(421, 4, '穿线订单', '/order',                    'Order',                    'order:page',         1, 1),
(422, 4, '大赛登记', '/tournament',               'BadmintonTournament',      'tournament:create',  2, 1),
(423, 4, '大赛记录', '/badminton-tournament-list','BadmintonTournamentList',  'tournament:page',    3, 1);

-- ===== 🎾 网球 =====
-- 基础数据
INSERT INTO sys_menu (id, parent_id, name, path, component, permission_code, sort_order, type) VALUES
(511, 5, '球员管理', '/tennis/player', 'TennisPlayer',     'tennis-player:page',      1, 1),
(512, 5, '球拍管理', '/tennis/racket', 'TennisRacket',     'tennis-racket:page',      2, 1),
(513, 5, '球线管理', '/tennis/string', 'TennisString',     'tennis-string:page',      3, 1);

-- 订单大赛
INSERT INTO sys_menu (id, parent_id, name, path, component, permission_code, sort_order, type) VALUES
(521, 5, '穿线订单',  '/tennis/order',           'TennisOrder',          'tennis-order:page',          1, 1),
(522, 5, '大赛登记',  '/tennis/tournament',      'TennisTournament',     'tennis-tournament:create',   2, 1),
(523, 5, '大赛记录',  '/tennis/tournament/list', 'TennisTournamentList', 'tennis-tournament:page',     3, 1);

-- ===== 重建角色-菜单关联 =====
INSERT INTO sys_role_menu (role_code, menu_id)
SELECT 'SUPER_ADMIN', id FROM sys_menu;

INSERT INTO sys_role_menu (role_code, menu_id)
SELECT 'TENANT_ADMIN', id FROM sys_menu WHERE id != 202;

INSERT INTO sys_role_menu (role_code, menu_id) VALUES
('STAFF', 1), ('STAFF', 101),    -- 工作台
('STAFF', 3), ('STAFF', 301), ('STAFF', 302), ('STAFF', 303),  -- 店铺
('STAFF', 4), ('STAFF', 411), ('STAFF', 412), ('STAFF', 413),  -- 🏸基础
('STAFF', 421), ('STAFF', 422), ('STAFF', 423),                -- 🏸订单
('STAFF', 5), ('STAFF', 511), ('STAFF', 512), ('STAFF', 513),  -- 🎾基础
('STAFF', 521), ('STAFF', 522), ('STAFF', 523);                -- 🎾订单

COMMIT;
