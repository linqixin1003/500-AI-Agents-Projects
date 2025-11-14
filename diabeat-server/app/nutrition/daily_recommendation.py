"""每日推荐营养摄入计算器"""
from typing import Optional, Dict, Any
from datetime import date
from app.food.recommendation_calculator import FoodRecommendationCalculator
import logging
from openai import OpenAI
import os
import json

logger = logging.getLogger(__name__)

class DailyNutritionRecommendation:
    """每日推荐营养摄入计算器"""
    
    def __init__(self):
        self.recommendation_calculator = FoodRecommendationCalculator()
        # 初始化OpenAI客户端（通义千问）
        api_key = os.getenv("DASHSCOPE_API_KEY")
        self.client = OpenAI(
            api_key=api_key,
            base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
        ) if api_key else None
    
    def calculate_daily_recommendation_with_ai(
        self,
        user_info: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        使用AI根据用户信息生成每日营养建议
        
        Args:
            user_info: 用户基本信息（身高、体重、年龄、性别、糖尿病类型等）
            
        Returns:
            Dict: 每日营养推荐
        """
        if not self.client:
            logger.warning("AI客户端未初始化，使用传统计算方法")
            return self.calculate_daily_recommendation(user_info, None)
        
        try:
            # 构建提示词
            gender_cn = {"male": "男性", "female": "女性", "other": "其它"}.get(user_info.get('gender', 'female'), "女性")
            diabetes_cn = {"type1": "1型糖尿病", "type2": "2型糖尿病", "gestational": "妊娠糖尿病"}.get(user_info.get('diabetes_type', 'type2'), "2型糖尿病")
            
            prompt = f"""你是一位专业的糖尿病营养师。请根据以下患者信息，计算每日营养摄入建议：

患者信息：
- 性别: {gender_cn}
- 年龄: {user_info.get('age', 30)}岁
- 身高: {user_info.get('height', 163)}cm
- 体重: {user_info.get('weight', 70)}kg
- 糖尿病类型: {diabetes_cn}
- 活动水平: 中等活动量

请计算并返回以下内容，**必须严格按照JSON格式返回**：
{{
    "daily_calories": 每日总热量(kcal，数字),
    "daily_carbs": 每日碳水化合物(g，数字),
    "daily_protein": 每日蛋白质(g，数字),
    "daily_fat": 每日脂肪(g，数字),
    "daily_fiber": 每日膳食纤维(g，数字),
    "carbs_per_meal": 每餐碳水化合物(g，数字),
    "bmr": 基础代谢率(kcal，数字),
    "reasoning": "简要说明计算依据"
}}

计算原则：
1. 糖尿病患者碳水化合物应占总热量的45-60%
2. 蛋白质占15-20%
3. 脂肪占25-35%
4. 每日膳食纤维建议25-30g
5. 考虑患者的BMI和活动水平
6. 一日三餐，碳水分配建议均匀

请只返回JSON，不要有其他解释文字。"""

            logger.info(f"调用AI生成营养建议: user={user_info.get('age')}岁 {gender_cn}")
            
            response = self.client.chat.completions.create(
                model="qwen-plus",
                messages=[
                    {"role": "system", "content": "你是一位专业的糖尿病营养师，擅长根据患者情况制定营养方案。必须返回纯JSON格式。"},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.3,
                max_tokens=1000
            )
            
            content = response.choices[0].message.content.strip()
            logger.info(f"AI响应: {content[:200]}...")
            
            # 解析JSON响应
            # 提取JSON内容（可能包含在```json```代码块中）
            if "```json" in content:
                content = content.split("```json")[1].split("```")[0].strip()
            elif "```" in content:
                content = content.split("```")[1].split("```")[0].strip()
            
            ai_result = json.loads(content)
            
            # 构建返回数据
            recommendation = {
                "daily_calories": float(ai_result.get("daily_calories", 2000)),
                "daily_carbs": float(ai_result.get("daily_carbs", 250)),
                "daily_protein": float(ai_result.get("daily_protein", 75)),
                "daily_fat": float(ai_result.get("daily_fat", 65)),
                "daily_fiber": float(ai_result.get("daily_fiber", 28)),
                "carbs_per_meal": float(ai_result.get("carbs_per_meal", 80)),
                "bmr": float(ai_result.get("bmr", 1500)),
                "activity_level": user_info.get("activity_level", "moderate"),
                "diabetes_type": user_info.get("diabetes_type", "type2"),
                "ai_reasoning": ai_result.get("reasoning", "")
            }
            
            logger.info(f"✅ AI生成营养建议成功: 热量={recommendation['daily_calories']}kcal")
            return recommendation
            
        except Exception as e:
            logger.error(f"AI生成营养建议失败: {str(e)}, 使用传统计算方法")
            return self.calculate_daily_recommendation(user_info, None)
    
    def calculate_daily_recommendation(
        self,
        user_info: Dict[str, Any],
        user_params: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """计算每日推荐营养摄入（传统方法）
        
        Args:
            user_info: 用户信息（包含 age, gender, weight, height, diabetes_type, date_of_birth 等）
            user_params: 用户参数（可选）
            
        Returns:
            dict: 包含每日推荐营养摄入量
        """
        try:
            # 获取用户信息（使用默认值填充缺失项）
            age = user_info.get("age")
            if not age and user_info.get("date_of_birth"):
                age = self.recommendation_calculator.calculate_age(user_info.get("date_of_birth"))
            if not age:
                age = 30  # 默认年龄
                
            gender = user_info.get("gender", self.recommendation_calculator.DEFAULT_GENDER) or self.recommendation_calculator.DEFAULT_GENDER
            height = float(user_info.get("height") or self.recommendation_calculator.DEFAULT_HEIGHT)
            weight = float(user_info.get("weight") or self.recommendation_calculator.DEFAULT_WEIGHT)
            diabetes_type = user_info.get("diabetes_type", self.recommendation_calculator.DEFAULT_DIABETES_TYPE)
            activity_level = user_info.get("activity_level", self.recommendation_calculator.DEFAULT_ACTIVITY_LEVEL)
            
            # 计算基础代谢率（BMR）- 使用 Mifflin-St Jeor 公式
            bmr_coeff = self.recommendation_calculator.BMR_COEFFICIENTS.get(gender, self.recommendation_calculator.BMR_COEFFICIENTS["male"])
            bmr = (
                bmr_coeff["base"] +
                bmr_coeff["weight"] * weight +
                bmr_coeff["height"] * (height / 100.0) -  # 身高转换为米
                bmr_coeff["age"] * age
            )
            
            # 计算每日总热量需求（TDEE = BMR × 活动系数）
            activity_factor = self.recommendation_calculator.ACTIVITY_FACTORS.get(activity_level, 1.55)
            daily_calories = bmr * activity_factor
            
            # 根据糖尿病类型调整热量需求
            # 2型糖尿病可能需要减少热量摄入
            if diabetes_type == "type2":
                daily_calories *= 0.9  # 减少10%
            elif diabetes_type == "type1":
                daily_calories *= 1.0  # 保持正常
            elif diabetes_type == "gestational":
                daily_calories *= 1.1  # 增加10%（妊娠期需要更多营养）
            
            # 计算每餐建议碳水摄入量
            carbs_per_meal = self.recommendation_calculator.get_recommended_carbs_per_meal(diabetes_type, activity_level)
            daily_carbs = carbs_per_meal * 3  # 假设一日三餐
            
            # 计算蛋白质需求（每公斤体重1.0-1.2g，糖尿病患者可能需要更多）
            protein_per_kg = 1.1 if diabetes_type == "type2" else 1.0
            daily_protein = weight * protein_per_kg
            
            # 计算脂肪需求（占总热量的25-35%）
            # 1g脂肪 = 9kcal
            fat_calories = daily_calories * 0.30  # 30%的热量来自脂肪
            daily_fat = fat_calories / 9.0
            
            # 计算纤维需求（每1000kcal需要14g纤维）
            daily_fiber = (daily_calories / 1000.0) * 14.0
            
            return {
                "daily_calories": round(daily_calories, 0),
                "daily_carbs": round(daily_carbs, 1),
                "daily_protein": round(daily_protein, 1),
                "daily_fat": round(daily_fat, 1),
                "daily_fiber": round(daily_fiber, 1),
                "carbs_per_meal": round(carbs_per_meal, 1),
                "bmr": round(bmr, 0),
                "activity_level": activity_level,
                "diabetes_type": diabetes_type
            }
        except Exception as e:
            logger.error(f"计算每日推荐营养失败: {str(e)}", exc_info=True)
            # 返回默认值
            return {
                "daily_calories": 2000.0,
                "daily_carbs": 135.0,
                "daily_protein": 70.0,
                "daily_fat": 65.0,
                "daily_fiber": 28.0,
                "carbs_per_meal": 45.0,
                "bmr": 1600.0,
                "activity_level": "moderate",
                "diabetes_type": "type2"
            }
