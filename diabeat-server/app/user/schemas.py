from pydantic import BaseModel, Field, EmailStr
from typing import Optional
from datetime import datetime

class DeviceAuthRequest(BaseModel):
    """设备认证请求 (注册或登录)"""
    device_id: str = Field(..., description="设备唯一标识符")
    name: Optional[str] = Field(None, description="用户昵称 (注册时可选)")
    diabetes_type: Optional[str] = Field(None, description="糖尿病类型 (注册时可选): type1, type2, gestational, prediabetes")
    height: Optional[float] = Field(None, ge=50, le=250, description="身高（厘米，注册时可选）")
    # 可以添加更多注册时需要提供的可选信息

class UserResponse(BaseModel):
    """用户响应"""
    id: str
    device_id: str
    email: Optional[EmailStr] = None # email 现在是可选的
    name: Optional[str] = None
    diabetes_type: str
    height: Optional[float] = Field(None, ge=50, le=250, description="身高（厘米）")
    created_at: datetime

    class Config:
        from_attributes = True

class TokenResponse(BaseModel):
    """Token 响应"""
    access_token: str
    token_type: str = "bearer"
    user: UserResponse

class UserParameterCreate(BaseModel):
    """创建用户参数请求"""
    insulin_type: Optional[str] = None  # 'rapid', 'long', 'mixed'
    isf: Optional[float] = None  # Insulin Sensitivity Factor
    icr: Optional[float] = None  # Insulin-to-Carb Ratio
    target_bg_low: float = 4.0
    target_bg_high: float = 7.8
    max_insulin_dose: Optional[float] = None
    min_insulin_dose: float = 0.5

class UserParameterResponse(BaseModel):
    """用户参数响应"""
    id: str
    user_id: str
    insulin_type: Optional[str] = None
    isf: Optional[float] = None
    icr: Optional[float] = None
    target_bg_low: float
    target_bg_high: float
    max_insulin_dose: Optional[float] = None
    min_insulin_dose: float
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True

