"""智能提醒路由"""
from fastapi import APIRouter, Depends, HTTPException, status
from app.records.schemas import SmartReminderResponse
from app.reminders.service import SmartReminderService, get_reminder_service
from app.user.schemas import UserResponse
from app.user.router import get_current_user_dependency
import logging

logger = logging.getLogger(__name__)

router = APIRouter()


@router.get(
    "/smart",
    response_model=SmartReminderResponse,
    summary="获取智能提醒",
    description="根据用户的饮食和用药习惯，提供智能的用餐和用药提醒"
)
async def get_smart_reminders(
    reminder_service: SmartReminderService = Depends(get_reminder_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取智能提醒"""
    try:
        logger.info(f"🔔 获取智能提醒: user={current_user.id}")
        result = await reminder_service.get_smart_reminders(current_user.id)
        
        return SmartReminderResponse(
            next_meal_time=result.get("next_meal_time"),
            next_medication_time=result.get("next_medication_time"),
            meal_reminder_message=result.get("meal_reminder_message"),
            medication_reminder_message=result.get("medication_reminder_message"),
            should_eat_soon=result.get("should_eat_soon", False),
            should_take_medication_soon=result.get("should_take_medication_soon", False),
            reasoning=result.get("reasoning", "")
        )
    except Exception as e:
        logger.error(f"Get smart reminders error: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取智能提醒失败: {str(e)}"
        )

