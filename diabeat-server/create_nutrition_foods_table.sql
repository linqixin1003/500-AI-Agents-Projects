-- 创建食物营养数据库表
CREATE TABLE IF NOT EXISTS nutrition_foods (
    name VARCHAR(255) PRIMARY KEY,
    calories FLOAT NOT NULL,
    carbs FLOAT NOT NULL,
    protein FLOAT NOT NULL,
    fat FLOAT NOT NULL,
    fiber FLOAT DEFAULT 0,
    gi_value INTEGER DEFAULT 50,
    category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入常见食物营养数据
INSERT INTO nutrition_foods (name, calories, carbs, protein, fat, fiber, gi_value, category) VALUES
-- 主食类
('米饭', 116, 25.6, 2.6, 0.3, 0.3, 73, '主食'),
('白米饭', 116, 25.6, 2.6, 0.3, 0.3, 73, '主食'),
('糙米饭', 111, 23.5, 2.6, 0.9, 1.8, 50, '主食'),
('面条', 137, 28.0, 4.5, 0.5, 1.2, 55, '主食'),
('馒头', 221, 47.0, 7.0, 1.1, 1.4, 88, '主食'),
('面包', 265, 50.0, 8.0, 4.0, 2.5, 70, '主食'),
('全麦面包', 247, 41.0, 13.0, 4.0, 7.0, 53, '主食'),

-- 肉类
('猪肉', 395, 0, 13.2, 37.0, 0, 0, '肉类'),
('牛肉', 125, 0, 20.0, 4.2, 0, 0, '肉类'),
('鸡肉', 167, 0, 19.3, 9.4, 0, 0, '肉类'),
('鱼肉', 104, 0, 17.6, 3.4, 0, 0, '肉类'),
('虾', 93, 0, 18.6, 1.2, 0, 0, '肉类'),
('鸡蛋', 144, 1.3, 13.3, 8.8, 0, 0, '肉类'),
('牛肉饼', 250, 5.0, 17.0, 15.0, 0.5, 30, '肉类'),

-- 蔬菜类
('白菜', 17, 3.2, 1.5, 0.1, 0.8, 15, '蔬菜'),
('青菜', 15, 2.8, 1.5, 0.3, 1.1, 15, '蔬菜'),
('生菜', 15, 2.9, 1.4, 0.2, 1.3, 15, '蔬菜'),
('番茄', 19, 4.0, 0.9, 0.2, 1.2, 30, '蔬菜'),
('黄瓜', 15, 3.6, 0.8, 0.1, 0.5, 15, '蔬菜'),
('胡萝卜', 39, 9.6, 0.6, 0.2, 2.8, 35, '蔬菜'),
('西兰花', 34, 7.2, 2.8, 0.4, 2.6, 15, '蔬菜'),
('洋葱', 39, 9.3, 1.1, 0.1, 1.4, 30, '蔬菜'),

-- 快餐类
('汉堡', 295, 34.0, 17.0, 11.0, 2.0, 66, '快餐'),
('炸鸡', 246, 12.0, 17.0, 15.0, 0.5, 55, '快餐'),
('薯条', 312, 41.0, 3.8, 15.0, 3.8, 75, '快餐'),
('披萨', 266, 33.0, 11.0, 10.0, 2.3, 60, '快餐'),
('炸洋葱圈', 331, 33.0, 3.8, 19.0, 2.1, 65, '快餐'),

-- 奶制品
('牛奶', 54, 5.0, 3.0, 3.2, 0, 27, '奶制品'),
('酸奶', 72, 9.3, 2.5, 2.7, 0, 36, '奶制品'),
('芝士', 353, 1.3, 23.0, 29.0, 0, 0, '奶制品'),
('奶酪', 328, 4.1, 25.0, 23.0, 0, 0, '奶制品'),

-- 水果类
('苹果', 52, 13.8, 0.3, 0.2, 2.4, 36, '水果'),
('香蕉', 89, 22.8, 1.1, 0.3, 2.6, 51, '水果'),
('橙子', 47, 11.8, 0.9, 0.1, 2.4, 40, '水果'),
('葡萄', 69, 18.1, 0.6, 0.2, 0.9, 59, '水果'),
('西瓜', 30, 7.6, 0.6, 0.2, 0.4, 72, '水果')

ON CONFLICT (name) DO UPDATE SET
    calories = EXCLUDED.calories,
    carbs = EXCLUDED.carbs,
    protein = EXCLUDED.protein,
    fat = EXCLUDED.fat,
    fiber = EXCLUDED.fiber,
    gi_value = EXCLUDED.gi_value,
    category = EXCLUDED.category,
    updated_at = CURRENT_TIMESTAMP;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_nutrition_foods_category ON nutrition_foods(category);
CREATE INDEX IF NOT EXISTS idx_nutrition_foods_gi_value ON nutrition_foods(gi_value);
