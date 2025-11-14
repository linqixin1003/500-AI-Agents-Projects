from fastapi import APIRouter, Depends, HTTPException, status, Query
from typing import Optional
from app.nutrition import schemas
from app.nutrition.service import NutritionService
from app.nutrition.daily_recommendation import DailyNutritionRecommendation
from app.user.router import get_current_user_dependency
from app.user.schemas import UserResponse
from app.user import crud as user_crud
from app.records import crud as records_crud
import logging

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="",
    tags=["nutrition"]
)

def get_nutrition_service() -> NutritionService:
    """获取营养成分计算服务实例"""
    return NutritionService()

def get_daily_recommendation() -> DailyNutritionRecommendation:
    """获取每日推荐营养计算器实例"""
    return DailyNutritionRecommendation()

@router.post(
    "/calculate",
    response_model=schemas.NutritionCalculationResponse,
    summary="计算营养成分",
    description="""
    **基于食物信息计算营养成分**
    
    ### 功能特性
    - 📊 自动计算总碳水、净碳水、蛋白质、脂肪、纤维、热量
    - 📈 计算升糖指数（GI）和血糖负荷（GL）
    - 🍳 考虑烹饪方式对营养成分的影响
    - 📝 提供详细的计算分解
    
    ### 使用说明
    1. 提供食物列表（名称、重量、烹饪方式）
    2. 系统自动查询营养成分数据库
    3. 计算并返回详细的营养成分信息
    """,
    responses={
        200: {
            "description": "计算成功",
            "content": {
                "application/json": {
                    "example": {
                        "total_carbs": 45.0,
                        "net_carbs": 42.0,
                        "protein": 8.5,
                        "fat": 12.0,
                        "fiber": 3.0,
                        "calories": 320.0,
                        "gi_value": 65.0,
                        "gl_value": 29.25,
                        "calculation_details": [
                            {
                                "name": "白米饭",
                                "weight": 200.0,
                                "carbs": 51.8,
                                "net_carbs": 51.5,
                                "protein": 5.2,
                                "fat": 0.6,
                                "fiber": 0.6,
                                "calories": 232.0,
                                "gi_value": 83.0
                            }
                        ]
                    }
                }
            }
        },
        400: {
            "description": "请求参数错误"
        },
        401: {
            "description": "未授权"
        }
    }
)
async def calculate_nutrition(
    request: schemas.NutritionCalculationRequest,
    nutrition_service: NutritionService = Depends(get_nutrition_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
    food_recognition_id: Optional[str] = Query(None, description="食物识别记录ID（可选）"),
):
    """
    计算营养成分 API
    
    基于提供的食物列表，计算总体的营养成分，包括：
    - 碳水化合物（总碳水、净碳水）
    - 蛋白质、脂肪、纤维
    - 总热量
    - 升糖指数（GI）和血糖负荷（GL）
    """
    try:
        logger.info(f"📊 营养成分计算请求: user={current_user.id}, foods={len(request.foods)}")
        
        result = await nutrition_service.calculate_nutrition(
            request=request,
            user_id=current_user.id,
            food_recognition_id=food_recognition_id
        )
        
        return result
        
    except Exception as e:
        logger.error(f"Nutrition calculation error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"计算过程中发生错误: {str(e)}"
        )

@router.get(
    "/daily-recommendation",
    summary="获取每日推荐营养摄入",
    description="根据用户信息计算每日推荐营养摄入量"
)
async def get_daily_recommendation(
    daily_recommendation: DailyNutritionRecommendation = Depends(get_daily_recommendation),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取每日推荐营养摄入"""
    try:
        user_info = await user_crud.get_user_by_id(current_user.id) or {}
        user_params = await user_crud.get_user_parameters(current_user.id)
        
        recommendation = daily_recommendation.calculate_daily_recommendation(
            user_info=user_info,
            user_params=user_params
        )
        
        return recommendation
    except Exception as e:
        logger.error(f"获取每日推荐营养失败: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取推荐营养失败: {str(e)}"
        )

@router.get(
    "/today-intake",
    summary="获取今日营养摄入",
    description="获取用户今日已摄入的营养统计"
)
async def get_today_intake(
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取今日营养摄入统计"""
    try:
        from datetime import datetime
        intake = await records_crud.get_today_nutrition_intake(current_user.id, datetime.utcnow())
        return intake
    except Exception as e:
        logger.error(f"获取今日营养摄入失败: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取今日营养摄入失败: {str(e)}"
        )
