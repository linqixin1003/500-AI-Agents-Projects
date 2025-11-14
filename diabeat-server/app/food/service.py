from typing import List, Optional
from fastapi import UploadFile
from uuid import uuid4
from datetime import datetime
from app.food.classifiers.food_classifier_factory import FoodClassifierFactory
from app.food.schemas import FoodRecognitionResponse, FoodItem, FoodRecommendation
from app.storage.base import StorageProvider
from app.storage.local import LocalStorageProvider
from app.database import database
from app.nutrition.calculator import NutritionCalculator
from app.nutrition.schemas import FoodItemInput
from app.food.recommendation_calculator import FoodRecommendationCalculator
from app.user import crud as user_crud
import logging

logger = logging.getLogger(__name__)

class FoodService:
    """食物识别服务"""
    
    def __init__(self, storage: Optional[StorageProvider] = None):
        """
        Args:
            storage: 存储提供者，如果为 None 则使用本地存储
        """
        self.storage = storage or LocalStorageProvider()
        # 根据配置选择分类器，默认使用通义千问
        self.classifier = FoodClassifierFactory.create()
        self.nutrition_calculator = NutritionCalculator() # 初始化 NutritionCalculator
        self.recommendation_calculator = FoodRecommendationCalculator() # 初始化建议计算器

    async def recognize_food(
        self,
        image: UploadFile,
        user_id: str
    ) -> FoodRecognitionResponse:
        """识别食物
        
        Args:
            image: 上传的图片文件
            user_id: 用户ID
            
        Returns:
            FoodRecognitionResponse: 识别结果
        """
        try:
            # 读取图片数据
            image_data = await image.read()
            
            # 保存图片
            file_extension = image.filename.split('.')[-1] if image.filename else 'jpg'
            file_path = f"food/{user_id}/{uuid4()}.{file_extension}"
            image_url = await self.storage.save(image_data, file_path)
            
            # 识别食物
            recognition_result, total_confidence = await self.classifier.identify(image_data)
            
            # 构建识别结果
            foods = []
            # 检查返回的是食物信息列表还是名称列表（兼容旧格式）
            if isinstance(recognition_result, list) and len(recognition_result) > 0:
                if isinstance(recognition_result[0], dict):
                    # 新格式：直接使用模型返回的完整信息
                    for food_info in recognition_result:
                        food_name = food_info.get("name", "未知食物")
                        food_weight = food_info.get("weight", 200.0)
                        cooking_method = food_info.get("cooking_method")
                        
                        # 计算该食物的营养成分
                        nutrition = await self._calculate_single_food_nutrition(
                            food_name, food_weight, cooking_method
                        )
                        
                        foods.append(FoodItem(
                            name=food_name,
                            weight=food_weight,
                            confidence=food_info.get("confidence", total_confidence),
                            cooking_method=cooking_method,
                            # 营养成分
                            calories=nutrition.get("calories"),
                            carbs=nutrition.get("carbs"),
                            net_carbs=nutrition.get("net_carbs"),
                            protein=nutrition.get("protein"),
                            fat=nutrition.get("fat"),
                            fiber=nutrition.get("fiber"),
                            gi_value=nutrition.get("gi_value"),
                            gl_value=nutrition.get("gl_value")
                        ))
                else:
                    # 旧格式：只有名称列表，需要估算重量
                    for food_name in recognition_result:
                        weight = await self.classifier.estimate_weight(image_data, food_name)
                        # 计算营养成分
                        nutrition = await self._calculate_single_food_nutrition(
                            food_name, weight, None
                        )
                        foods.append(FoodItem(
                            name=food_name,
                            weight=weight,
                            confidence=total_confidence,
                            cooking_method=None,
                            # 营养成分
                            calories=nutrition.get("calories"),
                            carbs=nutrition.get("carbs"),
                            net_carbs=nutrition.get("net_carbs"),
                            protein=nutrition.get("protein"),
                            fat=nutrition.get("fat"),
                            fiber=nutrition.get("fiber"),
                            gi_value=nutrition.get("gi_value"),
                            gl_value=nutrition.get("gl_value")
                        ))
            else:
                # 兼容处理：如果返回格式异常，使用默认值
                logger.warning("⚠️ 识别结果格式异常，使用默认值")
                foods.append(FoodItem(
                    name="未知食物",
                    weight=200.0,
                    confidence=0.5,
                    cooking_method=None
                ))
            
            # 获取用户信息和参数，计算建议食用量
            logger.info(f"👤 获取用户信息: user_id={user_id}")
            user_info = await user_crud.get_user_by_id(user_id) or {}
            user_params = await user_crud.get_user_parameters(user_id)
            logger.info(f"📋 用户信息: diabetes_type={user_info.get('diabetes_type')}, gender={user_info.get('gender')}")
            
            # 获取今日剩余营养额度
            from app.nutrition.daily_recommendation import DailyNutritionRecommendation
            from app.records import crud as records_crud
            daily_rec = DailyNutritionRecommendation()
            daily_recommendation = daily_rec.calculate_daily_recommendation(user_info, user_params)
            today_intake = await records_crud.get_today_nutrition_intake(user_id, datetime.utcnow())
            
            remaining_nutrition = {
                "calories": daily_recommendation.get("daily_calories", 0) - today_intake.get("total_calories", 0),
                "carbs": daily_recommendation.get("daily_carbs", 0) - today_intake.get("total_carbs", 0),
                "protein": daily_recommendation.get("daily_protein", 0) - today_intake.get("total_protein", 0),
                "fat": daily_recommendation.get("daily_fat", 0) - today_intake.get("total_fat", 0),
            }
            
            logger.info(f"📊 今日剩余营养额度: 热量={remaining_nutrition['calories']:.1f}kcal, "
                       f"碳水={remaining_nutrition['carbs']:.1f}g, "
                       f"蛋白质={remaining_nutrition['protein']:.1f}g, "
                       f"脂肪={remaining_nutrition['fat']:.1f}g")
            
            # 准备食物数据用于批量推荐计算
            foods_data = []
            for food in foods:
                foods_data.append({
                    "name": food.name,
                    "weight": food.weight or 0,
                    "calories": food.calories or 0,
                    "carbs": food.carbs or 0,
                    "protein": food.protein or 0,
                    "fat": food.fat or 0,
                    "gi_value": food.gi_value,
                    "gl_value": food.gl_value,
                })
            
            # 批量计算推荐（整体分析）
            logger.info(f"🍽️ 开始批量计算推荐（整体分析）...")
            recommendations_list = self.recommendation_calculator.calculate_batch_recommendation(
                foods_data=foods_data,
                remaining_nutrition=remaining_nutrition,
                user_info=user_info
            )
            
            # 为每个食物附加推荐信息
            foods_with_recommendations = []
            for food, recommendation_data in zip(foods, recommendations_list):
                recommendation = FoodRecommendation(**recommendation_data) if recommendation_data else None
                
                # 创建带建议的食物项
                food_with_recommendation = FoodItem(
                    name=food.name,
                    calories=food.calories,
                    weight=food.weight,
                    confidence=food.confidence,
                    cooking_method=food.cooking_method,
                    carbs=food.carbs,
                    net_carbs=food.net_carbs,
                    protein=food.protein,
                    fat=food.fat,
                    fiber=food.fiber,
                    gi_value=food.gi_value,
                    gl_value=food.gl_value,
                    recommendation=recommendation
                )
                foods_with_recommendations.append(food_with_recommendation)
            
            foods = foods_with_recommendations
            
            # 保存识别记录到数据库
            recognition_id = await self._save_recognition(
                user_id=user_id,
                image_url=image_url,
                foods=foods,
                total_confidence=total_confidence
            )
            
            return FoodRecognitionResponse(
                recognition_id=recognition_id,
                foods=foods,
                total_confidence=total_confidence,
                image_url=image_url
            )
            
        except Exception as e:
            logger.error(f"Food recognition error: {str(e)}")
            raise
    
    async def _save_recognition(
        self,
        user_id: str,
        image_url: str,
        foods: list,
        total_confidence: float
    ) -> str:
        """保存识别记录到数据库"""
        recognition_id = str(uuid4())
        
        # 构建识别结果 JSON
        recognition_result = {
            "foods": [
                {
                    "name": food.name,
                    "weight": food.weight,
                    "confidence": food.confidence,
                    "cooking_method": food.cooking_method
                }
                for food in foods
            ],
            "total_confidence": total_confidence
        }
        
        query = """
            INSERT INTO food_recognitions (id, user_id, image_url, recognition_result, created_at)
            VALUES (:id, :user_id, :image_url, CAST(:recognition_result AS JSONB), :created_at)
            RETURNING id
        """
        
        # 使用 JSONB 需要转换为字符串或使用 databases 库的 JSON 支持
        import json
        values = {
            "id": recognition_id,
            "user_id": user_id,
            "image_url": image_url,
            "recognition_result": json.dumps(recognition_result),  # 转换为 JSON 字符串
            "created_at": datetime.utcnow()
        }
        
        result = await database.fetch_one(query=query, values=values)
        return str(result["id"])

    def _translate_cooking_method(self, cooking_method: Optional[str]) -> Optional[str]:
        """将中文烹饪方式转换为英文
        
        Args:
            cooking_method: 中文烹饪方式
            
        Returns:
            str: 英文烹饪方式
        """
        if not cooking_method:
            return None
        
        # 中文到英文的映射
        translation_map = {
            "烤": "roasted",
            "炸": "fried",
            "煎": "fried",
            "蒸": "steamed",
            "煮": "boiled",
            "红烧": "braised",
            "炖": "braised",
            "炒": "fried",
            "生": "raw",
            "融化": "melted"
        }
        
        # 直接匹配
        if cooking_method in translation_map:
            return translation_map[cooking_method]
        
        # 模糊匹配
        for chinese, english in translation_map.items():
            if chinese in cooking_method or cooking_method in chinese:
                return english
        
        return None
    
    async def _calculate_single_food_nutrition(
        self, 
        food_name: str, 
        weight: float, 
        cooking_method: Optional[str]
    ) -> dict:
        """计算单个食物的营养成分
        
        Args:
            food_name: 食物名称
            weight: 食物重量（克）
            cooking_method: 烹饪方式（中文或英文）
            
        Returns:
            dict: 营养成分信息
        """
        try:
            # 转换烹饪方式为英文
            cooking_method_en = self._translate_cooking_method(cooking_method)
            
            # 获取基础营养成分（每100g）
            base_nutrition = await self.nutrition_calculator._get_food_nutrition(food_name)
            
            # 计算分量比例
            weight_ratio = weight / 100.0
            
            # 计算实际营养成分
            carbs = base_nutrition["carbs"] * weight_ratio
            protein = base_nutrition["protein"] * weight_ratio
            fat = base_nutrition["fat"] * weight_ratio
            fiber = base_nutrition["fiber"] * weight_ratio
            calories = base_nutrition["calories"] * weight_ratio
            
            # 计算净碳水（扣除纤维）
            net_carbs = carbs - fiber
            
            # 应用烹饪方式影响
            cooking_factor = self.nutrition_calculator._get_cooking_factor(cooking_method_en)
            gi_value = base_nutrition.get("gi_value", 0) * cooking_factor if base_nutrition.get("gi_value") else None
            
            # 计算血糖负荷 (GL = GI × 碳水含量 / 100)
            gl_value = (gi_value * carbs / 100.0) if gi_value and carbs > 0 else None
            
            return {
                "calories": round(calories, 2),
                "carbs": round(carbs, 2),
                "net_carbs": round(net_carbs, 2),
                "protein": round(protein, 2),
                "fat": round(fat, 2),
                "fiber": round(fiber, 2),
                "gi_value": round(gi_value, 1) if gi_value else None,
                "gl_value": round(gl_value, 2) if gl_value else None
            }
        except Exception as e:
            logger.warning(f"计算营养成分失败 {food_name}: {str(e)}")
            return {
                "calories": None,
                "carbs": None,
                "net_carbs": None,
                "protein": None,
                "fat": None,
                "fiber": None,
                "gi_value": None,
                "gl_value": None
            }
    
    async def search_foods(self, query: str) -> List[FoodItem]:
        """根据食物名称搜索食物，返回包含卡路里的食物列表"""
        search_results = await self.nutrition_calculator.search_food_by_name(query)
        return [
            FoodItem(
                name=r["name"],
                calories=r["calories"],
                weight=None, # 搜索结果通常不包含重量和置信度
                confidence=None
            )
            for r in search_results
        ]

