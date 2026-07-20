-- 清空并重建菜单 + 角色关联 (适配 role_id)
-- 注意: DB 中 sys_menu 无 permission_code 列(已迁移到 sys_permission)
BEGIN;

DELETE FROM sys_role_menu;
DELETE FROM sys_menu;

-- ===== 根目录 =====
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, type) VALUES
(1,  0, '工作台',     '/dashboard',        'Dashboard',        'DashboardOutlined',    1,  0),
(2,  0, '系统管理',   '/system',           '',                 'SettingOutlined',      99, 0),
(3,  0, '店铺管理',   '/shop',             '',                 'ShopOutlined',         2,  0),
(4,  0, '🏸 羽毛球',  '',                  '',                 'ThunderboltOutlined',  3,  0),
(5,  0, '🎾 网球',    '',                  '',                 'TrophyOutlined',       4,  0);

-- ===== 工作台子菜单 =====
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, type) VALUES
(101, 1, '首页',       '/dashboard',       'Dashboard',        '',                     1,  1);

-- ===== 系统管理子菜单 =====
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, type) VALUES
(201, 2, '用户管理',   '/user',            'User',             '',                     1,  1),
(202, 2, '租户管理',   '/tenant',          'Tenant',           '',                     2,  1),
(203, 2, '菜单管理',   '/system/menu',     'MenuManage',       '',                     3,  1),
(204, 2, '权限管理',   '/system/perm',     'PermManage',       '',                     4,  1),
(205, 2, '操作日志',   '/operation-log',   'OperationLog',     '',                     5,  1),
(206, 2, '打印模板',   '/print-template',  'PrintTemplate',    '',                     6,  1),
(207, 2, '打印配置',   '/print-config',    'PrintConfig',      '',                     7,  1),
(208, 2, '打印日志',   '/print-logs',      'PrintLogs',        '',                     8,  1);

-- ===== 店铺管理子菜单 =====
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, type) VALUES
(301, 3, '店铺列表',   '/shop',            'Shop',             '',                     1,  1),
(302, 3, '球线库存',   '/shop-string',     'ShopStrings',      '',                     2,  1),
(303, 3, '库存管理',   '/stock',           'Stock',            '',                     3,  1),
(304, 3, '财务统计',   '/finance',         'Finance',          '',                     4,  1);

-- ===== 羽毛球子菜单 =====
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, type) VALUES
(401, 4, '球员管理',   '/player',          'Player',           '',                     1,  1),
(402, 4, '球拍管理',   '/racket',          'Racket',           '',                     2,  1),
(403, 4, '球线管理',   '/string-info',     'StringInfo',       '',                     3,  1),
(411, 4, '穿线订单',   '/order',           'Order',            '',                     4,  1),
(412, 4, '创建订单',   '/order/create',    'CreateOrder',      '',                     5,  1),
(413, 4, '大赛登记',   '/tournament',      'TournamentOrder',  '',                     6,  1),
(414, 4, '大赛记录',   '/tournament/list', 'BadmintonTournamentList', '',             7,  1);

-- ===== 网球子菜单 =====
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order, type) VALUES
(501, 5, '球员管理',   '/tennis/player',           'TennisPlayer',     '',           1,  1),
(502, 5, '球拍管理',   '/tennis/racket',           'TennisRacket',     '',           2,  1),
(503, 5, '球线管理',   '/tennis/string',           'TennisString',     '',           3,  1),
(504, 5, '球线库存',   '/tennis/shop-string',      'TennisShopString', '',           4,  1),
(511, 5, '穿线订单',   '/tennis/order',            'TennisOrder',      '',           5,  1),
(512, 5, '大赛登记',   '/tennis/tournament',       'TennisTournament', '',           6,  1),
(513, 5, '大赛记录',   '/tennis/tournament/list',  'TennisTournamentList', '',       7,  1);

-- ===== 角色-菜单关联 =====
-- SUPER_ADMIN: 全部菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- TENANT_ADMIN: 除系统管理外全部
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'TENANT_ADMIN' AND m.id NOT IN (202, 203, 204, 205)
ON CONFLICT DO NOTHING;

-- STAFF: 基础菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r, sys_menu m
WHERE r.role_code = 'STAFF'
  AND m.id IN (1, 101, 3, 301, 302, 303, 4, 401, 402, 403, 411, 412, 413, 414, 5, 501, 502, 503, 504, 511, 512, 513)
ON CONFLICT DO NOTHING;

COMMIT;
