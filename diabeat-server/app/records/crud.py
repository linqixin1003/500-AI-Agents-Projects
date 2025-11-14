from typing import Optional, Dict, Any, List
from uuid import uuid4
from datetime import datetime, timezone
from app.database import database

def _normalize_datetime(dt: datetime) -> datetime:
    """将 datetime 统一为 timezone-naive UTC"""
    if dt.tzinfo is not None:
        return dt.astimezone(timezone.utc).replace(tzinfo=None)
    return dt

async def create_meal_record(
    user_id: str,
    meal_time: datetime,
    food_recognition_id: Optional[str] = None,
    nutrition_record_id: Optional[str] = None,
    notes: Optional[str] = None
) -> str:
    """创建用餐记录"""
    record_id = str(uuid4())
    
    query = """
        INSERT INTO meal_records 
        (id, user_id, meal_time, food_recognition_id, nutrition_record_id, notes, created_at)
        VALUES 
        (:id, :user_id, :meal_time, :food_recognition_id, :nutrition_record_id, :notes, :created_at)
        RETURNING id
    """
    
    values = {
        "id": record_id,
        "user_id": user_id,
        "meal_time": _normalize_datetime(meal_time),
        "food_recognition_id": food_recognition_id,
        "nutrition_record_id": nutrition_record_id,
        "notes": notes,
        "created_at": datetime.utcnow()
    }
    
    result = await database.fetch_one(query=query, values=values)
    return str(result["id"])

async def create_insulin_injection_record(
    user_id: str,
    injection_time: datetime,
    actual_dose: float,
    insulin_record_id: Optional[str] = None,
    notes: Optional[str] = None
) -> str:
    """创建胰岛素注射记录"""
    record_id = str(uuid4())
    
    query = """
        INSERT INTO insulin_injection_records 
        (id, user_id, injection_time, insulin_record_id, actual_dose, notes, created_at)
        VALUES 
        (:id, :user_id, :injection_time, :insulin_record_id, :actual_dose, :notes, :created_at)
        RETURNING id
    """
    
    values = {
        "id": record_id,
        "user_id": user_id,
        "injection_time": _normalize_datetime(injection_time),
        "insulin_record_id": insulin_record_id,
        "actual_dose": actual_dose,
        "notes": notes,
        "created_at": datetime.utcnow()
    }
    
    result = await database.fetch_one(query=query, values=values)
    return str(result["id"])

async def get_recent_meal_records(user_id: str, limit: int = 10) -> List[Dict[str, Any]]:
    """获取最近的用餐记录"""
    query = """
        SELECT * FROM meal_records 
        WHERE user_id = :user_id 
        ORDER BY meal_time DESC 
        LIMIT :limit
    """
    
    results = await database.fetch_all(query=query, values={"user_id": user_id, "limit": limit})
    return [dict(row) for row in results]

async def get_recent_insulin_records(user_id: str, limit: int = 10) -> List[Dict[str, Any]]:
    """获取最近的胰岛素注射记录"""
    query = """
        SELECT * FROM insulin_injection_records 
        WHERE user_id = :user_id 
        ORDER BY injection_time DESC 
        LIMIT :limit
    """
    
    results = await database.fetch_all(query=query, values={"user_id": user_id, "limit": limit})
    return [dict(row) for row in results]

async def get_last_meal_time(user_id: str) -> Optional[datetime]:
    """获取最后一次用餐时间"""
    query = """
        SELECT meal_time FROM meal_records 
        WHERE user_id = :user_id 
        ORDER BY meal_time DESC 
        LIMIT 1
    """
    
    result = await database.fetch_one(query=query, values={"user_id": user_id})
    if result:
        return result["meal_time"]
    return None

async def get_last_insulin_time(user_id: str) -> Optional[datetime]:
    """获取最后一次胰岛素注射时间"""
    query = """
        SELECT injection_time FROM insulin_injection_records 
        WHERE user_id = :user_id 
        ORDER BY injection_time DESC 
        LIMIT 1
    """
    
    result = await database.fetch_one(query=query, values={"user_id": user_id})
    if result:
        return result["injection_time"]
    return None

