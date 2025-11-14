-- DiabEat AI - 胰岛素相关表结构

-- 创建胰岛素记录表
CREATE TABLE IF NOT EXISTS insulin_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    nutrition_record_id UUID REFERENCES nutrition_records(id),
    current_bg DECIMAL(4,1), -- 当前血糖值
    recommended_dose DECIMAL(5,2) NOT NULL, -- 建议剂量
    actual_dose DECIMAL(5,2), -- 实际注射剂量（用户输入）
    carb_insulin DECIMAL(5,2), -- 碳水胰岛素
    correction_insulin DECIMAL(5,2), -- 校正胰岛素
    activity_factor DECIMAL(4,2) DEFAULT 1.0, -- 活动调整因子
    injection_time TIMESTAMP, -- 注射时间
    injection_type VARCHAR(20), -- 'single', 'split'
    risk_level VARCHAR(20), -- 'low', 'medium', 'high'
    doctor_approved BOOLEAN DEFAULT FALSE, -- 是否医生审核
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_insulin_records_user_id ON insulin_records(user_id);
CREATE INDEX IF NOT EXISTS idx_insulin_records_created_at ON insulin_records(created_at);
CREATE INDEX IF NOT EXISTS idx_insulin_records_nutrition_record_id ON insulin_records(nutrition_record_id);

