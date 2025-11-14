"""记录相关的数据模型"""
from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime

# 用餐记录相关
class FoodItemInput(BaseModel):
    """食物项输入"""
    name: str = Field(..., description="食物名称")
    weight: float = Field(..., description="重量（克）")
    cooking_method: Optional[str] = Field(None, description="烹饪方式")

class MealRecordCreate(BaseModel):
    """创建用餐记录"""
    meal_time: datetime = Field(..., description="用餐时间")
    food_recognition_id: Optional[str] = Field(None, description="食物识别记录ID")
    nutrition_record_id: Optional[str] = Field(None, description="营养记录ID")
    notes: Optional[str] = Field(None, description="备注")
    food_items: Optional[List[FoodItemInput]] = Field(None, description="手动添加的食物列表")
    meal_type: Optional[str] = Field(None, description="餐次类型 (breakfast/lunch/dinner/snack)")

class MealRecordResponse(BaseModel):
    """用餐记录响应"""
    id: str
    user_id: str
    meal_time: datetime
    food_recognition_id: Optional[str] = None
    nutrition_record_id: Optional[str] = None
    notes: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

# 胰岛素记录相关
class InsulinRecordCreate(BaseModel):
    """创建胰岛素记录"""
    injection_time: datetime = Field(..., description="注射时间")
    insulin_type: str = Field(..., description="胰岛素类型")
    dosage: float = Field(..., description="剂量（单位）")
    injection_site: Optional[str] = Field(None, description="注射部位")
    notes: Optional[str] = Field(None, description="备注")

class InsulinRecordResponse(BaseModel):
    """胰岛素记录响应"""
    id: str
    user_id: str
    injection_time: datetime
    insulin_type: str
    dosage: float
    injection_site: Optional[str] = None
    notes: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

# 运动记录相关
class ExerciseRecordCreate(BaseModel):
    """创建运动记录"""
    exercise_time: datetime = Field(..., description="运动时间")
    exercise_type: str = Field(..., description="运动类型 (walking/running/cycling/swimming/gym/other)")
    duration_minutes: int = Field(..., description="运动时长（分钟）", gt=0)
    intensity: str = Field(default="moderate", description="运动强度 (light/moderate/vigorous)")
    calories_burned: Optional[float] = Field(None, description="消耗热量（可选，系统会自动估算）")
    notes: Optional[str] = Field(None, description="备注")

class ExerciseRecordResponse(BaseModel):
    """运动记录响应"""
    id: str
    user_id: str
    exercise_time: datetime
    exercise_type: str
    duration_minutes: int
    intensity: str
    calories_burned: float
    notes: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

class TodayExerciseSummary(BaseModel):
    """今日运动汇总"""
    total_calories: float = Field(..., description="总消耗热量")
    total_duration: int = Field(..., description="总运动时长（分钟）")
    exercise_count: int = Field(..., description="运动次数")
    exercises: List[ExerciseRecordResponse] = Field(default_factory=list, description="运动记录列表")

# 水分记录相关
class WaterRecordCreate(BaseModel):
    """创建水分记录"""
    record_time: datetime = Field(..., description="记录时间")
    amount_ml: int = Field(..., description="摄入量（毫升）", gt=0, le=2000)  # 单次最多2L
    water_type: str = Field(default="water", description="类型 (water/tea/coffee/juice/other)")
    notes: Optional[str] = Field(None, description="备注")

class WaterRecordResponse(BaseModel):
    """水分记录响应"""
    id: str
    user_id: str
    record_time: datetime
    amount_ml: int
    water_type: str
    notes: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

class TodayWaterSummary(BaseModel):
    """今日水分摄入汇总"""
    total_ml: int = Field(..., description="总摄入量（毫升）")
    record_count: int = Field(..., description="记录次数")
    records: List[WaterRecordResponse] = Field(default_factory=list, description="水分记录列表")
    progress_percentage: float = Field(..., description="完成百分比（基于2000ml目标）")

# 胰岛素预测相关
class NextInsulinPredictionResponse(BaseModel):
    """下次胰岛素注射预测响应"""
    predicted_time: datetime = Field(..., description="预测的注射时间")
    predicted_dose: Optional[float] = Field(None, description="预测的剂量（如果可预测）")
    confidence: float = Field(..., description="预测置信度 (0-1)", ge=0, le=1)
    reasoning: str = Field(..., description="预测依据说明")
    notification_scheduled: bool = Field(default=False, description="是否已安排通知")

# 用药记录相关
class MedicationRecordCreate(BaseModel):
    """创建用药记录"""
    medication_time: datetime = Field(..., description="用药时间")
    medication_type: str = Field(..., description="药物类型 (insulin/oral_medication/other)")
    medication_name: str = Field(..., description="药物名称")
    dosage: float = Field(..., description="剂量")
    dosage_unit: str = Field(..., description="剂量单位 (units/mg/ml/tablets)")
    notes: Optional[str] = Field(None, description="备注")

class MedicationRecordResponse(BaseModel):
    """用药记录响应"""
    id: str
    user_id: str
    medication_time: datetime
    medication_type: str
    medication_name: str
    dosage: float
    dosage_unit: str
    notes: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

class TodayMedicationSummary(BaseModel):
    """今日用药汇总"""
    total_count: int = Field(..., description="总用药次数")
    insulin_count: int = Field(..., description="胰岛素注射次数")
    oral_medication_count: int = Field(..., description="口服药次数")
    medications: List[MedicationRecordResponse] = Field(default_factory=list, description="用药记录列表")

# 智能提醒相关
class SmartReminderResponse(BaseModel):
    """智能提醒响应"""
    next_meal_time: Optional[datetime] = Field(None, description="下次用餐时间预测")
    next_medication_time: Optional[datetime] = Field(None, description="下次用药时间预测")
    meal_reminder_message: Optional[str] = Field(None, description="用餐提醒消息")
    medication_reminder_message: Optional[str] = Field(None, description="用药提醒消息")
    should_eat_soon: bool = Field(default=False, description="是否应该尽快进食")
    should_take_medication_soon: bool = Field(default=False, description="是否应该尽快用药")
    reasoning: str = Field(..., description="提醒依据")

# 餐次历史相关
class MealHistoryItem(BaseModel):
    """餐次历史项（包含营养信息）"""
    id: str
    user_id: str
    meal_time: datetime
    food_recognition_id: Optional[str] = None
    nutrition_record_id: Optional[str] = None
    notes: Optional[str] = None
    created_at: datetime
    total_carbs: Optional[float] = None
    net_carbs: Optional[float] = None
    protein: Optional[float] = None
    fat: Optional[float] = None
    fiber: Optional[float] = None
    calories: Optional[float] = None
    
    class Config:
        from_attributes = True
