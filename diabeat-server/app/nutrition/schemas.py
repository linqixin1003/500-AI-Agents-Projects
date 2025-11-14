from pydantic import BaseModel, Field
from typing import Optional, List

class FoodItemInput(BaseModel):
    """输入的食物项"""
    name: str = Field(..., description="食物名称")
    weight: float = Field(..., gt=0, description="重量（克）")
    cooking_method: Optional[str] = Field(None, description="烹饪方式")

class NutritionCalculationRequest(BaseModel):
    """营养成分计算请求"""
    foods: List[FoodItemInput] = Field(..., description="食物列表")

class NutritionCalculationResponse(BaseModel):
    """营养成分计算响应"""
    total_carbs: float = Field(..., description="总碳水化合物（克）")
    net_carbs: float = Field(..., description="净碳水化合物（克）")
    protein: float = Field(..., description="蛋白质（克）")
    fat: float = Field(..., description="脂肪（克）")
    fiber: float = Field(..., description="膳食纤维（克）")
    calories: float = Field(..., description="总热量（千卡）")
    gi_value: Optional[float] = Field(None, description="升糖指数")
    gl_value: Optional[float] = Field(None, description="血糖负荷")
    calculation_details: Optional[List[dict]] = Field(None, description="计算详情")
    nutrition_record_id: Optional[str] = Field(None, description="营养记录ID（如果已保存）")

