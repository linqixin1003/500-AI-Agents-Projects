from fastapi import APIRouter, Depends, HTTPException, status, Query
from app.insulin import schemas
from app.insulin.service import InsulinService
from app.user.router import get_current_user_dependency
from app.user.schemas import UserResponse
import logging

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="",
    tags=["insulin"]
)

def get_insulin_service() -> InsulinService:
    """获取胰岛素计算服务实例"""
    return InsulinService()

@router.post(
    "/calculate",
    response_model=schemas.InsulinCalculationResponse,
    summary="计算胰岛素剂量",
    description="""
    **基于多因素智能计算胰岛素剂量**
    
    ### 功能特性
    - 💉 基于碳水含量计算基础剂量
    - 📊 考虑当前血糖值进行校正
    - 🏃 考虑活动水平调整剂量
    - ⏰ 考虑时间因素（昼夜节律）
    - 📈 考虑食物GI值
    - ⚠️ 安全限制和风险评估
    
    ### 使用说明
    1. 提供总碳水含量和当前血糖值
    2. 系统基于用户参数（ISF、ICR）计算
    3. 考虑活动水平、时间、GI值等因素
    4. 返回建议剂量和安全警告
    """,
    responses={
        200: {
            "description": "计算成功",
            "content": {
                "application/json": {
                    "example": {
                        "recommended_dose": 5.2,
                        "carb_insulin": 4.5,
                        "correction_insulin": 1.0,
                        "activity_adjustment": -0.3,
                        "injection_timing": "餐前15分钟",
                        "split_dose": False,
                        "risk_level": "low",
                        "warnings": []
                    }
                }
            }
        },
        400: {
            "description": "请求参数错误或用户参数未设置"
        },
        401: {
            "description": "未授权"
        }
    }
)
async def calculate_insulin(
    request: schemas.InsulinCalculationRequest,
    nutrition_record_id: str = Query(None, description="营养成分记录ID（可选）"),
    gi_value: float = Query(None, description="升糖指数（可选）"),
    insulin_service: InsulinService = Depends(get_insulin_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """
    计算胰岛素剂量 API
    
    基于多因素智能计算胰岛素剂量，包括：
    - 碳水胰岛素（基于ICR）
    - 血糖校正胰岛素（基于ISF）
    - 活动水平调整
    - 时间因子调整
    - GI值调整
    - 安全限制检查
    """
    try:
        logger.info(f"💉 胰岛素计算请求: user={current_user.id}, carbs={request.total_carbs}g, bg={request.current_bg}")
        
        result = await insulin_service.calculate_insulin_dose(
            request=request,
            user_id=current_user.id,
            nutrition_record_id=nutrition_record_id,
            gi_value=gi_value
        )
        
        return result
        
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        logger.error(f"Insulin calculation error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"计算过程中发生错误: {str(e)}"
        )
