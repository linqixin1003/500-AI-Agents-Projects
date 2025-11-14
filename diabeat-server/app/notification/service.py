from typing import Optional
from uuid import uuid4
from datetime import datetime, timedelta
from app.database import database
from app.notification.schemas import NotificationScheduleRequest
import logging

logger = logging.getLogger(__name__)

class NotificationService:
    """通知服务"""
    
    async def schedule_insulin_reminder(
        self,
        user_id: str,
        reminder_time: datetime,
        predicted_dose: Optional[float] = None
    ) -> str:
        """安排胰岛素提醒通知
        
        Args:
            user_id: 用户ID
            reminder_time: 提醒时间
            predicted_dose: 预测的剂量（可选）
            
        Returns:
            str: 通知ID
        """
        # 计算提前提醒时间（提前15分钟）
        reminder_time_early = reminder_time - timedelta(minutes=15)
        
        title = "💉 胰岛素注射提醒"
        if predicted_dose:
            body = f"建议在 {reminder_time.strftime('%H:%M')} 注射胰岛素，预测剂量：{predicted_dose} 单位"
        else:
            body = f"建议在 {reminder_time.strftime('%H:%M')} 注射胰岛素，请根据用餐情况调整剂量"
        
        return await self._schedule_notification(
            user_id=user_id,
            reminder_time=reminder_time_early,
            notification_type="insulin_reminder",
            title=title,
            body=body,
            data={"predicted_dose": predicted_dose, "injection_time": reminder_time.isoformat()}
        )
    
    async def schedule_meal_reminder(
        self,
        user_id: str,
        reminder_time: datetime
    ) -> str:
        """安排用餐提醒通知"""
        title = "🍽️ 用餐提醒"
        body = f"建议在 {reminder_time.strftime('%H:%M')} 用餐，记得拍照记录哦"
        
        return await self._schedule_notification(
            user_id=user_id,
            reminder_time=reminder_time,
            notification_type="meal_reminder",
            title=title,
            body=body
        )
    
    async def _schedule_notification(
        self,
        user_id: str,
        reminder_time: datetime,
        notification_type: str,
        title: str,
        body: str,
        data: Optional[dict] = None
    ) -> str:
        """安排通知（保存到数据库，实际发送由后台任务处理）"""
        notification_id = str(uuid4())
        
        import json
        query = """
            INSERT INTO notifications 
            (id, user_id, reminder_time, notification_type, title, body, data, sent, created_at)
            VALUES 
            (:id, :user_id, :reminder_time, :notification_type, :title, :body, :data::jsonb, :sent, :created_at)
            RETURNING id
        """
        
        values = {
            "id": notification_id,
            "user_id": user_id,
            "reminder_time": reminder_time,
            "notification_type": notification_type,
            "title": title,
            "body": body,
            "data": json.dumps(data or {}),
            "sent": False,
            "created_at": datetime.utcnow()
        }
        
        result = await database.fetch_one(query=query, values=values)
        
        logger.info(f"📅 已安排通知: user={user_id}, time={reminder_time}, type={notification_type}")
        
        # 后台任务会自动处理待发送的通知
        # 见 app/notification/background_tasks.py 中的周期性任务
        
        return str(result["id"])
    
    async def get_pending_notifications(user_id: Optional[str] = None) -> list:
        """获取待发送的通知"""
        now = datetime.utcnow()
        
        if user_id:
            query = """
                SELECT * FROM notifications 
                WHERE user_id = :user_id 
                  AND sent = FALSE 
                  AND reminder_time <= :now
                ORDER BY reminder_time ASC
            """
            results = await database.fetch_all(query=query, values={"user_id": user_id, "now": now})
        else:
            query = """
                SELECT * FROM notifications 
                WHERE sent = FALSE 
                  AND reminder_time <= :now
                ORDER BY reminder_time ASC
            """
            results = await database.fetch_all(query=query, values={"now": now})
        
        return [dict(row) for row in results]
    
    async def mark_notification_sent(notification_id: str):
        """标记通知已发送"""
        query = """
            UPDATE notifications 
            SET sent = TRUE 
            WHERE id = :id
        """
        await database.execute(query=query, values={"id": notification_id})