async def get_user_meal_pattern(user_id: str, days: int = 7) -> Dict[str, Any]:
    """分析用户用餐模式"""
    from datetime import timedelta
    
    query = """
        SELECT 
            DATE(meal_time) as meal_date,
            COUNT(*) as meal_count,
            AVG(EXTRACT(HOUR FROM meal_time)) as avg_hour
        FROM meal_records 
        WHERE user_id = :user_id 
          AND meal_time >= :start_date
        GROUP BY DATE(meal_time)
        ORDER BY meal_date DESC
    """
    
    start_date = datetime.utcnow() - timedelta(days=days)
    results = await database.fetch_all(
        query=query, 
        values={"user_id": user_id, "start_date": start_date}
    )
    
    if not results:
        return {"avg_meal_times": [], "meal_frequency": 0}
    
    # 计算平均用餐时间
    meal_times = []
    for row in results:
        meal_times.append({
            "date": row["meal_date"],
            "count": row["meal_count"],
            "avg_hour": round(row["avg_hour"], 1) if row["avg_hour"] else None
        })
    
    return {
        "meal_times": meal_times,
        "avg_meal_frequency": len(meal_times) / days if days > 0 else 0
    }

async def get_today_nutrition_intake(user_id: str, target_date: Optional[datetime] = None) -> Dict[str, Any]:
    """获取指定日期的营养摄入统计"""
    from datetime import date as date_type
    
    if target_date is None:
        target_date = datetime.utcnow()
    
    # 获取当天的开始和结束时间
    start_of_day = target_date.replace(hour=0, minute=0, second=0, microsecond=0)
    end_of_day = target_date.replace(hour=23, minute=59, second=59, microsecond=999999)
    
    query = """
        SELECT 
            COALESCE(SUM(nr.calories), 0) as total_calories,
            COALESCE(SUM(nr.total_carbs), 0) as total_carbs,
            COALESCE(SUM(nr.net_carbs), 0) as total_net_carbs,
            COALESCE(SUM(nr.protein), 0) as total_protein,
            COALESCE(SUM(nr.fat), 0) as total_fat,
            COALESCE(SUM(nr.fiber), 0) as total_fiber,
            COUNT(DISTINCT mr.id) as meal_count
        FROM meal_records mr
        LEFT JOIN nutrition_records nr ON mr.nutrition_record_id = nr.id
        WHERE mr.user_id = :user_id
          AND mr.meal_time >= :start_of_day
          AND mr.meal_time <= :end_of_day
    """
    
    result = await database.fetch_one(
        query=query,
        values={
            "user_id": user_id,
            "start_of_day": start_of_day,
            "end_of_day": end_of_day
        }
    )
    
    if result:
        return {
            "total_calories": float(result["total_calories"] or 0),
            "total_carbs": float(result["total_carbs"] or 0),
            "total_net_carbs": float(result["total_net_carbs"] or 0),
            "total_protein": float(result["total_protein"] or 0),
            "total_fat": float(result["total_fat"] or 0),
            "total_fiber": float(result["total_fiber"] or 0),
            "meal_count": int(result["meal_count"] or 0),
            "date": target_date.date().isoformat()
        }
    
    return {
        "total_calories": 0.0,
        "total_carbs": 0.0,
        "total_net_carbs": 0.0,
        "total_protein": 0.0,
        "total_fat": 0.0,
        "total_fiber": 0.0,
        "meal_count": 0,
        "date": target_date.date().isoformat()
    }

