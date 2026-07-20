-- =============================================================================
-- V1.9 租户认证架构 DDL
-- 数据库: PostgreSQL 12+
-- 作者:   老K (架构师)
-- 日期:   2026-05-12
-- 描述:   多租户认证体系 — 全局认证索引 + 租户独立用户表
-- =============================================================================

-- =============================================================================
-- 一、public schema: 全局认证索引表
-- 用途: 存储所有租户管理员 (TENANT_ADMIN) 的登录凭证
-- 特点: 跨租户唯一, 登录网关统一查询此表做认证路由
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.sys_user_auth_index (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(64)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    tenant_code     VARCHAR(50)     NOT NULL,
    role_type       VARCHAR(32)     NOT NULL DEFAULT 'TENANT_ADMIN',
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- 约束: role_type 限定为管理员角色
    CONSTRAINT chk_auth_index_role CHECK (role_type IN ('TENANT_ADMIN', 'SUPER_ADMIN'))
);

-- 索引: 按租户编码反查该租户下的所有管理员
CREATE INDEX IF NOT EXISTS idx_auth_index_tenant_code
    ON public.sys_user_auth_index (tenant_code);

-- 索引: 状态过滤 (登录时排除禁用账号)
CREATE INDEX IF NOT EXISTS idx_auth_index_status
    ON public.sys_user_auth_index (status);

-- 注释
COMMENT ON TABLE public.sys_user_auth_index IS
    '全局认证索引表 — 存储所有租户管理员的登录凭证, 属于 public schema, 跨租户共享';
COMMENT ON COLUMN public.sys_user_auth_index.id IS '主键 ID (自增)';
COMMENT ON COLUMN public.sys_user_auth_index.username IS '登录用户名 (全局唯一)';
COMMENT ON COLUMN public.sys_user_auth_index.password_hash IS '密码哈希 (BCrypt/Argon2)';
COMMENT ON COLUMN public.sys_user_auth_index.tenant_code IS '所属租户编码, 用于认证后路由到对应 schema';
COMMENT ON COLUMN public.sys_user_auth_index.role_type IS '角色类型: TENANT_ADMIN=租户管理员, SUPER_ADMIN=超级管理员';
COMMENT ON COLUMN public.sys_user_auth_index.status IS '状态: 1=启用, 0=禁用';
COMMENT ON COLUMN public.sys_user_auth_index.created_at IS '创建时间';
COMMENT ON COLUMN public.sys_user_auth_index.updated_at IS '更新时间';


-- =============================================================================
-- 二、租户 Schema 模板: sys_user (租户内部用户表)
-- 用途: 存储租户内部的普通员工 / 穿线师等业务用户
-- 执行: 在创建每个租户 schema (tenant_xxx) 后, 在此 schema 下运行此 DDL
-- =============================================================================

-- 说明: 以下为模板 SQL, 实际部署时将 schema 名替换为具体的 tenant_xxx
-- 示例: SET search_path TO tenant_001; 然后执行建表语句

CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(64)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    real_name       VARCHAR(50),
    role_type       VARCHAR(32)     NOT NULL DEFAULT 'STAFF',
    status          SMALLINT        NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- 约束: role_type 限定为租户内部角色
    CONSTRAINT chk_tenant_user_role CHECK (
        role_type IN ('STAFF', 'THREADER', 'TENANT_ADMIN')
    )
);

-- 索引: 按角色类型查询 (如查所有穿线师)
CREATE INDEX IF NOT EXISTS idx_tenant_user_role_type
    ON sys_user (role_type);

-- 索引: 状态过滤
CREATE INDEX IF NOT EXISTS idx_tenant_user_status
    ON sys_user (status);

-- 注释
COMMENT ON TABLE sys_user IS
    '租户用户表 — 存储租户内部员工/穿线师等业务用户, 属于租户独立 schema (tenant_xxx)';
COMMENT ON COLUMN sys_user.id IS '主键 ID (自增)';
COMMENT ON COLUMN sys_user.username IS '登录用户名 (租户内唯一)';
COMMENT ON COLUMN sys_user.password_hash IS '密码哈希 (BCrypt/Argon2)';
COMMENT ON COLUMN sys_user.real_name IS '真实姓名';
COMMENT ON COLUMN sys_user.role_type IS '角色类型: STAFF=普通员工, THREADER=穿线师, TENANT_ADMIN=租户管理员';
COMMENT ON COLUMN sys_user.status IS '状态: 1=启用, 0=禁用';
COMMENT ON COLUMN sys_user.created_at IS '创建时间';
COMMENT ON COLUMN sys_user.updated_at IS '更新时间';


-- =============================================================================
-- 三、示例数据 (可选)
-- =============================================================================

-- 3.1 在 public.sys_user_auth_index 中插入一个租户管理员
-- 密码哈希示例为 BCrypt 编码的 "Admin@123" (生产环境请用真实哈希)
INSERT INTO public.sys_user_auth_index (username, password_hash, tenant_code, role_type, status)
VALUES (
    'admin_demo',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- BCrypt("Admin@123")
    'tenant_demo',
    'TENANT_ADMIN',
    1
) ON CONFLICT (username) DO NOTHING;

-- 3.2 在租户 schema 中插入一个穿线师 (需在对应 schema 下执行)
-- 示例: SET search_path TO tenant_demo;
-- INSERT INTO sys_user (username, password_hash, real_name, role_type, status)
-- VALUES (
--     'threader_001',
--     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
--     '张穿线',
--     'THREADER',
--     1
-- ) ON CONFLICT (username) DO NOTHING;


-- =============================================================================
-- 四、自动化脚本: 为新租户创建 sys_user 表
-- 用途: 在租户初始化流程中调用此函数, 自动在 tenant_xxx schema 下建表
-- =============================================================================

CREATE OR REPLACE FUNCTION public.init_tenant_schema(p_tenant_code VARCHAR)
RETURNS VOID AS $$
DECLARE
    v_schema_name TEXT := 'tenant_' || p_tenant_code;
BEGIN
    -- 1. 创建租户 schema
    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', v_schema_name);

    -- 2. 在租户 schema 下创建 sys_user 表
    EXECUTE format($sql$
        CREATE TABLE IF NOT EXISTS %I.sys_user (
            id              BIGSERIAL       PRIMARY KEY,
            username        VARCHAR(64)     NOT NULL UNIQUE,
            password_hash   VARCHAR(255)    NOT NULL,
            real_name       VARCHAR(50),
            role_type       VARCHAR(32)     NOT NULL DEFAULT 'STAFF',
            status          SMALLINT        NOT NULL DEFAULT 1,
            created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
            CONSTRAINT chk_tenant_user_role_%s CHECK (
                role_type IN ('STAFF', 'THREADER', 'TENANT_ADMIN')
            )
        )
    $sql$, v_schema_name, v_schema_name);

    -- 3. 创建索引
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_user_role_type ON %I.sys_user (role_type)',
                   v_schema_name, v_schema_name);
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_user_status ON %I.sys_user (status)',
                   v_schema_name, v_schema_name);

    RAISE NOTICE 'Tenant schema "%" initialized successfully.', v_schema_name;
END;
$$ LANGUAGE plpgsql;

-- 使用示例:
-- SELECT public.init_tenant_schema('demo');  -- 创建 tenant_demo schema 及 sys_user 表
