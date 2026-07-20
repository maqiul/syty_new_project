-- 创建租户套餐表 (public schema)
CREATE TABLE IF NOT EXISTS public.package_info (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,              -- 套餐名称
    price DECIMAL(10, 2) DEFAULT 0.00,       -- 价格
    max_tenants INT DEFAULT 0,               -- 最大租户数限制
    duration_days INT DEFAULT 365,           -- 有效期天数
    features TEXT,                           -- 特性描述 (JSON)
    status INT DEFAULT 1,                    -- 1:启用, 0:停用
    deleted INT DEFAULT 0,                   -- 逻辑删除
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 插入一条默认数据
INSERT INTO public.package_info (name, price, duration_days, status) 
VALUES ('基础版', 99.00, 365, 1)
ON CONFLICT DO NOTHING;
