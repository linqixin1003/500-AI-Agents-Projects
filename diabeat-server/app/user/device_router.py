from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from app.user.router import get_current_user_dependency
from app.user.schemas import UserResponse
from app.database import database
from datetime import datetime
import logging

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="",
    tags=["devices"]
)

class DeviceRegisterRequest(BaseModel):
    """设备注册请求"""
    fcm_token: str = Field(..., description="FCM token")
    device_id: str = Field(None, description="设备ID")
    device_type: str = Field("android", description="设备类型")

@router.post("/devices/register", status_code=status.HTTP_201_CREATED)
async def register_device(
    request: DeviceRegisterRequest,
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """注册设备 FCM token"""
    try:
        # 检查是否已存在
        query = """
            SELECT id FROM user_devices 
            WHERE user_id = :user_id AND fcm_token = :fcm_token
        """
        existing = await database.fetch_one(
            query=query,
            values={"user_id": current_user.id, "fcm_token": request.fcm_token}
        )
        
        if existing:
            # 更新现有记录
            update_query = """
                UPDATE user_devices 
                SET active = TRUE, updated_at = :updated_at
                WHERE id = :id
            """
            await database.execute(
                query=update_query,
                values={"id": existing["id"], "updated_at": datetime.utcnow()}
            )
            return {"message": "Device token updated"}
        else:
            # 创建新记录
            insert_query = """
                INSERT INTO user_devices (user_id, fcm_token, device_id, device_type, active, created_at, updated_at)
                VALUES (:user_id, :fcm_token, :device_id, :device_type, TRUE, :created_at, :updated_at)
                RETURNING id
            """
            result = await database.fetch_one(
                query=insert_query,
                values={
                    "user_id": current_user.id,
                    "fcm_token": request.fcm_token,
                    "device_id": request.device_id,
                    "device_type": request.device_type,
                    "created_at": datetime.utcnow(),
                    "updated_at": datetime.utcnow()
                }
            )
            return {"message": "Device registered", "device_id": str(result["id"])}
            
    except Exception as e:
        logger.error(f"Register device error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"注册设备失败: {str(e)}"
        )

