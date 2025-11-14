# DiabEat AI API 使用示例

## 📋 完整流程示例

### 1. 用户注册和登录

```bash
# 注册
curl -X POST "http://localhost:8000/api/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "diabetes_type": "type1",
    "name": "张三"
  }'

# 登录获取 Token
curl -X POST "http://localhost:8000/api/users/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user@example.com&password=password123"

# 响应示例
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "name": "张三",
    "diabetes_type": "type1",
    "created_at": "2025-11-06T10:00:00Z"
  }
}
```

### 2. 设置用户参数

```bash
# 设置用户参数（ISF, ICR等）
curl -X POST "http://localhost:8000/api/users/{user_id}/parameters" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "isf": 2.5,
    "icr": 10.0,
    "target_bg_low": 4.0,
    "target_bg_high": 7.8,
    "max_insulin_dose": 15.0,
    "min_insulin_dose": 0.5
  }'
```

### 3. 食物识别

```bash
# 上传食物图片进行识别
curl -X POST "http://localhost:8000/api/food/recognize" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "image=@food.jpg"

# 响应示例
{
  "recognition_id": "abc-123-def-456",
  "foods": [
    {
      "name": "白米饭",
      "weight": 200.0,
      "confidence": 0.95,
      "cooking_method": "steamed"
    },
    {
      "name": "红烧肉",
      "weight": 120.0,
      "confidence": 0.90,
      "cooking_method": "braised"
    }
  ],
  "total_confidence": 0.92,
  "image_url": "http://localhost:8000/static/food/user123/image.jpg"
}
```

### 4. 计算营养成分

```bash
# 基于识别结果计算营养成分
curl -X POST "http://localhost:8000/api/nutrition/calculate" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "foods": [
      {
        "name": "白米饭",
        "weight": 200.0,
        "cooking_method": "steamed"
      },
      {
        "name": "红烧肉",
        "weight": 120.0,
        "cooking_method": "braised"
      }
    ]
  }'

# 响应示例
{
  "total_carbs": 56.8,
  "net_carbs": 56.5,
  "protein": 8.5,
  "fat": 36.6,
  "fiber": 0.3,
  "calories": 512.0,
  "gi_value": 65.2,
  "gl_value": 37.0,
  "calculation_details": [
    {
      "name": "白米饭",
      "weight": 200.0,
      "carbs": 51.8,
      "net_carbs": 51.5,
      "protein": 5.2,
      "fat": 0.6,
      "fiber": 0.6,
      "calories": 232.0,
      "gi_value": 78.85
    },
    {
      "name": "红烧肉",
      "weight": 120.0,
      "carbs": 6.0,
      "net_carbs": 6.0,
      "protein": 18.0,
      "fat": 36.0,
      "fiber": 0.0,
      "calories": 384.0,
      "gi_value": null
    }
  ]
}
```

### 5. 计算胰岛素剂量

```bash
# 基于营养成分和当前血糖计算胰岛素剂量
curl -X POST "http://localhost:8000/api/insulin/calculate" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "total_carbs": 56.8,
    "current_bg": 8.5,
    "activity_level": "sedentary",
    "meal_time": "2025-11-06T12:00:00Z"
  }' \
  -G \
  --data-urlencode "gi_value=65.2"

# 响应示例
{
  "recommended_dose": 7.2,
  "carb_insulin": 5.68,
  "correction_insulin": 1.8,
  "activity_adjustment": 0.0,
  "injection_timing": "餐前15分钟",
  "split_dose": false,
  "risk_level": "low",
  "warnings": []
}
```

### 6. 预测血糖

```bash
# 预测餐后血糖变化
curl -X POST "http://localhost:8000/api/prediction/blood-glucose" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "total_carbs": 56.8,
    "insulin_dose": 7.2,
    "current_bg": 8.5,
    "gi_value": 65.2,
    "activity_level": "sedentary"
  }'

# 响应示例
{
  "predictions": [
    {
      "time_minutes": 30,
      "bg_value": 9.2,
      "confidence": 0.85
    },
    {
      "time_minutes": 60,
      "bg_value": 10.5,
      "confidence": 0.90
    },
    {
      "time_minutes": 90,
      "bg_value": 10.8,
      "confidence": 0.90
    },
    {
      "time_minutes": 120,
      "bg_value": 9.5,
      "confidence": 0.88
    },
    {
      "time_minutes": 180,
      "bg_value": 8.2,
      "confidence": 0.85
    },
    {
      "time_minutes": 240,
      "bg_value": 7.5,
      "confidence": 0.80
    }
  ],
  "peak_time": 90,
  "peak_value": 10.8,
  "risk_level": "medium",
  "recommendations": [
    "预测血糖略高，建议适当增加胰岛素或增加运动",
    "建议监测餐后2小时血糖，如超过目标范围需调整"
  ]
}
```

## 🔄 完整工作流程

```
1. 用户注册/登录
   ↓
2. 设置用户参数（ISF, ICR等）
   ↓
3. 拍照识别食物
   ↓
4. 计算营养成分
   ↓
5. 计算胰岛素剂量
   ↓
6. 预测血糖变化
   ↓
7. 确认并记录
```

## 📝 注意事项

1. **所有 API 都需要认证**（除了注册和登录）
2. **用户参数必须先设置**才能计算胰岛素剂量
3. **营养成分数据库**当前使用内置数据，实际应该从数据库查询
4. **血糖预测**当前使用规则引擎，后续可升级为 ML 模型

---

**最后更新**: 2025-11-06

