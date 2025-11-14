from typing import Optional, Dict, Any
from databases import Database
from app.database import database
from uuid import uuid4
from datetime import datetime
from decimal import Decimal
import logging

logger = logging.getLogger(__name__)

async def get_user_by_id(user_id: str) -> Optional[Dict[str, Any]]:
    """根据用户ID获取用户"""
    try:
        query = "SELECT * FROM users WHERE id = :id"
        result = await database.fetch_one(query=query, values={"id": user_id})
        if result:
            user_dict = dict(result)
            # 转换 Decimal 类型为 float
            if user_dict.get('height') is not None and isinstance(user_dict['height'], Decimal):
                user_dict['height'] = float(user_dict['height'])
            if user_dict.get('weight') is not None and isinstance(user_dict['weight'], Decimal):
                user_dict['weight'] = float(user_dict['weight'])
            return user_dict
        return None
    except Exception as e:
        logger.error(f"Error in get_user_by_id for user_id {user_id}: {e}")
        raise

async def get_user_by_device_id(device_id: str) -> Optional[Dict[str, Any]]:
    """根据设备ID获取用户"""
    try:
        query = "SELECT * FROM users WHERE device_id = :device_id"
        result = await database.fetch_one(query=query, values={"device_id": device_id})
        if result:
            user_dict = dict(result)
            # 转换 Decimal 类型为 float
            if user_dict.get('height') is not None and isinstance(user_dict['height'], Decimal):
                user_dict['height'] = float(user_dict['height'])
            if user_dict.get('weight') is not None and isinstance(user_dict['weight'], Decimal):
                user_dict['weight'] = float(user_dict['weight'])
            return user_dict
        return None
    except Exception as e:
        logger.error(f"Error in get_user_by_device_id for device_id {device_id}: {e}")
        raise

async def create_user_with_device_id(
    device_id: str,
    diabetes_type: str,
    name: Optional[str] = None,
    email: Optional[str] = None,
    phone: Optional[str] = None,
    height: Optional[float] = None,
) -> str:
    """通过设备ID创建新用户"""
    try:
        user_id = str(uuid4())
        query = """
            INSERT INTO users (id, device_id, email, name, phone, diabetes_type, height, created_at, updated_at)
            VALUES (:id, :device_id, :email, :name, :phone, :diabetes_type, :height, :created_at, :updated_at)
            RETURNING id
        """
        values = {
            "id": user_id,
            "device_id": device_id,
            "email": email,
            "name": name,
            "phone": phone,
            "diabetes_type": diabetes_type,
            "height": height,
            "created_at": datetime.utcnow(),
            "updated_at": datetime.utcnow()
        }
        result = await database.fetch_one(query=query, values=values)
        return str(result["id"])
    except Exception as e:
        logger.error(f"Error in create_user_with_device_id for device_id {device_id}: {e}")
        raise

async def get_user_parameters(user_id: str) -> Optional[Dict[str, Any]]:
    """获取用户参数"""
    try:
        query = "SELECT * FROM user_parameters WHERE user_id = :user_id"
        result = await database.fetch_one(query=query, values={"user_id": user_id})
        if result:
            return dict(result)
        return None
    except Exception as e:
        logger.error(f"Error in get_user_parameters for user_id {user_id}: {e}")
        raise

async def create_user_parameters(user_id: str, params: Dict[str, Any]) -> str:
    """创建用户参数"""
    try:
        param_id = str(uuid4())
        query = """
            INSERT INTO user_parameters 
            (id, user_id, insulin_type, isf, icr, target_bg_low, target_bg_high, 
             max_insulin_dose, min_insulin_dose, created_at, updated_at)
            VALUES 
            (:id, :user_id, :insulin_type, :isf, :icr, :target_bg_low, :target_bg_high,
             :max_insulin_dose, :min_insulin_dose, :created_at, :updated_at)
            RETURNING id
        """
        values = {
            "id": param_id,
            "user_id": user_id,
            "insulin_type": params.get("insulin_type"),
            "isf": params.get("isf"),
            "icr": params.get("icr"),
            "target_bg_low": params.get("target_bg_low", 4.0),
            "target_bg_high": params.get("target_bg_high", 7.8),
            "max_insulin_dose": params.get("max_insulin_dose"),
            "min_insulin_dose": params.get("min_insulin_dose", 0.5),
            "created_at": datetime.utcnow(),
            "updated_at": datetime.utcnow()
        }
        result = await database.fetch_one(query=query, values=values)
        return str(result["id"])
    except Exception as e:
        logger.error(f"Error in create_user_parameters for user_id {user_id} with params {params}: {e}")
        raise

async def update_user_parameters(user_id: str, params: Dict[str, Any]) -> bool:
    """更新用户参数"""
    try:
        # 构建更新字段
        update_fields = []
        values = {"user_id": user_id, "updated_at": datetime.utcnow()}
        
        for key in ["insulin_type", "isf", "icr", "target_bg_low", "target_bg_high", 
                    "max_insulin_dose", "min_insulin_dose"]:
            if key in params:
                update_fields.append(f"{key} = :{key}")
                values[key] = params[key]
        
        if not update_fields:
            return False
        
        update_fields.append("updated_at = :updated_at")
        query = f"""
            UPDATE user_parameters 
            SET {', '.join(update_fields)}
            WHERE user_id = :user_id
        """
        await database.execute(query=query, values=values)
        return True
    except Exception as e:
        logger.error(f"Error in update_user_parameters for user_id {user_id} with params {params}: {e}")
        raise

async def update_user_info(
    user_id: str,
    height: Optional[float] = None,
    weight: Optional[float] = None,
    age: Optional[int] = None,
    gender: Optional[str] = None,
    diabetes_type: Optional[str] = None
):
    """更新用户基本信息"""
    update_fields = []
    values = {"user_id": user_id}
    
    if height is not None:
        update_fields.append("height = :height")
        values["height"] = height
    
    if weight is not None:
        update_fields.append("weight = :weight")
        values["weight"] = weight
    
    if age is not None:
        update_fields.append("age = :age")
        values["age"] = age
    
    if gender is not None:
        update_fields.append("gender = :gender")
        values["gender"] = gender
    
    if diabetes_type is not None:
        update_fields.append("diabetes_type = :diabetes_type")
        values["diabetes_type"] = diabetes_type
    
    if not update_fields:
        return
    
    query = f"""
        UPDATE users 
        SET {', '.join(update_fields)}
        WHERE id = :user_id
    """
    
    await database.execute(query=query, values=values)

