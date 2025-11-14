from fastapi import APIRouter, Depends, HTTPException, status, BackgroundTasks
from app.notification import schemas
from app.notification.service import NotificationService
from app.user.router import get_current_user_dependency
from app.user.schemas import UserResponse
import logging

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="",
    tags=["notifications"]
)

def get_notification_service() -> NotificationService:
    """获取通知服务实例"""
    return NotificationService()

@router.post(
    "/schedule",
    response_model=schemas.NotificationResponse,
    status_code=status.HTTP_201_CREATED,
    summary="安排通知",
    description="安排一个通知提醒"
)
async def schedule_notification(
    request: schemas.NotificationScheduleRequest,
    notification_service: NotificationService = Depends(get_notification_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """安排通知"""
    try:
        notification_id = await notification_service._schedule_notification(
            user_id=current_user.id,
            reminder_time=request.reminder_time,
            notification_type=request.notification_type,
            title=request.title,
            body=request.body,
            data=request.data
        )
        
        # 获取创建的通知
        from app.database import database
        query = "SELECT * FROM notifications WHERE id = :id"
        result = await database.fetch_one(query=query, values={"id": notification_id})
        
        if result:
            return schemas.NotificationResponse(
                id=str(result["id"]),
                user_id=str(result["user_id"]),
                reminder_time=result["reminder_time"],
                notification_type=result["notification_type"],
                title=result["title"],
                body=result["body"],
                sent=result["sent"],
                created_at=result["created_at"]
            )
        raise Exception("Failed to create notification")
    except Exception as e:
        logger.error(f"Schedule notification error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"安排通知失败: {str(e)}"
        )

