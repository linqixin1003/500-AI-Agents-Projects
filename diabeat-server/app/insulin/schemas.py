from pydantic import BaseModel, Field
from typing import Optional, List

class InsulinCalculationRequest(BaseModel):
    """胰岛素计算请求"""
    total_carbs: float = Field(..., gt=0, description="总碳水化合物（克）")
    current_bg: float = Field(..., gt=0, description="当前血糖值（mmol/L）")
    activity_level: Optional[str] = Field("sedentary", description="活动水平: sedentary, light, moderate, vigorous")
    meal_time: Optional[str] = Field(None, description="用餐时间（ISO格式）")

class InsulinCalculationResponse(BaseModel):
    """胰岛素计算响应"""
    recommended_dose: float = Field(..., description="建议总剂量（单位）")
    carb_insulin: float = Field(..., description="碳水胰岛素（单位）")
    correction_insulin: float = Field(..., description="校正胰岛素（单位）")
    activity_adjustment: float = Field(..., description="活动调整（单位）")
    injection_timing: str = Field(..., description="建议注射时机")
    split_dose: bool = Field(False, description="是否需要分次注射")
    risk_level: str = Field(..., description="风险等级: low, medium, high")
    warnings: List[str] = Field(default_factory=list, description="警告信息")

