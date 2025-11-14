from typing import Optional, Dict, Any, List
from datetime import datetime, timedelta
from app.records import schemas, crud
from app.user import crud as user_crud
from app.database import database
import logging

logger = logging.getLogger(__name__)

class RecordService:
    """记录服务"""
    
    async def create_meal_record(
        self,
        request: schemas.MealRecordCreate,
        user_id: str
    ) -> schemas.MealRecordResponse:
        """创建用餐记录"""
        record_id = await crud.create_meal_record(
            user_id=user_id,
            meal_time=request.meal_time,
            food_recognition_id=request.food_recognition_id,
            nutrition_record_id=request.nutrition_record_id,
            notes=request.notes
        )
        
        # 获取创建的记录
        records = await crud.get_recent_meal_records(user_id, limit=1)
        if records:
            record = records[0]
            return schemas.MealRecordResponse(
                id=str(record["id"]),
                user_id=str(record["user_id"]),
                meal_time=record["meal_time"],
                food_recognition_id=str(record.get("food_recognition_id")) if record.get("food_recognition_id") else None,
                nutrition_record_id=str(record.get("nutrition_record_id")) if record.get("nutrition_record_id") else None,
                notes=record.get("notes"),
                created_at=record["created_at"]
            )
        raise Exception("Failed to create meal record")
    
    async def create_insulin_record(
        self,
        request: schemas.InsulinRecordCreate,
        user_id: str
    ) -> schemas.InsulinRecordResponse:
        """创建胰岛素注射记录"""
        record_id = await crud.create_insulin_injection_record(
            user_id=user_id,
            injection_time=request.injection_time,
            actual_dose=request.actual_dose,
            insulin_record_id=request.insulin_record_id,
            notes=request.notes
        )
        
        # 获取创建的记录
        records = await crud.get_recent_insulin_records(user_id, limit=1)
        if records:
            record = records[0]
            return schemas.InsulinRecordResponse(
                id=str(record["id"]),
                user_id=str(record["user_id"]),
                injection_time=record["injection_time"],
                insulin_record_id=record.get("insulin_record_id"),
                actual_dose=record["actual_dose"],
                notes=record.get("notes"),
                created_at=record["created_at"]
            )
        raise Exception("Failed to create insulin record")
    
    async def predict_next_insulin_time(
        self,
        user_id: str
    ) -> schemas.NextInsulinPredictionResponse:
        """预测下次胰岛素注射时间"""
        # 获取用户参数
        user_params = await user_crud.get_user_parameters(user_id)
        if not user_params:
            raise ValueError("用户参数未设置")
        
        # 获取最近的用餐和注射记录
        last_meal_time = await crud.get_last_meal_time(user_id)
        last_insulin_time = await crud.get_last_insulin_time(user_id)
        
        # 分析用餐模式
        meal_pattern = await crud.get_user_meal_pattern(user_id, days=7)
        
        if not last_meal_time:
            # 如果没有用餐记录，基于常规用餐时间预测
            now = datetime.utcnow()
            current_hour = now.hour
            
            # 预测下次用餐时间（基于常规时间）
            if current_hour < 8:
                # 早餐时间（7-8点）
                predicted_time = now.replace(hour=7, minute=30, second=0, microsecond=0)
                if predicted_time < now:
                    predicted_time += timedelta(days=1)
                reasoning = "基于常规早餐时间预测"
            elif current_hour < 12:
                # 午餐时间（12-13点）
                predicted_time = now.replace(hour=12, minute=30, second=0, microsecond=0)
                if predicted_time < now:
                    predicted_time += timedelta(days=1)
                reasoning = "基于常规午餐时间预测"
            elif current_hour < 19:
                # 晚餐时间（18-19点）
                predicted_time = now.replace(hour=18, minute=30, second=0, microsecond=0)
                if predicted_time < now:
                    predicted_time += timedelta(days=1)
                reasoning = "基于常规晚餐时间预测"
            else:
                # 明天早餐
                predicted_time = (now + timedelta(days=1)).replace(hour=7, minute=30, second=0, microsecond=0)
                reasoning = "基于常规早餐时间预测（明天）"
            
            confidence = 0.6
        else:
            # 基于历史用餐模式预测
            if meal_pattern.get("meal_times"):
                # 计算平均用餐间隔
                avg_hour = sum([m.get("avg_hour", 12) for m in meal_pattern["meal_times"]]) / len(meal_pattern["meal_times"])
                
                # 预测下次用餐时间（基于平均时间）
                now = datetime.utcnow()
                predicted_time = now.replace(hour=int(avg_hour), minute=30, second=0, microsecond=0)
                
                # 如果预测时间已过，则预测明天
                if predicted_time < now:
                    predicted_time += timedelta(days=1)
                
                # 或者基于上次用餐时间 + 平均间隔
                if last_meal_time:
                    # 计算平均用餐间隔（假设一天3餐）
                    avg_interval = timedelta(hours=24/3)
                    predicted_time = last_meal_time + avg_interval
                    
                    # 如果预测时间已过，调整到明天
                    if predicted_time < now:
                        predicted_time += timedelta(days=1)
                
                reasoning = f"基于历史用餐模式预测（平均用餐时间：{int(avg_hour)}点）"
                confidence = 0.75
            else:
                # 使用默认时间
                now = datetime.utcnow()
                if now.hour < 12:
                    predicted_time = now.replace(hour=12, minute=30, second=0, microsecond=0)
                elif now.hour < 19:
                    predicted_time = now.replace(hour=18, minute=30, second=0, microsecond=0)
                else:
                    predicted_time = (now + timedelta(days=1)).replace(hour=7, minute=30, second=0, microsecond=0)
                reasoning = "基于常规用餐时间预测"
                confidence = 0.6
        
        # 预测剂量（如果有最近的营养记录，可以基于此预测）
        predicted_dose = None
        if last_meal_time:
            # 可以基于历史数据预测，这里简化处理
            # 实际应该查询最近的营养记录来预测
            predicted_dose = None  # 需要更多信息才能预测
        
        return schemas.NextInsulinPredictionResponse(
            predicted_time=predicted_time,
            predicted_dose=predicted_dose,
            confidence=confidence,
            reasoning=reasoning,
            notification_scheduled=False  # 通知将在通知服务中安排
        )


    # ==================== 运动记录相关 ====================
    
    async def create_exercise_record(
        self,
        request: schemas.ExerciseRecordCreate,
        user_id: str
    ) -> schemas.ExerciseRecordResponse:
        """创建运动记录"""
        record_id = await crud.create_exercise_record(
            user_id=user_id,
            exercise_time=request.exercise_time,
            exercise_type=request.exercise_type,
            duration_minutes=request.duration_minutes,
            intensity=request.intensity,
            calories_burned=request.calories_burned,
            notes=request.notes
        )
        
        # 查询完整记录信息返回
        query = """
            SELECT * FROM exercise_records WHERE id = :id
        """
        record = await database.fetch_one(query=query, values={"id": record_id})
        
        return schemas.ExerciseRecordResponse(
            id=str(record["id"]),
            user_id=str(record["user_id"]),
            exercise_time=record["exercise_time"],
            exercise_type=record["exercise_type"],
            duration_minutes=record["duration_minutes"],
            intensity=record["intensity"],
            calories_burned=float(record["calories_burned"]),
            notes=record.get("notes"),
            created_at=record["created_at"]
        )
    
    async def get_today_exercise_summary(
        self,
        user_id: str
    ) -> schemas.TodayExerciseSummary:
        """获取今日运动汇总"""
        summary = await crud.get_today_exercise_summary(user_id, datetime.utcnow())
        
        # 转换为响应模型
        exercises = [
            schemas.ExerciseRecordResponse(
                id=str(r["id"]),
                user_id=str(r["user_id"]),
                exercise_time=r["exercise_time"],
                exercise_type=r["exercise_type"],
                duration_minutes=r["duration_minutes"],
                intensity=r["intensity"],
                calories_burned=float(r["calories_burned"]),
                notes=r.get("notes"),
                created_at=r["created_at"]
            )
            for r in summary["exercises"]
        ]
        
        return schemas.TodayExerciseSummary(
            total_calories=summary["total_calories"],
            total_duration=summary["total_duration"],
            exercise_count=summary["exercise_count"],
            exercises=exercises
        )
    
    async def get_exercise_records(
        self,
        user_id: str,
        start_date: Optional[datetime] = None,
        end_date: Optional[datetime] = None,
        limit: int = 50
    ) -> List[schemas.ExerciseRecordResponse]:
        """获取运动记录列表"""
        records = await crud.get_exercise_records(
            user_id=user_id,
            start_date=start_date,
            end_date=end_date,
            limit=limit
        )
        
        return [
            schemas.ExerciseRecordResponse(
                id=str(r["id"]),
                user_id=str(r["user_id"]),
                exercise_time=r["exercise_time"],
                exercise_type=r["exercise_type"],
                duration_minutes=r["duration_minutes"],
                intensity=r["intensity"],
                calories_burned=float(r["calories_burned"]),
                notes=r.get("notes"),
                created_at=r["created_at"]
            )
            for r in records
        ]
    
    # ==================== 水分记录相关 ====================
    
    async def create_water_record(
        self,
        request: schemas.WaterRecordCreate,
        user_id: str
    ) -> schemas.WaterRecordResponse:
        """创建水分记录"""
        record_id = await crud.create_water_record(
            user_id=user_id,
            record_time=request.record_time,
            amount_ml=request.amount_ml,
            water_type=request.water_type,
            notes=request.notes
        )
        
        # 查询完整记录信息返回
        query = """
            SELECT * FROM water_records WHERE id = :id
        """
        record = await database.fetch_one(query=query, values={"id": record_id})
        
        return schemas.WaterRecordResponse(
            id=str(record["id"]),
            user_id=str(record["user_id"]),
            record_time=record["record_time"],
            amount_ml=record["amount_ml"],
            water_type=record["water_type"],
            notes=record.get("notes"),
            created_at=record["created_at"]
        )
    
    async def get_today_water_summary(
        self,
        user_id: str
    ) -> schemas.TodayWaterSummary:
        """获取今日水分摄入汇总"""
        summary = await crud.get_today_water_summary(user_id, datetime.utcnow())
        
        # 转换为响应模型
        records = [
            schemas.WaterRecordResponse(
                id=str(r["id"]),
                user_id=str(r["user_id"]),
                record_time=r["record_time"],
                amount_ml=r["amount_ml"],
                water_type=r["water_type"],
                notes=r.get("notes"),
                created_at=r["created_at"]
            )
            for r in summary["records"]
        ]
        
        return schemas.TodayWaterSummary(
            total_ml=summary["total_ml"],
            record_count=summary["record_count"],
            records=records,
            progress_percentage=summary["progress_percentage"]
        )
    
    async def get_water_records(
        self,
        user_id: str,
        start_date: Optional[datetime] = None,
        end_date: Optional[datetime] = None,
        limit: int = 50
    ) -> List[schemas.WaterRecordResponse]:
        """获取水分记录列表"""
        records = await crud.get_water_records(
            user_id=user_id,
            start_date=start_date,
            end_date=end_date,
            limit=limit
        )
        
        return [
            schemas.WaterRecordResponse(
                id=str(r["id"]),
                user_id=str(r["user_id"]),
                record_time=r["record_time"],
                amount_ml=r["amount_ml"],
                water_type=r["water_type"],
                notes=r.get("notes"),
                created_at=r["created_at"]
            )
            for r in records
        ]

    # ==================== 用药记录相关 ====================
    
    async def create_medication_record(
        self,
        request: schemas.MedicationRecordCreate,
        user_id: str
    ) -> schemas.MedicationRecordResponse:
        """创建用药记录"""
        record_id = await crud.create_medication_record(
            user_id=user_id,
            medication_time=request.medication_time,
            medication_type=request.medication_type,
            medication_name=request.medication_name,
            dosage=request.dosage,
            dosage_unit=request.dosage_unit,
            notes=request.notes
        )
        
        # 查询完整记录信息返回
        query = """
            SELECT * FROM medication_records WHERE id = :id
        """
        record = await database.fetch_one(query=query, values={"id": record_id})
        
        return schemas.MedicationRecordResponse(
            id=str(record["id"]),
            user_id=str(record["user_id"]),
            medication_time=record["medication_time"],
            medication_type=record["medication_type"],
            medication_name=record["medication_name"],
            dosage=float(record["dosage"]),
            dosage_unit=record["dosage_unit"],
            notes=record.get("notes"),
            created_at=record["created_at"]
        )
    
    async def get_today_medication_summary(
        self,
        user_id: str
    ) -> schemas.TodayMedicationSummary:
        """获取今日用药汇总"""
        summary = await crud.get_today_medication_summary(user_id, datetime.utcnow())
        
        # 转换为响应模型
        medications = [
            schemas.MedicationRecordResponse(
                id=str(r["id"]),
                user_id=str(r["user_id"]),
                medication_time=r["medication_time"],
                medication_type=r["medication_type"],
                medication_name=r["medication_name"],
                dosage=float(r["dosage"]),
                dosage_unit=r["dosage_unit"],
                notes=r.get("notes"),
                created_at=r["created_at"]
            )
            for r in summary["medications"]
        ]
        
        return schemas.TodayMedicationSummary(
            total_count=summary["total_count"],
            insulin_count=summary["insulin_count"],
            oral_medication_count=summary["oral_medication_count"],
            medications=medications
        )
    
    async def get_medication_records(
        self,
        user_id: str,
        start_date: Optional[datetime] = None,
        end_date: Optional[datetime] = None,
        limit: int = 50
    ) -> List[schemas.MedicationRecordResponse]:
        """获取用药记录列表"""
        records = await crud.get_medication_records(
            user_id=user_id,
            start_date=start_date,
            end_date=end_date,
            limit=limit
        )
        
        return [
            schemas.MedicationRecordResponse(
                id=str(r["id"]),
                user_id=str(r["user_id"]),
                medication_time=r["medication_time"],
                medication_type=r["medication_type"],
                medication_name=r["medication_name"],
                dosage=float(r["dosage"]),
                dosage_unit=r["dosage_unit"],
                notes=r.get("notes"),
                created_at=r["created_at"]
            )
            for r in records
        ]
