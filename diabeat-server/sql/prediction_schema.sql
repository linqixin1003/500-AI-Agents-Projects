-- DiabEat AI - 血糖预测相关表结构

-- 创建血糖预测记录表
CREATE TABLE IF NOT EXISTS bg_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    insulin_record_id UUID REFERENCES insulin_records(id),
    nutrition_record_id UUID REFERENCES nutrition_records(id),
    prediction_data JSONB NOT NULL, -- 预测数据
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建实际血糖记录表（用于时序数据，也可使用 InfluxDB）
CREATE TABLE IF NOT EXISTS actual_blood_glucose (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bg_value DECIMAL(4,1) NOT NULL, -- 血糖值
    source VARCHAR(20) NOT NULL, -- 'manual', 'cgm', 'meter'
    measured_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户活动记录表
CREATE TABLE IF NOT EXISTS user_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL, -- 'walking', 'running', 'cycling', etc.
    duration_minutes INTEGER, -- 持续时间（分钟）
    intensity VARCHAR(20), -- 'light', 'moderate', 'vigorous'
    calories_burned DECIMAL(6,2),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_bg_predictions_user_id ON bg_predictions(user_id);
CREATE INDEX IF NOT EXISTS idx_bg_predictions_created_at ON bg_predictions(created_at);
CREATE INDEX IF NOT EXISTS idx_actual_bg_user_id ON actual_blood_glucose(user_id);
CREATE INDEX IF NOT EXISTS idx_actual_bg_measured_at ON actual_blood_glucose(measured_at);
CREATE INDEX IF NOT EXISTS idx_user_activities_user_id ON user_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_user_activities_start_time ON user_activities(start_time);

