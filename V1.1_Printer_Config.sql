-- V1.1 打印配置模块迁移脚本
-- 创建打印机配置表、规则表、资源表

-- 1. 打印机配置表 (记录每台机器的打印机列表)
CREATE TABLE IF NOT EXISTS printer_config (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    machine_id VARCHAR(64) NOT NULL, -- 客户端机器码 (GUID)
    printer_name VARCHAR(100) NOT NULL, -- 物理打印机名称
    status INT DEFAULT 1, -- 1: 在线, 0: 离线
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- 唯一约束：同一店铺、同一机器、同一打印机只能有一条记录
    CONSTRAINT uq_printer UNIQUE (shop_id, machine_id, printer_name)
);

-- 2. 打印规则表 (定义 单据类型 -> 打印机 -> 模板 的映射)
CREATE TABLE IF NOT EXISTS print_rule (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    doc_type VARCHAR(50) NOT NULL, -- 单据类型: RECEIPT(收据), TAG(标签) 等
    printer_name VARCHAR(100) NOT NULL, -- 指定使用的打印机
    template_path VARCHAR(255), -- 模板文件路径/名称
    copies INT DEFAULT 1, -- 打印份数
    is_enabled BOOLEAN DEFAULT TRUE, -- 是否启用
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rule UNIQUE (shop_id, doc_type, printer_name)
);

-- 3. 资源表 (管理 Logo、收款码等静态图片 URL)
CREATE TABLE IF NOT EXISTS print_resource (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT DEFAULT 0, -- 0 代表全局资源，其他为店铺专属
    resource_key VARCHAR(50) NOT NULL, -- 资源标识: LOGO, QR_PAY, QR_MINI
    resource_url VARCHAR(500) NOT NULL, -- 图片 URL (OSS/MinIO)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_resource UNIQUE (shop_id, resource_key)
);

COMMENT ON TABLE printer_config IS '客户端打印机配置表';
COMMENT ON TABLE print_rule IS '打印规则配置表';
COMMENT ON TABLE print_resource IS '打印静态资源表';
