-- DiabEat AI - 记录相关表结构

-- 创建用餐记录表
CREATE TABLE IF NOT EXISTS meal_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    meal_time TIMESTAMP NOT NULL,
    food_recognition_id UUID REFERENCES food_recognitions(id),
    nutrition_record_id UUID REFERENCES nutrition_records(id),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建胰岛素注射记录表
CREATE TABLE IF NOT EXISTS insulin_injection_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    injection_time TIMESTAMP NOT NULL,
    insulin_record_id UUID REFERENCES insulin_records(id),
    actual_dose DECIMAL(5,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_meal_records_user_id ON meal_records(user_id);
CREATE INDEX IF NOT EXISTS idx_meal_records_meal_time ON meal_records(meal_time);
CREATE INDEX IF NOT EXISTS idx_insulin_injection_records_user_id ON insulin_injection_records(user_id);
CREATE INDEX IF NOT EXISTS idx_insulin_injection_records_injection_time ON insulin_injection_records(injection_time);

