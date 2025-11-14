from typing import List, Dict, Any, Optional
from app.nutrition.schemas import FoodItemInput
import logging

logger = logging.getLogger(__name__)

class NutritionCalculator:
    """营养成分计算器"""
    
    async def _get_food_nutrition_from_db(self, food_name: str) -> Optional[Dict[str, float]]:
        """从数据库获取食物营养成分"""
        try:
            from app.database import database
            query = """
                SELECT carbs, protein, fat, fiber, calories, gi_value 
                FROM nutrition_foods 
                WHERE name_cn = :name OR name_cn LIKE :name_like
                LIMIT 1
            """
            result = await database.fetch_one(
                query=query,
                values={"name": food_name, "name_like": f"%{food_name}%"}
            )
            if result:
                return {
                    "carbs": float(result["carbs"]),
                    "protein": float(result["protein"]),
                    "fat": float(result["fat"]),
                    "fiber": float(result["fiber"]),
                    "calories": float(result["calories"]),
                    "gi_value": float(result["gi_value"]) if result["gi_value"] else None
                }
        except Exception as e:
            logger.warning(f"Failed to query database for {food_name}: {str(e)}")
        return None
    
    async def search_food_by_name(self, food_name: str) -> List[Dict[str, Any]]:
        """根据食物名称从数据库中搜索食物，返回名称和卡路里"""
        try:
            from app.database import database
            query = """
                SELECT id, name_cn AS name, calories
                FROM nutrition_foods
                WHERE LOWER(name_cn) LIKE LOWER(:food_name_like)
                LIMIT 20
            """
            results = await database.fetch_all(
                query=query,
                values={
                    "food_name_like": f"%{food_name}%"
                }
            )
            return [
                {"id": str(r["id"]), "name": r["name"], "calories": int(r["calories"]) if r["calories"] else 0}
                for r in results
            ]
        except Exception as e:
            logger.warning(f"Failed to search database for {food_name}: {str(e)}")
            return []

    # 常见食物的营养成分数据库（每100g）- 作为后备
    FOOD_NUTRITION_DB = {
        "白米饭": {
            "carbs": 25.9,
            "protein": 2.6,
            "fat": 0.3,
            "fiber": 0.3,
            "calories": 116,
            "gi_value": 83
        },
        "米饭": {
            "carbs": 25.9,
            "protein": 2.6,
            "fat": 0.3,
            "fiber": 0.3,
            "calories": 116,
            "gi_value": 83
        },
        "红烧肉": {
            "carbs": 5.0,
            "protein": 15.0,
            "fat": 30.0,
            "fiber": 0.0,
            "calories": 320,
            "gi_value": 0
        },
        "青菜": {
            "carbs": 3.0,
            "protein": 2.0,
            "fat": 0.2,
            "fiber": 2.0,
            "calories": 20,
            "gi_value": 15
        },
        "小白菜": {
            "carbs": 2.5,
            "protein": 1.5,
            "fat": 0.2,
            "fiber": 1.5,
            "calories": 15,
            "gi_value": 15
        },
        "鸡蛋": {
            "carbs": 1.1,
            "protein": 13.0,
            "fat": 10.0,
            "fiber": 0.0,
            "calories": 144,
            "gi_value": 0
        },
        "鸡肉": {
            "carbs": 0.0,
            "protein": 20.0,
            "fat": 5.0,
            "fiber": 0.0,
            "calories": 125,
            "gi_value": 0
        },
        "猪肉": {
            "carbs": 0.0,
            "protein": 20.0,
            "fat": 15.0,
            "fiber": 0.0,
            "calories": 200,
            "gi_value": 0
        },
        "面条": {
            "carbs": 25.0,
            "protein": 4.0,
            "fat": 1.0,
            "fiber": 1.0,
            "calories": 130,
            "gi_value": 55
        },
        "面包": {
            "carbs": 50.0,
            "protein": 8.0,
            "fat": 3.0,
            "fiber": 2.0,
            "calories": 250,
            "gi_value": 70
        }
    }
    
    # 烹饪方式影响因子
    COOKING_FACTORS = {
        "steamed": 0.95,  # 蒸：GI值降低
        "boiled": 0.95,   # 煮：GI值降低
        "fried": 1.10,    # 炸：GI值升高
        "braised": 1.05,  # 红烧：GI值略高
        "roasted": 1.05,  # 烤：GI值略高
        "raw": 1.0        # 生：无影响
    }
    
    async def calculate_nutrition(self, foods: List[FoodItemInput]) -> Dict[str, Any]:
        """计算营养成分
        
        Args:
            foods: 食物列表
            
        Returns:
            Dict: 营养成分计算结果
        """
        total_carbs = 0.0
        total_net_carbs = 0.0
        total_protein = 0.0
        total_fat = 0.0
        total_fiber = 0.0
        total_calories = 0.0
        weighted_gi = 0.0
        total_carb_weight = 0.0
        
        calculation_details = []
        
        for food in foods:
            # 查找食物营养成分
            nutrition = await self._get_food_nutrition(food.name)
            
            # 计算分量比例
            weight_ratio = food.weight / 100.0
            
            # 计算营养成分
            carbs = nutrition["carbs"] * weight_ratio
            protein = nutrition["protein"] * weight_ratio
            fat = nutrition["fat"] * weight_ratio
            fiber = nutrition["fiber"] * weight_ratio
            calories = nutrition["calories"] * weight_ratio
            
            # 应用烹饪方式影响
            cooking_factor = self._get_cooking_factor(food.cooking_method)
            gi_value = nutrition.get("gi_value", 0) * cooking_factor
            
            # 计算净碳水（扣除纤维）
            net_carbs = carbs - fiber
            
            # 累加
            total_carbs += carbs
            total_net_carbs += net_carbs
            total_protein += protein
            total_fat += fat
            total_fiber += fiber
            total_calories += calories
            
            # 计算加权GI值
            if carbs > 0:
                weighted_gi += gi_value * carbs
                total_carb_weight += carbs
            
            # 记录计算详情
            calculation_details.append({
                "name": food.name,
                "weight": food.weight,
                "carbs": round(carbs, 2),
                "net_carbs": round(net_carbs, 2),
                "protein": round(protein, 2),
                "fat": round(fat, 2),
                "fiber": round(fiber, 2),
                "calories": round(calories, 2),
                "gi_value": round(gi_value, 1) if gi_value > 0 else None
            })
        
        # 计算平均GI值
        avg_gi = weighted_gi / total_carb_weight if total_carb_weight > 0 else None
        
        # 计算血糖负荷 (GL = GI × 碳水含量 / 100)
        gl_value = (avg_gi * total_carbs / 100.0) if avg_gi else None
        
        return {
            "total_carbs": round(total_carbs, 2),
            "net_carbs": round(total_net_carbs, 2),
            "protein": round(total_protein, 2),
            "fat": round(total_fat, 2),
            "fiber": round(total_fiber, 2),
            "calories": round(total_calories, 2),
            "gi_value": round(avg_gi, 1) if avg_gi else None,
            "gl_value": round(gl_value, 2) if gl_value else None,
            "calculation_details": calculation_details
        }
    
    async def _get_food_nutrition(self, food_name: str) -> Dict[str, float]:
        """获取食物营养成分
        
        Args:
            food_name: 食物名称
            
        Returns:
            Dict: 营养成分（每100g）
        """
        # 首先尝试从数据库查询
        db_result = await self._get_food_nutrition_from_db(food_name)
        if db_result:
            return db_result
        
        # 如果数据库没有，尝试内存数据库
        if food_name in self.FOOD_NUTRITION_DB:
            return self.FOOD_NUTRITION_DB[food_name]
        
        # 尝试模糊匹配
        for key, value in self.FOOD_NUTRITION_DB.items():
            if key in food_name or food_name in key:
                return value
        
        # 如果找不到，返回默认值（基于常见食物平均值）
        logger.warning(f"Food not found in database: {food_name}, using default values")
        return {
            "carbs": 20.0,
            "protein": 5.0,
            "fat": 5.0,
            "fiber": 2.0,
            "calories": 150,
            "gi_value": 50
        }
    
    def _get_cooking_factor(self, cooking_method: Optional[str]) -> float:
        """获取烹饪方式影响因子
        
        Args:
            cooking_method: 烹饪方式
            
        Returns:
            float: 影响因子
        """
        if not cooking_method:
            return 1.0
        return self.COOKING_FACTORS.get(cooking_method.lower(), 1.0)