async def get_meal_history(
    user_id: str,
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    limit: int = 100
) -> List[Dict[str, Any]]:
    """获取用户的饮食历史记录（支持日期范围查询）
    
    Args:
        user_id: 用户ID
        start_date: 开始日期（可选）
        end_date: 结束日期（可选）
        limit: 返回记录数量限制
        
    Returns:
        List[Dict]: 饮食历史记录列表，包含营养信息
    """
    query = """
        SELECT 
            mr.id,
            mr.user_id,
            mr.meal_time,
            mr.food_recognition_id,
            mr.nutrition_record_id,
            mr.notes,
            mr.created_at,
            nr.total_carbs,
            nr.net_carbs,
            nr.protein,
            nr.fat,
            nr.fiber,
            nr.calories,
            nr.gi_value,
            nr.gl_value,
            fr.image_url,
            fr.recognition_result
        FROM meal_records mr
        LEFT JOIN nutrition_records nr ON mr.nutrition_record_id = nr.id
        LEFT JOIN food_recognitions fr ON mr.food_recognition_id = fr.id
        WHERE mr.user_id = :user_id
    """
    
    values = {"user_id": user_id, "limit": limit}
    
    # 添加日期范围过滤
    if start_date:
        query += " AND mr.meal_time >= :start_date"
        values["start_date"] = _normalize_datetime(start_date)
    
    if end_date:
        query += " AND mr.meal_time <= :end_date"
        values["end_date"] = _normalize_datetime(end_date)
    
    query += " ORDER BY mr.meal_time DESC LIMIT :limit"
    
    results = await database.fetch_all(query=query, values=values)
    return [dict(row) for row in results]

# ==================== 运动记录相关 ====================

async def create_exercise_record(
    user_id: str,
    exercise_time: datetime,
    exercise_type: str,
    duration_minutes: int,
    intensity: str = "moderate",
    calories_burned: Optional[float] = None,
    notes: Optional[str] = None
) -> str:
    """创建运动记录
    
    如果未提供calories_burned，系统会根据运动类型、时长和强度自动估算
    """
    record_id = str(uuid4())
    
    # 如果未提供消耗热量，自动估算
    if calories_burned is None:
        calories_burned = _estimate_calories_burned(exercise_type, duration_minutes, intensity)
    
    query = """
        INSERT INTO exercise_records 
        (id, user_id, exercise_time, exercise_type, duration_minutes, intensity, calories_burned, notes, created_at)
        VALUES 
        (:id, :user_id, :exercise_time, :exercise_type, :duration_minutes, :intensity, :calories_burned, :notes, :created_at)
        RETURNING id
    """
    
    values = {
        "id": record_id,
        "user_id": user_id,
        "exercise_time": _normalize_datetime(exercise_time),
        "exercise_type": exercise_type,
        "duration_minutes": duration_minutes,
        "intensity": intensity,
        "calories_burned": calories_burned,
        "notes": notes,
        "created_at": datetime.utcnow()
    }
    
    result = await database.fetch_one(query=query, values=values)
    return str(result["id"])

def _estimate_calories_burned(exercise_type: str, duration_minutes: int, intensity: str) -> float:
    """估算运动消耗热量
    
    基于MET (Metabolic Equivalent of Task) 值
    热量消耗 = MET × 体重(kg) × 时长(小时)
    这里假设平均体重70kg
    """
    # MET值表 (代谢当量)
    MET_VALUES = {
        "walking": {"light": 2.5, "moderate": 3.5, "vigorous": 4.5},
        "running": {"light": 6.0, "moderate": 8.0, "vigorous": 10.0},
        "cycling": {"light": 4.0, "moderate": 6.0, "vigorous": 8.0},
        "swimming": {"light": 4.0, "moderate": 6.0, "vigorous": 8.0},
        "gym": {"light": 3.0, "moderate": 5.0, "vigorous": 7.0},
        "yoga": {"light": 2.5, "moderate": 3.0, "vigorous": 4.0},
        "dancing": {"light": 3.0, "moderate": 4.5, "vigorous": 6.0},
        "other": {"light": 3.0, "moderate": 4.0, "vigorous": 5.0},
    }
    
    # 获取MET值
    met = MET_VALUES.get(exercise_type, MET_VALUES["other"]).get(intensity, 4.0)
    
    # 计算热量消耗 (假设体重70kg)
    weight_kg = 70.0
    hours = duration_minutes / 60.0
    calories = met * weight_kg * hours
    
    return round(calories, 1)

