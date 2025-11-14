from typing import Optional
import firebase_admin
from firebase_admin import credentials, messaging
from app.config import settings
import logging

logger = logging.getLogger(__name__)

class FCMService:
    """Firebase Cloud Messaging 服务"""
    
    _initialized = False
    
    @classmethod
    def initialize(cls):
        """初始化 FCM"""
        if cls._initialized:
            return
        
        try:
            # 从环境变量或配置文件读取 Firebase 凭证
            # 方法1: 使用 FIREBASE_CREDENTIALS_PATH
            if hasattr(settings, 'FIREBASE_CREDENTIALS_PATH') and settings.FIREBASE_CREDENTIALS_PATH:
                cred = credentials.Certificate(settings.FIREBASE_CREDENTIALS_PATH)
                firebase_admin.initialize_app(cred)
            # 方法2: 使用 GOOGLE_APPLICATION_CREDENTIALS 环境变量
            elif os.getenv("GOOGLE_APPLICATION_CREDENTIALS"):
                # firebase_admin 会自动从环境变量读取
                firebase_admin.initialize_app()
            else:
                # 使用默认凭证（从环境变量）
                firebase_admin.initialize_app()
            
            cls._initialized = True
            logger.info("FCM initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize FCM: {str(e)}")
            raise
    
    @classmethod
    def send_notification(
        cls,
        fcm_token: str,
        title: str,
        body: str,
        data: Optional[dict] = None
    ) -> bool:
        """发送推送通知
        
        Args:
            fcm_token: FCM 设备 token
            title: 通知标题
            body: 通知内容
            data: 附加数据
            
        Returns:
            bool: 是否发送成功
        """
        if not cls._initialized:
            cls.initialize()
        
        try:
            message = messaging.Message(
                notification=messaging.Notification(
                    title=title,
                    body=body
                ),
                data=data or {},
                token=fcm_token
            )
            
            response = messaging.send(message)
            logger.info(f"Successfully sent message: {response}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to send FCM notification: {str(e)}")
            return False
    
    @classmethod
    def send_multicast(
        cls,
        fcm_tokens: list,
        title: str,
        body: str,
        data: Optional[dict] = None
    ) -> dict:
        """批量发送推送通知
        
        Args:
            fcm_tokens: FCM 设备 token 列表
            title: 通知标题
            body: 通知内容
            data: 附加数据
            
        Returns:
            dict: 发送结果统计
        """
        if not cls._initialized:
            cls.initialize()
        
        try:
            message = messaging.MulticastMessage(
                notification=messaging.Notification(
                    title=title,
                    body=body
                ),
                data=data or {},
                tokens=fcm_tokens
            )
            
            response = messaging.send_multicast(message)
            
            result = {
                "success_count": response.success_count,
                "failure_count": response.failure_count,
                "responses": []
            }
            
            for idx, resp in enumerate(response.responses):
                if resp.success:
                    result["responses"].append({
                        "token": fcm_tokens[idx],
                        "status": "success"
                    })
                else:
                    result["responses"].append({
                        "token": fcm_tokens[idx],
                        "status": "failed",
                        "error": str(resp.exception) if resp.exception else "Unknown error"
                    })
            
            logger.info(f"Multicast result: {result['success_count']} success, {result['failure_count']} failed")
            return result
            
        except Exception as e:
            logger.error(f"Failed to send multicast FCM notification: {str(e)}")
            return {
                "success_count": 0,
                "failure_count": len(fcm_tokens),
                "error": str(e)
            }

