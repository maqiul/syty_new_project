-- 店铺球线应该从店铺管理页面内跳转，不在侧边栏直接出现
BEGIN;

-- 标记店铺球线菜单为隐藏（不在侧边栏显示）
UPDATE sys_menu SET hidden = 1 WHERE path = '/shop-string' OR path = '/tennis/shop-string';

-- 同时从 STAFF 和 TENANT_ADMIN 的菜单中移除（SUPER_ADMIN 保留用于管理）
-- 注意：shop-string 页面仍然存在，只是不显示在侧边栏

COMMIT;
