-- 添加身高字段到用户表
-- 身高单位：厘米 (cm)

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS height DECIMAL(5,2); -- 身高（厘米），支持范围 50-250cm

-- 添加注释
COMMENT ON COLUMN users.height IS '用户身高（厘米）';