async def get_today_exercise_summary(user_id: str, date: datetime) -> Dict[str, Any]:
    """获取指定日期的运动汇总"""
    start_of_day = date.replace(hour=0, minute=0, second=0, microsecond=0)
    end_of_day = date.replace(hour=23, minute=59, second=59, microsecond=999999)
    
    query = """
        SELECT 
            id,
            user_id,
            exercise_time,
            exercise_type,
            duration_minutes,
            intensity,
            calories_burned,
            notes,
            created_at
        FROM exercise_records
        WHERE user_id = :user_id
        AND exercise_time >= :start_date
        AND exercise_time <= :end_date
        ORDER BY exercise_time DESC
    """
    
    values = {
        "user_id": user_id,
        "start_date": _normalize_datetime(start_of_day),
        "end_date": _normalize_datetime(end_of_day)
    }
    
    records = await database.fetch_all(query=query, values=values)
    records_list = [dict(row) for row in records]
    
    # 计算汇总
    total_calories = sum(r["calories_burned"] for r in records_list)
    total_duration = sum(r["duration_minutes"] for r in records_list)
    
    return {
        "total_calories": round(total_calories, 1),
        "total_duration": total_duration,
        "exercise_count": len(records_list),
        "exercises": records_list
    }

async def get_exercise_records(
    user_id: str,
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    limit: int = 50
) -> List[Dict[str, Any]]:
    """获取运动记录列表"""
    query = """
        SELECT 
            id,
            user_id,
            exercise_time,
            exercise_type,
            duration_minutes,
            intensity,
            calories_burned,
            notes,
            created_at
        FROM exercise_records
        WHERE user_id = :user_id
    """
    
    values = {"user_id": user_id, "limit": limit}
    
    if start_date:
        query += " AND exercise_time >= :start_date"
        values["start_date"] = _normalize_datetime(start_date)
    
    if end_date:
        query += " AND exercise_time <= :end_date"
        values["end_date"] = _normalize_datetime(end_date)
    
    query += " ORDER BY exercise_time DESC LIMIT :limit"
    
    results = await database.fetch_all(query=query, values=values)
    return [dict(row) for row in results]

# ==================== 水分记录相关 ====================

async def create_water_record(
    user_id: str,
    record_time: datetime,
    amount_ml: int,
    water_type: str = "water",
    notes: Optional[str] = None
) -> str:
    """创建水分记录"""
    record_id = str(uuid4())
    
    query = """
        INSERT INTO water_records 
        (id, user_id, record_time, amount_ml, water_type, notes, created_at)
        VALUES 
        (:id, :user_id, :record_time, :amount_ml, :water_type, :notes, :created_at)
        RETURNING id
    """
    
    values = {
        "id": record_id,
        "user_id": user_id,
        "record_time": _normalize_datetime(record_time),
        "amount_ml": amount_ml,
        "water_type": water_type,
        "notes": notes,
        "created_at": datetime.utcnow()
    }
    
    result = await database.fetch_one(query=query, values=values)
    return str(result["id"])

async def get_today_water_summary(user_id: str, date: datetime) -> Dict[str, Any]:
    """获取指定日期的水分摄入汇总"""
    start_of_day = date.replace(hour=0, minute=0, second=0, microsecond=0)
    end_of_day = date.replace(hour=23, minute=59, second=59, microsecond=999999)
    
    query = """
        SELECT 
            id,
            user_id,
            record_time,
            amount_ml,
            water_type,
            notes,
            created_at
        FROM water_records
        WHERE user_id = :user_id
        AND record_time >= :start_date
        AND record_time <= :end_date
        ORDER BY record_time DESC
    """
    
    values = {
        "user_id": user_id,
        "start_date": _normalize_datetime(start_of_day),
        "end_date": _normalize_datetime(end_of_day)
    }
    
    records = await database.fetch_all(query=query, values=values)
    records_list = [dict(row) for row in records]
    
    # 计算汇总
    total_ml = sum(r["amount_ml"] for r in records_list)
    progress_percentage = (total_ml / 2000.0) * 100  # 基于2000ml目标
    
    return {
        "total_ml": total_ml,
        "record_count": len(records_list),
        "records": records_list,
        "progress_percentage": round(progress_percentage, 1)
    }

