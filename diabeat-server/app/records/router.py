from fastapi import APIRouter, Depends, HTTPException, status, Query
from typing import List, Optional
from datetime import datetime
from app.records import schemas
from app.records.service import RecordService
from app.user.router import get_current_user_dependency
from app.user.schemas import UserResponse
import logging

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="",
    tags=["records"]
)

def get_record_service() -> RecordService:
    """获取记录服务实例"""
    return RecordService()

@router.post(
    "/meals",
    response_model=schemas.MealRecordResponse,
    status_code=status.HTTP_201_CREATED,
    summary="记录用餐时间",
    description="记录用户的用餐时间和相关信息"
)
async def create_meal_record(
    request: schemas.MealRecordCreate,
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """创建用餐记录"""
    try:
        logger.info(f"🍽️ 记录用餐: user={current_user.id}, time={request.meal_time}")
        return await record_service.create_meal_record(request, current_user.id)
    except Exception as e:
        logger.error(f"Create meal record error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"创建用餐记录失败: {str(e)}"
        )

@router.post(
    "/insulin",
    response_model=schemas.InsulinRecordResponse,
    status_code=status.HTTP_201_CREATED,
    summary="记录打胰岛素时间",
    description="记录用户的胰岛素注射时间和剂量"
)
async def create_insulin_record(
    request: schemas.InsulinRecordCreate,
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """创建胰岛素注射记录"""
    try:
        logger.info(f"💉 记录胰岛素注射: user={current_user.id}, time={request.injection_time}, dose={request.actual_dose}")
        return await record_service.create_insulin_record(request, current_user.id)
    except Exception as e:
        logger.error(f"Create insulin record error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"创建胰岛素记录失败: {str(e)}"
        )

@router.get(
    "/meals",
    response_model=List[schemas.MealRecordResponse],
    summary="获取用餐记录",
    description="获取用户的用餐记录列表"
)
async def get_meal_records(
    limit: int = Query(10, ge=1, le=100, description="返回记录数量"),
    date: Optional[str] = Query(None, description="日期过滤（YYYY-MM-DD格式）"),
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取用餐记录"""
    try:
        from app.records import crud
        from datetime import datetime
        
        if date:
            # 如果指定了日期，获取该日期的记录
            try:
                target_date = datetime.strptime(date, "%Y-%m-%d")
                start_date = target_date.replace(hour=0, minute=0, second=0, microsecond=0)
                end_date = target_date.replace(hour=23, minute=59, second=59, microsecond=999999)
                records = await crud.get_meal_history(
                    current_user.id,
                    start_date=start_date,
                    end_date=end_date,
                    limit=limit
                )
            except ValueError:
                # 日期格式错误，使用默认查询
                records = await crud.get_recent_meal_records(current_user.id, limit=limit)
        else:
            records = await crud.get_recent_meal_records(current_user.id, limit=limit)
        
        return [
            schemas.MealRecordResponse(
                id=str(r["id"]),
                user_id=str(r["user_id"]),
                meal_time=r["meal_time"],
                food_recognition_id=str(r.get("food_recognition_id")) if r.get("food_recognition_id") else None,
                nutrition_record_id=str(r.get("nutrition_record_id")) if r.get("nutrition_record_id") else None,
                notes=r.get("notes"),
                created_at=r["created_at"]
            )
            for r in records
        ]
    except Exception as e:
        logger.error(f"Get meal records error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取用餐记录失败: {str(e)}"
        )

@router.get(
    "/insulin",
    response_model=List[schemas.InsulinRecordResponse],
    summary="获取胰岛素记录",
    description="获取用户的胰岛素注射记录列表"
)
async def get_insulin_records(
    limit: int = Query(10, ge=1, le=100, description="返回记录数量"),
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取胰岛素注射记录"""
    try:
        from app.records import crud
        records = await crud.get_recent_insulin_records(current_user.id, limit=limit)
        return [
            schemas.InsulinRecordResponse(
                id=str(r["id"]),
                user_id=str(r["user_id"]),
                injection_time=r["injection_time"],
                insulin_record_id=r.get("insulin_record_id"),
                actual_dose=r["actual_dose"],
                notes=r.get("notes"),
                created_at=r["created_at"]
            )
            for r in records
        ]
    except Exception as e:
        logger.error(f"Get insulin records error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取胰岛素记录失败: {str(e)}"
        )

@router.get(
    "/predict-next-insulin",
    response_model=schemas.NextInsulinPredictionResponse,
    summary="预测下次打胰岛素时间",
    description="基于历史用餐和注射模式，预测下次需要打胰岛素的时间"
)
async def predict_next_insulin(
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """预测下次胰岛素注射时间"""
    try:
        logger.info(f"🔮 预测下次胰岛素时间: user={current_user.id}")
        result = await record_service.predict_next_insulin_time(current_user.id)
        
        # 安排通知（异步）
        from app.notification.service import NotificationService
        notification_service = NotificationService()
        await notification_service.schedule_insulin_reminder(
            user_id=current_user.id,
            reminder_time=result.predicted_time
        )
        result.notification_scheduled = True
        
        return result
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        logger.error(f"Predict next insulin error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"预测失败: {str(e)}"
        )

@router.get(
    "/meals/history",
    response_model=List[schemas.MealHistoryItem],
    summary="获取饮食历史记录",
    description="获取用户的饮食历史记录，支持日期范围查询，包含营养信息"
)
async def get_meal_history(
    start_date: Optional[str] = Query(None, description="开始日期（YYYY-MM-DD格式）"),
    end_date: Optional[str] = Query(None, description="结束日期（YYYY-MM-DD格式）"),
    limit: int = Query(100, ge=1, le=500, description="返回记录数量"),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取饮食历史记录（包含营养信息）"""
    try:
        from app.records import crud
        
        start_dt = None
        end_dt = None
        
        if start_date:
            try:
                start_dt = datetime.strptime(start_date, "%Y-%m-%d")
                start_dt = start_dt.replace(hour=0, minute=0, second=0, microsecond=0)
            except ValueError:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="开始日期格式错误，请使用 YYYY-MM-DD 格式"
                )
        
        if end_date:
            try:
                end_dt = datetime.strptime(end_date, "%Y-%m-%d")
                end_dt = end_dt.replace(hour=23, minute=59, second=59, microsecond=999999)
            except ValueError:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="结束日期格式错误，请使用 YYYY-MM-DD 格式"
                )
        
        records = await crud.get_meal_history(
            user_id=current_user.id,
            start_date=start_dt,
            end_date=end_dt,
            limit=limit
        )
        
        return [
            schemas.MealHistoryItem(
                id=str(r["id"]),
                user_id=str(r["user_id"]),
                meal_time=r["meal_time"],
                food_recognition_id=r.get("food_recognition_id"),
                nutrition_record_id=r.get("nutrition_record_id"),
                notes=r.get("notes"),
                created_at=r["created_at"],
                total_carbs=float(r["total_carbs"]) if r.get("total_carbs") else None,
                net_carbs=float(r["net_carbs"]) if r.get("net_carbs") else None,
                protein=float(r["protein"]) if r.get("protein") else None,
                fat=float(r["fat"]) if r.get("fat") else None,
                fiber=float(r["fiber"]) if r.get("fiber") else None,
                calories=float(r["calories"]) if r.get("calories") else None,
                gi_value=float(r["gi_value"]) if r.get("gi_value") else None,
                gl_value=float(r["gl_value"]) if r.get("gl_value") else None,
                image_url=r.get("image_url"),
                recognition_result=r.get("recognition_result")
            )
            for r in records
        ]
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Get meal history error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取饮食历史失败: {str(e)}"
        )


# ==================== 运动记录相关 ====================

@router.post(
    "/exercises",
    response_model=schemas.ExerciseRecordResponse,
    status_code=status.HTTP_201_CREATED,
    summary="记录运动",
    description="记录用户的运动信息，系统会自动估算消耗热量"
)
async def create_exercise_record(
    request: schemas.ExerciseRecordCreate,
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """创建运动记录"""
    try:
        logger.info(f"🏃 记录运动: user={current_user.id}, type={request.exercise_type}, duration={request.duration_minutes}min")
        return await record_service.create_exercise_record(request, current_user.id)
    except Exception as e:
        logger.error(f"Create exercise record error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"创建运动记录失败: {str(e)}"
        )

@router.get(
    "/exercises/today",
    response_model=schemas.TodayExerciseSummary,
    summary="获取今日运动汇总",
    description="获取用户今日运动的总消耗热量、总时长等汇总信息"
)
async def get_today_exercise_summary(
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取今日运动汇总"""
    try:
        logger.info(f"📊 获取今日运动汇总: user={current_user.id}")
        return await record_service.get_today_exercise_summary(current_user.id)
    except Exception as e:
        logger.error(f"Get today exercise summary error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取今日运动汇总失败: {str(e)}"
        )

@router.get(
    "/exercises",
    response_model=List[schemas.ExerciseRecordResponse],
    summary="获取运动记录列表",
    description="获取用户的运动记录历史"
)
async def get_exercise_records(
    start_date: Optional[str] = Query(None, description="开始日期 (ISO格式)"),
    end_date: Optional[str] = Query(None, description="结束日期 (ISO格式)"),
    limit: int = Query(50, ge=1, le=100, description="返回记录数量"),
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取运动记录列表"""
    try:
        logger.info(f"📋 获取运动记录: user={current_user.id}")
        
        start_dt = datetime.fromisoformat(start_date) if start_date else None
        end_dt = datetime.fromisoformat(end_date) if end_date else None
        
        return await record_service.get_exercise_records(
            current_user.id,
            start_date=start_dt,
            end_date=end_dt,
            limit=limit
        )
    except Exception as e:
        logger.error(f"Get exercise records error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取运动记录失败: {str(e)}"
        )

# ==================== 水分记录相关 ====================

@router.post(
    "/water",
    response_model=schemas.WaterRecordResponse,
    status_code=status.HTTP_201_CREATED,
    summary="记录水分摄入",
    description="记录用户的水分摄入，支持快捷记录不同容量"
)
async def create_water_record(
    request: schemas.WaterRecordCreate,
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """创建水分记录"""
    try:
        logger.info(f"💧 记录水分摄入: user={current_user.id}, amount={request.amount_ml}ml")
        return await record_service.create_water_record(request, current_user.id)
    except Exception as e:
        logger.error(f"Create water record error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"创建水分记录失败: {str(e)}"
        )

@router.get(
    "/water/today",
    response_model=schemas.TodayWaterSummary,
    summary="获取今日水分摄入汇总",
    description="获取用户今日水分摄入总量、完成进度等信息"
)
async def get_today_water_summary(
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取今日水分摄入汇总"""
    try:
        logger.info(f"📊 获取今日水分汇总: user={current_user.id}")
        return await record_service.get_today_water_summary(current_user.id)
    except Exception as e:
        logger.error(f"Get today water summary error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取今日水分汇总失败: {str(e)}"
        )

@router.get(
    "/water",
    response_model=List[schemas.WaterRecordResponse],
    summary="获取水分记录列表",
    description="获取用户的水分摄入历史记录"
)
async def get_water_records(
    start_date: Optional[str] = Query(None, description="开始日期 (ISO格式)"),
    end_date: Optional[str] = Query(None, description="结束日期 (ISO格式)"),
    limit: int = Query(50, ge=1, le=100, description="返回记录数量"),
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取水分记录列表"""
    try:
        logger.info(f"📋 获取水分记录: user={current_user.id}")
        
        start_dt = datetime.fromisoformat(start_date) if start_date else None
        end_dt = datetime.fromisoformat(end_date) if end_date else None
        
        return await record_service.get_water_records(
            current_user.id,
            start_date=start_dt,
            end_date=end_dt,
            limit=limit
        )
    except Exception as e:
        logger.error(f"Get water records error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取水分记录失败: {str(e)}"
        )

# ==================== 用药记录相关 ====================

@router.post(
    "/medications",
    response_model=schemas.MedicationRecordResponse,
    status_code=status.HTTP_201_CREATED,
    summary="记录用药",
    description="记录用户的用药信息，支持胰岛素和口服药"
)
async def create_medication_record(
    request: schemas.MedicationRecordCreate,
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """创建用药记录"""
    try:
        logger.info(f"💊 记录用药: user={current_user.id}, type={request.medication_type}, name={request.medication_name}")
        return await record_service.create_medication_record(request, current_user.id)
    except Exception as e:
        logger.error(f"Create medication record error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"创建用药记录失败: {str(e)}"
        )

@router.get(
    "/medications/today",
    response_model=schemas.TodayMedicationSummary,
    summary="获取今日用药汇总",
    description="获取用户今日用药的总次数、分类统计等汇总信息"
)
async def get_today_medication_summary(
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取今日用药汇总"""
    try:
        logger.info(f"📊 获取今日用药汇总: user={current_user.id}")
        return await record_service.get_today_medication_summary(current_user.id)
    except Exception as e:
        logger.error(f"Get today medication summary error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取今日用药汇总失败: {str(e)}"
        )

@router.get(
    "/medications",
    response_model=List[schemas.MedicationRecordResponse],
    summary="获取用药记录列表",
    description="获取用户的用药历史记录"
)
async def get_medication_records(
    start_date: Optional[str] = Query(None, description="开始日期 (ISO格式)"),
    end_date: Optional[str] = Query(None, description="结束日期 (ISO格式)"),
    limit: int = Query(50, ge=1, le=100, description="返回记录数量"),
    record_service: RecordService = Depends(get_record_service),
    current_user: UserResponse = Depends(get_current_user_dependency),
):
    """获取用药记录列表"""
    try:
        logger.info(f"📋 获取用药记录: user={current_user.id}")
        
        start_dt = datetime.fromisoformat(start_date) if start_date else None
        end_dt = datetime.fromisoformat(end_date) if end_date else None
        
        return await record_service.get_medication_records(
            current_user.id,
            start_date=start_dt,
            end_date=end_dt,
            limit=limit
        )
    except Exception as e:
        logger.error(f"Get medication records error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"获取用药记录失败: {str(e)}"
        )
