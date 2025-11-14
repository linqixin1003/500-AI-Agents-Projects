from pydantic import BaseModel, Field
from typing import List, Optional

class BloodGlucosePrediction(BaseModel):
    """血糖预测点"""
    time_minutes: int = Field(..., description="时间（分钟，餐后）")
    bg_value: float = Field(..., description="预测血糖值（mmol/L）")
    confidence: float = Field(..., ge=0, le=1, description="预测置信度（0-1）")

class BloodGlucosePredictionRequest(BaseModel):
    """血糖预测请求"""
    total_carbs: float = Field(..., gt=0, description="总碳水化合物（克）")
    insulin_dose: float = Field(..., ge=0, description="胰岛素剂量（单位）")
    current_bg: float = Field(..., gt=0, description="当前血糖值（mmol/L）")
    gi_value: Optional[float] = Field(None, description="升糖指数")
    activity_level: Optional[str] = Field("sedentary", description="活动水平")

class BloodGlucosePredictionResponse(BaseModel):
    """血糖预测响应"""
    predictions: List[BloodGlucosePrediction] = Field(..., description="预测数据点")
    peak_time: int = Field(..., description="峰值时间（分钟）")
    peak_value: float = Field(..., description="峰值血糖值（mmol/L）")
    risk_level: str = Field(..., description="风险等级: low, medium, high")
    recommendations: List[str] = Field(default_factory=list, description="优化建议")
    confidence_score: Optional[float] = Field(0.85, description="预测置信度分数")
    note: Optional[str] = Field(None, description="附加说明")
    risk_assessment: Optional[dict] = Field(default_factory=dict, description="详细风险评估")

class HealthInsightQuery(BaseModel):
    """健康见解查询请求"""
    question: str = Field(..., description="健康咨询问题", min_length=5, max_length=1000)
    context: Optional[str] = Field(None, description="额外上下文信息")