async def get_water_records(
    user_id: str,
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    limit: int = 50
) -> List[Dict[str, Any]]:
    """获取水分记录列表"""
    query = """
        SELECT 
            id,
            user_id,
            record_time,
            amount_ml,
            water_type,
            notes,
            created_at
        FROM water_records
        WHERE user_id = :user_id
    """
    
    values = {"user_id": user_id, "limit": limit}
    
    if start_date:
        query += " AND record_time >= :start_date"
        values["start_date"] = _normalize_datetime(start_date)
    
    if end_date:
        query += " AND record_time <= :end_date"
        values["end_date"] = _normalize_datetime(end_date)
    
    query += " ORDER BY record_time DESC LIMIT :limit"
    
    results = await database.fetch_all(query=query, values=values)
    return [dict(row) for row in results]


# ==================== 用药记录相关 ====================

async def create_medication_record(
    user_id: str,
    medication_time: datetime,
    medication_type: str,
    medication_name: str,
    dosage: float,
    dosage_unit: str,
    notes: Optional[str] = None
) -> str:
    """创建用药记录"""
    record_id = str(uuid4())
    
    query = """
        INSERT INTO medication_records 
        (id, user_id, medication_time, medication_type, medication_name, dosage, dosage_unit, notes, created_at)
        VALUES 
        (:id, :user_id, :medication_time, :medication_type, :medication_name, :dosage, :dosage_unit, :notes, :created_at)
        RETURNING id
    """
    
    values = {
        "id": record_id,
        "user_id": user_id,
        "medication_time": _normalize_datetime(medication_time),
        "medication_type": medication_type,
        "medication_name": medication_name,
        "dosage": dosage,
        "dosage_unit": dosage_unit,
        "notes": notes,
        "created_at": datetime.utcnow()
    }
    
    result = await database.fetch_one(query=query, values=values)
    return str(result["id"])

async def get_today_medication_summary(user_id: str, date: datetime) -> Dict[str, Any]:
    """获取指定日期的用药汇总"""
    start_of_day = date.replace(hour=0, minute=0, second=0, microsecond=0)
    end_of_day = date.replace(hour=23, minute=59, second=59, microsecond=999999)
    
    query = """
        SELECT 
            id,
            user_id,
            medication_time,
            medication_type,
            medication_name,
            dosage,
            dosage_unit,
            notes,
            created_at
        FROM medication_records
        WHERE user_id = :user_id
        AND medication_time >= :start_date
        AND medication_time <= :end_date
        ORDER BY medication_time DESC
    """
    
    values = {
        "user_id": user_id,
        "start_date": _normalize_datetime(start_of_day),
        "end_date": _normalize_datetime(end_of_day)
    }
    
    records = await database.fetch_all(query=query, values=values)
    records_list = [dict(row) for row in records]
    
    # 统计不同类型的用药
    insulin_count = sum(1 for r in records_list if r["medication_type"] == "insulin")
    oral_count = sum(1 for r in records_list if r["medication_type"] == "oral_medication")
    
    return {
        "total_count": len(records_list),
        "insulin_count": insulin_count,
        "oral_medication_count": oral_count,
        "medications": records_list
    }

async def get_medication_records(
    user_id: str,
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    limit: int = 50
) -> List[Dict[str, Any]]:
    """获取用药记录列表"""
    query = """
        SELECT 
            id,
            user_id,
            medication_time,
            medication_type,
            medication_name,
            dosage,
            dosage_unit,
            notes,
            created_at
        FROM medication_records
        WHERE user_id = :user_id
    """
    
    values = {"user_id": user_id, "limit": limit}
    
    if start_date:
        query += " AND medication_time >= :start_date"
        values["start_date"] = _normalize_datetime(start_date)
    
    if end_date:
        query += " AND medication_time <= :end_date"
        values["end_date"] = _normalize_datetime(end_date)
    
    query += " ORDER BY medication_time DESC LIMIT :limit"
    
    results = await database.fetch_all(query=query, values=values)
    return [dict(row) for row in results]

async def get_last_medication_time(user_id: str) -> Optional[datetime]:
    """获取最后一次用药时间"""
    query = """
        SELECT medication_time
        FROM medication_records
        WHERE user_id = :user_id
        ORDER BY medication_time DESC
        LIMIT 1
    """
    result = await database.fetch_one(query=query, values={"user_id": user_id})
    return result["medication_time"] if result else None

