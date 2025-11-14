from typing import List
from fastapi import APIRouter, Depends, UploadFile, File, HTTPException, status, Query
from app.food import schemas, service
from app.food.service import FoodService
from app.storage.base import StorageProvider
from app.storage.local import LocalStorageProvider
from app.user.router import get_current_user_dependency
from app.user.schemas import UserResponse
from app.config import settings
import logging

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="",
    tags=["food"]
)

def get_food_service() -> FoodService:
    """获取食物识别服务实例"""
    # 根据配置选择存储方式
    storage: StorageProvider = LocalStorageProvider()
    
    # 如果配置了 AWS，可以使用 S3 存储
    if settings.AWS_S3_BUCKET and settings.AWS_ACCESS_KEY_ID:
        from app.storage.s3 import S3StorageProvider
        storage = S3StorageProvider(
            bucket_name=settings.AWS_S3_BUCKET,
            aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
            region=settings.AWS_REGION
        )
    
    return FoodService(storage=storage)

@router.post(
    "/recognize",
    response_model=schemas.FoodRecognitionResponse,
    summary="识别食物图片",
    description="""
    **上传食物图片进行AI识别**
    
    ### 功能特性
    - 🤖 AI 识别食物种类
    - 📊 估算食物分量（重量）
    - 🎯 识别置信度
    - 🍽️ 支持混合菜品识别
    
    ### 支持的图片格式
    - JPG/JPEG
    - PNG
    - WEBP
    
    ### 使用说明
    1. 上传食物照片
    2. AI 自动识别食物种类和分量
    3. 返回识别结果，包括食物名称、重量估算、置信度
    """,
    responses={
        200: {
            "description": "识别成功",
            "content": {
                "application/json": {
                    "example": {
                        "recognition_id": "123e4567-e89b-12d3-a456-426614174000",
                        "foods": [
                            {
                                "name": "白米饭",
                                "weight": 200.0,
                                "confidence": 0.95,
                                "cooking_method": "steamed"
                            },
                            {
                                "name": "红烧肉",
                                "weight": 120.0,
                                "confidence": 0.90,
                                "cooking_method": "braised"
                            }
                        ],
                        "total_confidence": 0.92,
                        "image_url": "http://localhost:8000/static/food/user123/image.jpg"
                    }
                }
            }
        },
        400: {
            "description": "请求参数错误",
            "content": {
                "application/json": {
                    "example": {
                        "detail": "Invalid image format"
                    }
                }
            }
        },
        401: {
            "description": "未授权"
        },
        500: {
            "description": "服务器内部错误"
        }
    }
)
async def recognize_food(
    image: UploadFile = File(..., description="食物图片文件（支持 JPG, PNG, WEBP 等格式）"),
    food_service: FoodService = Depends(get_food_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """
    识别食物图片 API
    
    上传食物图片，AI 自动识别食物种类、估算分量，并返回识别结果。
    """
    try:
        # 验证文件类型
        if not image.content_type or not image.content_type.startswith('image/'):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="File must be an image"
            )
        
        logger.info(f"🔍 食物识别请求: user={current_user.id}, file={image.filename}")
        
        # 调用识别服务
        result = await food_service.recognize_food(
            image=image,
            user_id=current_user.id
        )
        
        return result
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Food recognition error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"识别过程中发生错误: {str(e)}"
        )

@router.get(
    "/search",
    response_model=List[schemas.FoodItem],
    summary="搜索食物",
    description="""
    **根据关键词搜索食物**
    
    ### 功能特性
    - 🔍 根据食物名称进行模糊匹配搜索
    - 📊 返回食物名称和卡路里信息
    
    ### 使用说明
    1. 提供食物名称关键词
    2. 返回匹配的食物列表，包括名称和卡路里
    """
)
async def search_foods(
    query: str = Query(..., min_length=1, description="食物名称关键词"),
    food_service: FoodService = Depends(get_food_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    logger.info(f"🔍 食物搜索请求: user={current_user.id}, query={query}")
    try:
        results = await food_service.search_foods(query)
        return results
    except Exception as e:
        logger.error(f"Food search error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"搜索过程中发生错误: {str(e)}"
        )

