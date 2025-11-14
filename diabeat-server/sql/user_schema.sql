-- DiabEat AI - 用户相关表结构

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id VARCHAR(255) UNIQUE NOT NULL, -- 新增设备ID，唯一且非空
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    name VARCHAR(100),
    phone VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(10),
    height DECIMAL(5,2), -- 身高（厘米）
    weight DECIMAL(5,2), -- 体重（公斤）
    age INTEGER, -- 年龄
    diabetes_type VARCHAR(20) NOT NULL, -- 'type1', 'type2', 'gestational', 'prediabetes'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE
);

-- 创建用户参数表
CREATE TABLE IF NOT EXISTS user_parameters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    insulin_type VARCHAR(50), -- 'rapid', 'long', 'mixed'
    isf DECIMAL(5,2), -- Insulin Sensitivity Factor (每单位胰岛素降低的血糖值)
    icr DECIMAL(5,2), -- Insulin-to-Carb Ratio (每单位胰岛素覆盖的碳水克数)
    target_bg_low DECIMAL(4,1) DEFAULT 4.0, -- 目标血糖下限
    target_bg_high DECIMAL(4,1) DEFAULT 7.8, -- 目标血糖上限
    max_insulin_dose DECIMAL(5,2), -- 最大单次剂量
    min_insulin_dose DECIMAL(5,2) DEFAULT 0.5, -- 最小单次剂量
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_diabetes_type ON users(diabetes_type);
CREATE INDEX IF NOT EXISTS idx_user_parameters_user_id ON user_parameters(user_id);
CREATE INDEX IF NOT EXISTS idx_users_device_id ON users(device_id); -- 新增设备ID索引

