-- ============================================================
-- V2.1 权限码初始化
-- 为所有核心业务接口添加权限控制
-- ============================================================

-- 订单管理
INSERT INTO sys_permission (code, name, type, parent_code, sort_order) VALUES
('order:view', '查看订单', 'BUTTON', 'order', 1),
('order:create', '创建订单', 'BUTTON', 'order', 2),
('order:edit', '编辑订单', 'BUTTON', 'order', 3),
('order:delete', '删除订单', 'BUTTON', 'order', 4),
('order:complete', '完成订单', 'BUTTON', 'order', 5),
('order:print', '打印订单', 'BUTTON', 'order', 6),
('order:export', '导出订单', 'BUTTON', 'order', 7);

-- 球员管理
INSERT INTO sys_permission (code, name, type, parent_code, sort_order) VALUES
('player:view', '查看球员', 'BUTTON', 'assets', 10),
('player:create', '创建球员', 'BUTTON', 'assets', 11),
('player:edit', '编辑球员', 'BUTTON', 'assets', 12),
('player:delete', '删除球员', 'BUTTON', 'assets', 13);

-- 球拍管理
INSERT INTO sys_permission (code, name, type, parent_code, sort_order) VALUES
('racket:view', '查看球拍', 'BUTTON', 'assets', 20),
('racket:create', '创建球拍', 'BUTTON', 'assets', 21),
('racket:edit', '编辑球拍', 'BUTTON', 'assets', 22),
('racket:delete', '删除球拍', 'BUTTON', 'assets', 23);

-- 球线管理
INSERT INTO sys_permission (code, name, type, parent_code, sort_order) VALUES
('string:view', '查看球线', 'BUTTON', 'assets', 30),
('string:create', '创建球线', 'BUTTON', 'assets', 31),
('string:edit', '编辑球线', 'BUTTON', 'assets', 32),
('string:delete', '删除球线', 'BUTTON', 'assets', 33);

-- 供应商管理
INSERT INTO sys_permission (code, name, type, parent_code, sort_order) VALUES
('supplier:view', '查看供应商', 'BUTTON', 'assets', 40),
('supplier:create', '创建供应商', 'BUTTON', 'assets', 41),
('supplier:edit', '编辑供应商', 'BUTTON', 'assets', 42),
('supplier:delete', '删除供应商', 'BUTTON', 'assets', 43);

-- 库存管理
INSERT INTO sys_permission (code, name, type, parent_code, sort_order) VALUES
('stock:view', '查看库存', 'BUTTON', 'inventory', 1),
('stock:inbound', '入库操作', 'BUTTON', 'inventory', 2),
('stock:adjust', '库存调整', 'BUTTON', 'inventory', 3),
('stock:check', '库存盘点', 'BUTTON', 'inventory', 4);

-- 财务管理
INSERT INTO sys_permission (code, name, type, parent_code, sort_order) VALUES
('finance:view', '查看财务', 'BUTTON', 'finance', 1),
('finance:repay', '还款操作', 'BUTTON', 'finance', 2);

-- 提成规则
INSERT INTO sys_permission (code, name, type, parent_code, sort_order) VALUES
('commission:view', '查看提成规则', 'BUTTON', 'commission', 1),
('commission:create', '创建提成规则', 'BUTTON', 'commission', 2),
('commission:edit', '编辑提成规则', 'BUTTON', 'commission', 3),
('commission:delete', '删除提成规则', 'BUTTON', 'commission', 4);

-- 门店管理
INSERT INTO sys_permission (code, name, type, parent_code, sort_order) VALUES
('shop:view', '查看门店', 'BUTTON', 'shop', 1),
('shop:create', '创建门店', 'BUTTON', 'shop', 2),
('shop:edit', '编辑门店', 'BUTTON', 'shop', 3),
('shop:delete', '删除门店', 'BUTTON', 'shop', 4);

-- 打印管理
INSERT INTO sys_permission (code, name, type, parent_code, sort_order) VALUES
('print:template:view', '查看打印模板', 'BUTTON', 'print', 1),
('print:template:create', '创建打印模板', 'BUTTON', 'print', 2),
('print:template:edit', '编辑打印模板', 'BUTTON', 'print', 3),
('print:template:delete', '删除打印模板', 'BUTTON', 'print', 4);
