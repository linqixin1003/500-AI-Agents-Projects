from fastapi import APIRouter, Depends, HTTPException, status, Query
from fastapi.security import OAuth2PasswordBearer
from app.user import schemas, crud, auth_service
from app.user.schemas import UserResponse
from app.user.auth_service import get_current_user
import logging

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="",
    tags=["users"]
)

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/users/device-auth") # 更新 Token URL

async def get_current_user_dependency(token: str = Depends(oauth2_scheme)) -> UserResponse:
    """获取当前用户的依赖"""
    return await get_current_user(token)

@router.post("/device-auth", response_model=schemas.TokenResponse, status_code=status.HTTP_200_OK)
async def device_auth(request: schemas.DeviceAuthRequest):
    """设备认证 (注册或登录)"""
    user = await crud.get_user_by_device_id(request.device_id)
    
    if not user:
        # 如果用户不存在，则注册新用户
        if not request.diabetes_type:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="新设备注册时必须提供 diabetes_type"
            )
        valid_types = ["type1", "type2", "gestational", "prediabetes"]
        if request.diabetes_type not in valid_types:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"无效的 diabetes_type. 必须是: {', '.join(valid_types)}"
            )
        
        user_id = await crud.create_user_with_device_id(
            device_id=request.device_id,
            diabetes_type=request.diabetes_type,
            name=request.name,
            height=request.height
        )
        user = await crud.get_user_by_id(user_id)
        if not user:
            raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="用户创建失败")
            
    user_response = schemas.UserResponse(
        id=str(user["id"]),
        device_id=user["device_id"],
        email=user.get("email"),
        name=user.get("name"),
        diabetes_type=user["diabetes_type"],
        height=user.get("height"),
        created_at=user["created_at"]
    )
    access_token = auth_service.create_access_token(data={"sub": str(user["id"])})
    return schemas.TokenResponse(
        access_token=access_token,
        token_type="bearer",
        user=user_response
    )

@router.post(
    "/complete-onboarding",
    summary="完成引导页，保存用户基本信息",
    description="保存身高、体重、年龄、性别等信息，使用AI计算每日营养建议"
)
async def complete_onboarding(
    height: float = Query(..., ge=50, le=250, description="身高(cm)"),
    weight: float = Query(..., ge=20, le=300, description="体重(kg)"),
    age: int = Query(..., ge=1, le=120, description="年龄"),
    gender: str = Query(..., description="性别 (male/female/other)"),
    diabetes_type: str = Query(None, description="糖尿病类型"),
    current_user: UserResponse = Depends(get_current_user_dependency)
):
    """完成引导页，保存用户信息并通过AI返回营养建议"""
    try:
        logger.info(f"📋 完成引导页: user={current_user.id}, height={height}, weight={weight}, age={age}, gender={gender}")
        
        # 更新用户信息
        await crud.update_user_info(
            user_id=current_user.id,
            height=height,
            weight=weight,
            age=age,
            gender=gender,
            diabetes_type=diabetes_type or current_user.diabetes_type
        )
        
        # 使用AI计算每日营养建议
        from app.nutrition.daily_recommendation import DailyNutritionRecommendation
        
        user_info = {
            "gender": gender,
            "age": age,
            "height": height,
            "weight": weight,
            "diabetes_type": diabetes_type or current_user.diabetes_type,
            "activity_level": "moderate"  # 默认中等活动量
        }
        
        daily_rec = DailyNutritionRecommendation()
        
        # 优先使用AI生成，失败则使用传统计算
        recommendation = daily_rec.calculate_daily_recommendation_with_ai(user_info)
        
        logger.info(f"✅ 引导完成: user={current_user.id}, 每日热量={recommendation.get('daily_calories')}kcal")
        
        return {
            "message": "引导完成，已保存用户信息",
            "user_info": {
                "height": height,
                "weight": weight,
                "age": age,
                "gender": gender,
                "diabetes_type": diabetes_type or current_user.diabetes_type
            },
            "daily_recommendation": recommendation
        }
        
    except Exception as e:
        logger.error(f"Complete onboarding error: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"保存用户信息失败: {str(e)}"
        )

@router.get("/me", response_model=schemas.UserResponse)
async def get_current_user_info(current_user: UserResponse = Depends(get_current_user_dependency)):
    """获取当前用户信息"""
    return current_user

@router.get("/{user_id}/parameters", response_model=schemas.UserParameterResponse)
async def get_user_parameters(
    user_id: str,
    current_user: UserResponse = Depends(get_current_user_dependency)
):
    """获取用户参数"""
    # 验证用户只能查看自己的参数
    if current_user.id != user_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized to access this resource"
        )
    
    params = await crud.get_user_parameters(user_id)
    if not params:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User parameters not found"
        )
    
    # 确保 id 和 user_id 是字符串类型
    params["id"] = str(params["id"])
    params["user_id"] = str(params["user_id"])
    return schemas.UserParameterResponse(**params)

@router.post("/{user_id}/parameters", response_model=schemas.UserParameterResponse, status_code=status.HTTP_201_CREATED)
async def create_user_parameters(
    user_id: str,
    params: schemas.UserParameterCreate,
    current_user: UserResponse = Depends(get_current_user_dependency)
):
    """创建用户参数"""
    # 验证用户只能创建自己的参数
    if current_user.id != user_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized to access this resource"
        )
    
    # 检查是否已存在
    existing = await crud.get_user_parameters(user_id)
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="User parameters already exist. Use PUT to update."
        )
    
    param_id = await crud.create_user_parameters(user_id, params.dict())
    params_data = await crud.get_user_parameters(user_id)
    # 确保 id 和 user_id 是字符串类型
    params_data["id"] = str(params_data["id"])
    params_data["user_id"] = str(params_data["user_id"])
    return schemas.UserParameterResponse(**params_data)

@router.put("/{user_id}/parameters", response_model=schemas.UserParameterResponse)
async def update_user_parameters(
    user_id: str,
    params: schemas.UserParameterCreate,
    current_user: UserResponse = Depends(get_current_user_dependency)
):
    """更新用户参数"""
    # 验证用户只能更新自己的参数
    if current_user.id != user_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Not authorized to access this resource"
        )
    
    # 检查是否存在
    existing = await crud.get_user_parameters(user_id)
    if not existing:
        # 如果不存在，创建新的
        await crud.create_user_parameters(user_id, params.dict())
    else:
        # 更新现有参数
        await crud.update_user_parameters(user_id, params.dict())
    
    params_data = await crud.get_user_parameters(user_id)
    # 确保 id 和 user_id 是字符串类型
    params_data["id"] = str(params_data["id"])
    params_data["user_id"] = str(params_data["user_id"])
    return schemas.UserParameterResponse(**params_data)

