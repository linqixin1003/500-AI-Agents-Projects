from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

class NotificationScheduleRequest(BaseModel):
    """安排通知请求"""
    reminder_time: datetime = Field(..., description="提醒时间")
    notification_type: str = Field(..., description="通知类型: insulin_reminder, meal_reminder")
    title: str = Field(..., description="通知标题")
    body: str = Field(..., description="通知内容")
    data: Optional[dict] = Field(None, description="附加数据")

class NotificationResponse(BaseModel):
    """通知响应"""
    id: str
    user_id: str
    reminder_time: datetime
    notification_type: str
    title: str
    body: str
    sent: bool
    created_at: datetime

