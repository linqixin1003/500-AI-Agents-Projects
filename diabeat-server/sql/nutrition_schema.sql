-- DiabEat AI - 营养成分相关表结构

-- 创建食物识别记录表
CREATE TABLE IF NOT EXISTS food_recognitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    image_url VARCHAR(500), -- 存储在 S3/OSS
    recognition_result JSONB, -- 识别结果详情
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建营养成分记录表
CREATE TABLE IF NOT EXISTS nutrition_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_recognition_id UUID REFERENCES food_recognitions(id),
    total_carbs DECIMAL(6,2) NOT NULL, -- 总碳水化合物 (g)
    net_carbs DECIMAL(6,2) NOT NULL, -- 净碳水化合物 (g)
    fiber DECIMAL(6,2) DEFAULT 0, -- 膳食纤维 (g)
    protein DECIMAL(6,2) DEFAULT 0, -- 蛋白质 (g)
    fat DECIMAL(6,2) DEFAULT 0, -- 脂肪 (g)
    calories DECIMAL(6,2) DEFAULT 0, -- 总热量 (kcal)
    gi_value DECIMAL(4,1), -- 升糖指数
    gl_value DECIMAL(5,2), -- 血糖负荷
    calculation_details JSONB, -- 计算详情
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_food_recognitions_user_id ON food_recognitions(user_id);
CREATE INDEX IF NOT EXISTS idx_food_recognitions_created_at ON food_recognitions(created_at);
CREATE INDEX IF NOT EXISTS idx_nutrition_records_user_id ON nutrition_records(user_id);
CREATE INDEX IF NOT EXISTS idx_nutrition_records_created_at ON nutrition_records(created_at);
CREATE INDEX IF NOT EXISTS idx_nutrition_records_food_recognition_id ON nutrition_records(food_recognition_id);

