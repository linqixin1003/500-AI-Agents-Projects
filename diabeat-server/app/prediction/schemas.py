from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class BloodGlucosePrediction(BaseModel):
    """血糖预测点"""
    time_minutes: int = Field(..., description="时间（分钟，餐后）")
    bg_value: float = Field(..., description="预测血糖值（mmol/L）")
    confidence: float = Field(..., ge=0, le=1, description="预测置信度（0-1）")

# 历史记录模型（用于AI上下文）
class MealHistoryItem(BaseModel):
    """餐食历史记录"""
    meal_time: str = Field(..., description="进食时间（ISO格式）")
    total_carbs: float = Field(..., description="碳水化合物（克）")
    meal_type: Optional[str] = Field(None, description="餐次类型")
    foods: Optional[str] = Field(None, description="食物描述")

class MedicationHistoryItem(BaseModel):
    """用药历史记录"""
    medication_time: str = Field(..., description="用药时间（ISO格式）")
    medication_type: str = Field(..., description="药物类型")
    dosage: float = Field(..., description="剂量")

class ExerciseHistoryItem(BaseModel):
    """运动历史记录"""
    exercise_time: str = Field(..., description="运动时间（ISO格式）")
    exercise_type: str = Field(..., description="运动类型")
    duration: int = Field(..., description="时长（分钟）")
    intensity: Optional[str] = Field(None, description="强度")

class WaterHistoryItem(BaseModel):
    """饮水历史记录"""
    record_time: str = Field(..., description="记录时间（ISO格式）")
    amount: int = Field(..., description="水量（ml）")

class BloodGlucosePredictionRequest(BaseModel):
    """血糖预测请求"""
    total_carbs: float = Field(..., gt=0, description="总碳水化合物（克）")
    insulin_dose: float = Field(..., ge=0, description="胰岛素剂量（单位）")
    current_bg: float = Field(..., gt=0, description="当前血糖值（mmol/L）")
    gi_value: Optional[float] = Field(None, description="升糖指数")
    activity_level: Optional[str] = Field("sedentary", description="活动水平")
    
    # 时间上下文（可选，用于时间感知预测）
    meal_time: Optional[str] = Field(None, description="最近进食时间（ISO格式）")
    medication_time: Optional[str] = Field(None, description="最近用药时间（ISO格式）")
    current_time: Optional[str] = Field(None, description="当前时间（ISO格式）")
    
    # 用户基础信息（可选，用于个性化预测）
    weight: Optional[float] = Field(None, description="体重（kg）")
    height: Optional[float] = Field(None, description="身高（cm）")
    age: Optional[int] = Field(None, description="年龄")
    gender: Optional[str] = Field(None, description="性别（male/female）")
    diabetes_type: Optional[str] = Field(None, description="糖尿病类型（type1/type2）")
    
    # 历史记录（可选，用于AI上下文理解）
    recent_meals: Optional[List[MealHistoryItem]] = Field(None, description="最近3次进食")
    recent_medications: Optional[List[MedicationHistoryItem]] = Field(None, description="最近3次用药")
    recent_exercises: Optional[List[ExerciseHistoryItem]] = Field(None, description="最近3次运动")
    recent_water: Optional[List[WaterHistoryItem]] = Field(None, description="最近3次饮水")

class BloodGlucosePredictionResponse(BaseModel):
    """血糖预测响应"""
    prediction_id: Optional[str] = Field(None, description="预测记录ID，用于纠正追踪")
    predictions: List[BloodGlucosePrediction] = Field(..., description="预测数据点")
    peak_time: int = Field(..., description="峰值时间（分钟）")
    peak_value: float = Field(..., description="峰值血糖值（mmol/L）")
    risk_level: str = Field(..., description="风险等级: low, medium, high")
    recommendations: List[str] = Field(default_factory=list, description="优化建议")
    confidence_score: Optional[float] = Field(0.85, description="预测置信度分数")
    note: Optional[str] = Field(None, description="附加说明")
    risk_assessment: Optional[dict] = Field(default_factory=dict, description="详细风险评估")

class BloodGlucoseCorrectionRequest(BaseModel):
    """血糖预测纠正请求"""
    prediction_id: str = Field(..., description="预测记录ID")
    actual_value: float = Field(..., gt=0, description="实测血糖值（mmol/L）")
    prediction_time_minutes: Optional[int] = Field(None, description="纠正的预测时间点（分钟）")
    measured_at: Optional[datetime] = Field(None, description="实测时间，默认当前时间")
    source: str = Field("manual", description="数据来源: manual/cgm/meter")
    note: Optional[str] = Field(None, description="备注")

class BloodGlucoseCorrectionResponse(BaseModel):
    """血糖预测纠正响应"""
    id: str
    prediction_id: str
    predicted_value: float
    actual_value: float
    difference: float
    prediction_time_minutes: Optional[int] = None
    measured_at: datetime
    source: str
    note: Optional[str] = None
    created_at: datetime

class HealthInsightQuery(BaseModel):
    """健康见解查询请求"""
    question: str = Field(..., description="健康咨询问题", min_length=5, max_length=1000)
    context: Optional[str] = Field(None, description="额外上下文信息")

