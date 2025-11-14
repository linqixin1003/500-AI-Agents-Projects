from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class FoodRecommendation(BaseModel):
    """食物建议信息"""
    recommended_weight: float = Field(..., description="建议食用量（克）")
    recommended_carbs: float = Field(..., description="建议碳水摄入量（克）")
    reason: str = Field(..., description="建议原因")
    adjustment_factor: float = Field(..., description="调整系数（建议量/当前量）")
    adjustment_percent: float = Field(..., description="调整百分比（%）")
    warning: Optional[str] = Field(None, description="警告信息")
    current_weight: float = Field(..., description="当前识别重量（克）")
    gi_value: Optional[float] = Field(None, description="升糖指数")
    gl_value: Optional[float] = Field(None, description="血糖负荷")
    can_eat_all: Optional[bool] = Field(None, description="是否可以全部食用（基于今日剩余额度的整体判断）")

class FoodItem(BaseModel):
    """识别出的食物项"""
    name: str = Field(..., description="食物名称")
    calories: Optional[float] = Field(None, description="卡路里（大卡）")
    weight: Optional[float] = Field(None, description="重量（克）")
    confidence: Optional[float] = Field(None, ge=0, le=1, description="识别置信度（0-1）")
    cooking_method: Optional[str] = Field(None, description="烹饪方式")
    # 营养成分（对糖尿病人重要）
    carbs: Optional[float] = Field(None, description="碳水化合物（克）")
    net_carbs: Optional[float] = Field(None, description="净碳水化合物（克，扣除纤维）")
    protein: Optional[float] = Field(None, description="蛋白质（克）")
    fat: Optional[float] = Field(None, description="脂肪（克）")
    fiber: Optional[float] = Field(None, description="膳食纤维（克）")
    gi_value: Optional[float] = Field(None, description="升糖指数（GI）")
    gl_value: Optional[float] = Field(None, description="血糖负荷（GL）")
    # 建议信息
    recommendation: Optional[FoodRecommendation] = Field(None, description="建议食用量信息")

class FoodRecognitionResponse(BaseModel):
    """食物识别响应"""
    recognition_id: str = Field(..., description="识别记录ID")
    foods: List[FoodItem] = Field(..., description="识别出的食物列表")
    total_confidence: float = Field(..., ge=0, le=1, description="总体置信度")
    image_url: str = Field(..., description="图片访问URL")

class FoodRecognitionRequest(BaseModel):
    """食物识别请求（用于文档）"""
    pass  # 实际使用 multipart/form-data

