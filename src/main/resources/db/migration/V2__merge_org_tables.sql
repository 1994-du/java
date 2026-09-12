-- 迁移脚本：将 organizations 和 departments 表的数据合并到 org_structures 表
-- 执行前请先备份数据

-- 1. 给 org_structures 表增加新字段（如果不存在）
ALTER TABLE org_structures ADD COLUMN IF NOT EXISTS type INT DEFAULT 1 NOT NULL;
ALTER TABLE org_structures ADD COLUMN IF NOT EXISTS organization_id BIGINT;
ALTER TABLE org_structures ADD COLUMN IF NOT EXISTS sort INT;

-- 2. 迁移 organizations 表数据到 org_structures（type=1 表示组织）
INSERT INTO org_structures (name, description, parent_id, code, status, type, level, is_leaf, created_at, updated_at)
SELECT 
    o.name,
    o.description,
    o.parent_id,
    o.code,
    o.status,
    1 AS type,
    COALESCE(
        (SELECT MAX(os.level) FROM org_structures os WHERE os.id = o.parent_id) + 1,
        1
    ) AS level,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM organizations child WHERE child.parent_id = o.id
        ) THEN 0 
        ELSE 1 
    END AS is_leaf,
    NOW() AS created_at,
    NOW() AS updated_at
FROM organizations o
WHERE NOT EXISTS (
    SELECT 1 FROM org_structures os 
    WHERE os.code = o.code AND os.type = 1
);

-- 3. 迁移 departments 表数据到 org_structures（type=2 表示部门）
INSERT INTO org_structures (name, description, parent_id, code, status, type, organization_id, level, is_leaf, created_at, updated_at)
SELECT 
    d.name,
    d.description,
    d.parent_id,
    d.code,
    d.status,
    2 AS type,
    d.organization_id,
    COALESCE(
        (SELECT MAX(os.level) FROM org_structures os WHERE os.id = d.parent_id) + 1,
        1
    ) AS level,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM departments child WHERE child.parent_id = d.id
        ) THEN 0 
        ELSE 1 
    END AS is_leaf,
    NOW() AS created_at,
    NOW() AS updated_at
FROM departments d
WHERE NOT EXISTS (
    SELECT 1 FROM org_structures os 
    WHERE os.code = d.code AND os.type = 2
);

-- 4. 更新 is_leaf 状态（确保有子节点的不是叶子节点）
UPDATE org_structures os
SET is_leaf = 0
WHERE EXISTS (
    SELECT 1 FROM org_structures child WHERE child.parent_id = os.id
);

-- 5. 更新 level 层级（确保层级正确）
WITH RECURSIVE level_calc AS (
    SELECT id, 1 AS calc_level
    FROM org_structures
    WHERE parent_id IS NULL OR parent_id = 0
    
    UNION ALL
    
    SELECT os.id, lc.calc_level + 1
    FROM org_structures os
    INNER JOIN level_calc lc ON os.parent_id = lc.id
    WHERE lc.calc_level < 10
)
UPDATE org_structures os
SET level = (
    SELECT calc_level FROM level_calc lc WHERE lc.id = os.id
)
WHERE EXISTS (
    SELECT 1 FROM level_calc lc WHERE lc.id = os.id
);

-- 6. 创建索引（提高查询性能）
CREATE INDEX IF NOT EXISTS idx_org_structures_type ON org_structures(type);
CREATE INDEX IF NOT EXISTS idx_org_structures_organization_id ON org_structures(organization_id);
CREATE INDEX IF NOT EXISTS idx_org_structures_parent_id ON org_structures(parent_id);
CREATE INDEX IF NOT EXISTS idx_org_structures_type_parent ON org_structures(type, parent_id);
