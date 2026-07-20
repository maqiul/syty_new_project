-- 解决多租户同名问题：增加 (tenant_id, username) 联合唯一索引
-- 注意：执行前请确保没有脏数据（相同 tenant_id + username 的记录）

-- 1. 清理可能存在的重复数据（保留 ID 最小的那条）
DELETE FROM sys_user 
WHERE id NOT IN (
    SELECT min_id FROM (
        SELECT MIN(id) as min_id 
        FROM sys_user 
        WHERE deleted = 0 
        GROUP BY tenant_id, username
    ) as tmp
);

-- 2. 增加联合唯一索引
CREATE UNIQUE INDEX uk_user_tenant_username 
ON sys_user(tenant_id, username) 
WHERE deleted = 0;
