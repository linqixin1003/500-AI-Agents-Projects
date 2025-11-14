from typing import Optional
from uuid import uuid4
from datetime import datetime
from app.insulin.schemas import InsulinCalculationRequest, InsulinCalculationResponse
from app.insulin.calculator import InsulinCalculator
from app.user import crud
from app.database import database
import logging

logger = logging.getLogger(__name__)

class InsulinService:
    """胰岛素计算服务"""
    
    def __init__(self):
        self.calculator = InsulinCalculator()
    
    async def calculate_insulin_dose(
        self,
        request: InsulinCalculationRequest,
        user_id: str,
        nutrition_record_id: Optional[str] = None,
        gi_value: Optional[float] = None
    ) -> InsulinCalculationResponse:
        """计算胰岛素剂量
        
        Args:
            request: 计算请求
            user_id: 用户ID
            nutrition_record_id: 营养成分记录ID（可选）
            gi_value: 升糖指数（可选）
            
        Returns:
            InsulinCalculationResponse: 计算结果
        """
        try:
            # 获取用户参数
            user_params = await crud.get_user_parameters(user_id)
            if not user_params:
                raise ValueError("用户参数未设置，请先设置用户参数")
            
            # 提取参数（转换为 float 避免 Decimal 类型问题）
            isf = float(user_params.get("isf")) if user_params.get("isf") else None
            icr = float(user_params.get("icr")) if user_params.get("icr") else None
            target_bg_low = float(user_params.get("target_bg_low", 4.0))
            target_bg_high = float(user_params.get("target_bg_high", 7.8))
            max_dose = float(user_params.get("max_insulin_dose")) if user_params.get("max_insulin_dose") else None
            min_dose = float(user_params.get("min_insulin_dose", 0.5))
            
            if not isf or not icr:
                raise ValueError("ISF 或 ICR 未设置，请先设置用户参数")
            
            # 使用目标血糖范围的中值
            target_bg = (target_bg_low + target_bg_high) / 2.0
            
            # 执行计算
            result = self.calculator.calculate_insulin_dose(
                total_carbs=request.total_carbs,
                current_bg=request.current_bg,
                isf=isf,
                icr=icr,
                target_bg=target_bg,
                activity_level=request.activity_level or "sedentary",
                meal_time=request.meal_time,
                gi_value=gi_value,
                max_dose=max_dose,
                min_dose=min_dose
            )
            
            # 保存记录到数据库
            insulin_record_id = await self._save_insulin_record(
                user_id=user_id,
                nutrition_record_id=nutrition_record_id,
                request=request,
                result=result
            )
            
            return InsulinCalculationResponse(**result)
            
        except ValueError as e:
            logger.error(f"Insulin calculation error: {str(e)}")
            raise
        except Exception as e:
            logger.error(f"Insulin calculation error: {str(e)}")
            raise
    
    async def _save_insulin_record(
        self,
        user_id: str,
        nutrition_record_id: Optional[str],
        request: InsulinCalculationRequest,
        result: dict
    ) -> str:
        """保存胰岛素记录到数据库"""
        record_id = str(uuid4())
        
        query = """
            INSERT INTO insulin_records 
            (id, user_id, nutrition_record_id, current_bg, recommended_dose, 
             carb_insulin, correction_insulin, activity_factor, injection_time,
             injection_type, risk_level, created_at)
            VALUES 
            (:id, :user_id, :nutrition_record_id, :current_bg, :recommended_dose,
             :carb_insulin, :correction_insulin, :activity_factor, :injection_time,
             :injection_type, :risk_level, :created_at)
            RETURNING id
        """
        
        values = {
            "id": record_id,
            "user_id": user_id,
            "nutrition_record_id": nutrition_record_id,
            "current_bg": request.current_bg,
            "recommended_dose": result["recommended_dose"],
            "carb_insulin": result["carb_insulin"],
            "correction_insulin": result["correction_insulin"],
            "activity_factor": 1.0 + (result["activity_adjustment"] / result["recommended_dose"] if result["recommended_dose"] > 0 else 0),
            "injection_time": datetime.utcnow(),
            "injection_type": "split" if result["split_dose"] else "single",
            "risk_level": result["risk_level"],
            "created_at": datetime.utcnow()
        }
        
        db_result = await database.fetch_one(query=query, values=values)
        return str(db_result["id"])

