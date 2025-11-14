import asyncio
from datetime import datetime
from typing import List
from app.database import database
from app.notification.service import NotificationService
from app.notification.fcm_service import FCMService
import logging

logger = logging.getLogger(__name__)

class NotificationBackgroundTask:
    """通知后台任务"""
    
    @staticmethod
    async def process_pending_notifications():
        """处理待发送的通知"""
        try:
            # 获取待发送的通知
            notifications = await NotificationService.get_pending_notifications()
            
            if not notifications:
                return
            
            logger.info(f"Processing {len(notifications)} pending notifications")
            
            for notification in notifications:
                try:
                    # 获取用户的 FCM token
                    user_id = notification["user_id"]
                    fcm_token = await NotificationBackgroundTask.get_user_fcm_token(user_id)
                    
                    if not fcm_token:
                        logger.warning(f"No FCM token for user {user_id}")
                        # 标记为已发送（即使没有 token，避免重复处理）
                        await NotificationService.mark_notification_sent(notification["id"])
                        continue
                    
                    # 发送 FCM 通知
                    success = FCMService.send_notification(
                        fcm_token=fcm_token,
                        title=notification["title"],
                        body=notification["body"],
                        data=notification.get("data", {})
                    )
                    
                    if success:
                        # 标记为已发送
                        await NotificationService.mark_notification_sent(notification["id"])
                        logger.info(f"Notification {notification['id']} sent successfully")
                    else:
                        logger.error(f"Failed to send notification {notification['id']}")
                        
                except Exception as e:
                    logger.error(f"Error processing notification {notification.get('id')}: {str(e)}")
                    
        except Exception as e:
            logger.error(f"Error in process_pending_notifications: {str(e)}")
    
    @staticmethod
    async def get_user_fcm_token(user_id: str) -> str:
        """获取用户的 FCM token"""
        query = """
            SELECT fcm_token FROM user_devices 
            WHERE user_id = :user_id AND active = TRUE 
            ORDER BY updated_at DESC 
            LIMIT 1
        """
        
        result = await database.fetch_one(query=query, values={"user_id": user_id})
        if result:
            return result.get("fcm_token")
        return None
    
    @staticmethod
    async def run_periodic_task():
        """运行周期性任务"""
        while True:
            try:
                await NotificationBackgroundTask.process_pending_notifications()
                # 每 60 秒检查一次
                await asyncio.sleep(60)
            except Exception as e:
                logger.error(f"Error in periodic task: {str(e)}")
                await asyncio.sleep(60)

