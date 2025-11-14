from typing import Optional
from uuid import uuid4
from datetime import datetime
from app.nutrition.schemas import NutritionCalculationRequest, NutritionCalculationResponse, FoodItemInput
from app.nutrition.calculator import NutritionCalculator
from app.database import database
import logging

logger = logging.getLogger(__name__)

class NutritionService:
    """营养成分计算服务"""
    
    def __init__(self):
        self.calculator = NutritionCalculator()
    
    async def calculate_nutrition(
        self,
        request: NutritionCalculationRequest,
        user_id: str,
        food_recognition_id: Optional[str] = None
    ) -> NutritionCalculationResponse:
        """计算营养成分
        
        Args:
            request: 计算请求
            user_id: 用户ID
            food_recognition_id: 食物识别记录ID（可选）
            
        Returns:
            NutritionCalculationResponse: 计算结果
        """
        try:
            # 执行计算
            result = await self.calculator.calculate_nutrition(request.foods)
            
            # 保存计算结果到数据库
            nutrition_record_id = await self._save_nutrition_record(
                user_id=user_id,
                food_recognition_id=food_recognition_id,
                result=result
            )
            
            return NutritionCalculationResponse(
                total_carbs=result["total_carbs"],
                net_carbs=result["net_carbs"],
                protein=result["protein"],
                fat=result["fat"],
                fiber=result["fiber"],
                calories=result["calories"],
                gi_value=result["gi_value"],
                gl_value=result["gl_value"],
                calculation_details=result["calculation_details"],
                nutrition_record_id=nutrition_record_id
            )
            
        except Exception as e:
            logger.error(f"Nutrition calculation error: {str(e)}")
            raise
    
    async def _save_nutrition_record(
        self,
        user_id: str,
        food_recognition_id: Optional[str],
        result: dict
    ) -> str:
        """保存营养成分记录到数据库"""
        record_id = str(uuid4())
        
        import json
        calculation_details = json.dumps(result.get("calculation_details", []))
        
        query = """
            INSERT INTO nutrition_records 
            (id, user_id, food_recognition_id, total_carbs, net_carbs, fiber, protein, fat, 
             calories, gi_value, gl_value, calculation_details, created_at)
            VALUES 
            (:id, :user_id, :food_recognition_id, :total_carbs, :net_carbs, :fiber, :protein, :fat,
             :calories, :gi_value, :gl_value, CAST(:calculation_details AS JSONB), :created_at)
            RETURNING id
        """
        
        values = {
            "id": record_id,
            "user_id": user_id,
            "food_recognition_id": food_recognition_id,
            "total_carbs": result["total_carbs"],
            "net_carbs": result["net_carbs"],
            "fiber": result["fiber"],
            "protein": result["protein"],
            "fat": result["fat"],
            "calories": result["calories"],
            "gi_value": result["gi_value"],
            "gl_value": result["gl_value"],
            "calculation_details": calculation_details,
            "created_at": datetime.utcnow()
        }
        
        db_result = await database.fetch_one(query=query, values=values)
        return str(db_result["id"])

