from fastapi import APIRouter, Depends, HTTPException, status, Query
from app.prediction import schemas
from app.prediction.service import PredictionService
from app.prediction.mcp_service import mcp_prediction_service
from app.user.router import get_current_user_dependency
from app.user.schemas import UserResponse
from app.utils.fastmcp_client import ask_health_question
from datetime import datetime
import logging

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="",
    tags=["prediction"]
)

def get_prediction_service() -> PredictionService:
    """获取血糖预测服务实例"""
    return PredictionService()

# 添加MCP增强的血糖预测路由
@router.post(
    "/blood-glucose/mcp-enhanced",
    response_model=schemas.BloodGlucosePredictionResponse,
    summary="使用MCP代理增强的血糖预测",
    description="""
    **使用AI代理增强的餐后血糖预测**
    
    ### 功能特性
    - 🤖 利用MCP代理提供更智能的血糖预测
    - 📈 预测多个时间点的血糖值
    - ⚠️ 风险评估和个性化建议
    - 💡 AI驱动的健康洞察
    
    ### 使用说明
    1. 提供与标准预测相同的参数
    2. 系统会调用MCP代理进行增强预测
    3. 返回更智能的预测结果和建议
    """,
    responses={
        200: {"description": "预测成功"},
        503: {"description": "MCP服务不可用，使用回退模型"}
    }
)
async def predict_blood_glucose_mcp_enhanced(
    request: schemas.BloodGlucosePredictionRequest,
    current_user: UserResponse = Depends(get_current_user_dependency),
    agent_name: str = Query(default="glucose_prediction_agent", description="要使用的MCP代理名称")
):
    """使用MCP代理进行增强的血糖预测"""
    try:
        result = await mcp_prediction_service.predict_with_agent(
            request=request,
            user_id=current_user.id,
            agent_name=agent_name
        )
        return result
    except Exception as e:
        logger.error(f"MCP增强预测失败: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="MCP服务暂时不可用"
        )

@router.post(
    "/health-insights",
    response_model=dict,
    summary="获取健康分析师代理的见解",
    description="""
    **向健康分析师AI代理提问获取个性化见解**
    
    ### 功能特性
    - 🧠 基于AI的健康咨询
    - 💬 支持自然语言查询
    - 📊 个性化的健康建议
    - 🔍 深度分析健康数据
    
    ### 使用说明
    1. 输入您的健康相关问题
    2. 系统将问题发送给健康分析师代理
    3. 返回代理提供的专业见解
    """
)
async def get_health_insights(
    query: schemas.HealthInsightQuery,
    current_user: UserResponse = Depends(get_current_user_dependency),
    agent_name: str = Query(default="health_analyst_agent", description="要使用的健康分析师代理名称")
):
    """获取健康分析师代理的见解"""
    try:
        result = await mcp_prediction_service.get_agent_insights(
            user_id=current_user.id,
            query=query.question,
            agent_name=agent_name
        )
        return result
    except Exception as e:
        logger.error(f"获取健康见解失败: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="健康分析服务暂时不可用"
        )

@router.post(
    "/blood-glucose",
    response_model=schemas.BloodGlucosePredictionResponse,
    summary="预测血糖",
    description="""
    **预测餐后血糖变化趋势**
    
    ### 功能特性
    - 📈 预测多个时间点的血糖值（30分钟、1小时、2小时、3小时等）
    - 📊 预测血糖峰值时间和高度
    - ⚠️ 风险评估
    - 💡 优化建议
    
    ### 使用说明
    1. 提供总碳水、胰岛素剂量、当前血糖值
    2. 可选提供GI值和活动水平
    3. 系统预测餐后血糖变化曲线
    4. 返回风险评估和优化建议
    """,
    responses={
        200: {
            "description": "预测成功",
            "content": {
                "application/json": {
                    "example": {
                        "predictions": [
                            {
                                "time_minutes": 30,
                                "bg_value": 8.5,
                                "confidence": 0.85
                            },
                            {
                                "time_minutes": 60,
                                "bg_value": 10.2,
                                "confidence": 0.90
                            },
                            {
                                "time_minutes": 120,
                                "bg_value": 8.5,
                                "confidence": 0.88
                            }
                        ],
                        "peak_time": 90,
                        "peak_value": 10.5,
                        "risk_level": "medium",
                        "recommendations": [
                            "预测血糖略高，建议适当增加胰岛素或增加运动",
                            "建议监测餐后2小时血糖，如超过目标范围需调整"
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
async def predict_blood_glucose(
    request: schemas.BloodGlucosePredictionRequest,
    insulin_record_id: str = Query(None, description="胰岛素记录ID（可选）"),
    nutrition_record_id: str = Query(None, description="营养成分记录ID（可选）"),
    prediction_service: PredictionService = Depends(get_prediction_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """
    预测血糖 API
    
    基于食物信息、胰岛素剂量、个人历史数据，预测餐后血糖变化趋势。
    
    返回多个时间点的预测值，包括：
    - 餐后30分钟、1小时、2小时、3小时、4小时
    - 血糖峰值时间和高度
    - 风险评估
    - 优化建议
    """
    try:
        logger.info(f"📈 血糖预测请求: user={current_user.id}, carbs={request.total_carbs}g, dose={request.insulin_dose}")
        
        result = await prediction_service.predict_blood_glucose(
            request=request,
            user_id=current_user.id,
            insulin_record_id=insulin_record_id,
            nutrition_record_id=nutrition_record_id
        )
        
        return result
        
    except Exception as e:
        logger.error(f"Blood glucose prediction error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"预测过程中发生错误: {str(e)}"
        )

@router.post("/health-insight", response_model=dict)
async def get_health_insight(
    query: schemas.HealthInsightQuery,
    current_user: UserResponse = Depends(get_current_user_dependency)
) -> dict:
    """
    获取健康见解和建议
    
    - **question**: 健康咨询问题（5-1000字符）
    - **context**: 可选的额外上下文信息
    """
    try:
        logger.info(f"用户 {current_user.id} 请求健康咨询: {query.question}")
        
        # 调用MCP客户端获取健康见解
        response = await ask_health_question(
            question=query.question,
            context=query.context
        )
        
        # 添加用户ID和时间戳到响应中
        response.update({
            "user_id": current_user.id,
            "timestamp": datetime.utcnow().isoformat() + "Z"
        })
        
        logger.info(f"为用户 {current_user.id} 获取健康见解成功")
        return response
        
    except Exception as e:
        logger.error(f"获取健康见解失败: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail="获取健康见解失败，请稍后再试"
        )
