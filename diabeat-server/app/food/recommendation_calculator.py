"""食物建议计算器 - 根据用户信息计算建议食用量"""
from typing import Optional, Dict, Any, List
from datetime import datetime, date
import logging

logger = logging.getLogger(__name__)

class FoodRecommendationCalculator:
    """食物建议计算器 - 根据糖尿病人的个人信息计算建议食用量"""
    
    # 默认值（当用户信息缺失时使用）
    DEFAULT_AGE = 45  # 默认年龄
    DEFAULT_WEIGHT = 70.0  # 默认体重（kg）
    DEFAULT_HEIGHT = 175.0  # 默认身高（cm）
    DEFAULT_GENDER = "male"  # 默认性别
    DEFAULT_DIABETES_TYPE = "type2"  # 默认糖尿病类型
    DEFAULT_ACTIVITY_LEVEL = "moderate"  # 默认活动水平
    
    # 每餐建议碳水摄入量（克）- 根据糖尿病类型和活动水平
    CARBS_PER_MEAL = {
        "type1": {
            "sedentary": 45,  # 久坐
            "moderate": 60,   # 中等活动
            "active": 75      # 活跃
        },
        "type2": {
            "sedentary": 30,
            "moderate": 45,
            "active": 60
        },
        "gestational": {
            "sedentary": 40,
            "moderate": 50,
            "active": 60
        },
        "prediabetes": {
            "sedentary": 35,
            "moderate": 50,
            "active": 65
        }
    }
    
    # 根据年龄和性别的基础代谢率（BMR）系数
    # 用于计算每日总热量需求
    BMR_COEFFICIENTS = {
        "male": {
            "base": 88.362,
            "weight": 13.397,
            "height": 4.799,
            "age": 5.677
        },
        "female": {
            "base": 447.593,
            "weight": 9.247,
            "height": 3.098,
            "age": 4.330
        }
    }
    
    # 活动水平系数（用于计算总热量需求）
    ACTIVITY_FACTORS = {
        "sedentary": 1.2,    # 久坐（很少运动）
        "light": 1.375,      # 轻度活动（每周1-3天轻度运动）
        "moderate": 1.55,    # 中等活动（每周3-5天中等强度运动）
        "active": 1.725,     # 活跃（每周6-7天高强度运动）
        "very_active": 1.9   # 非常活跃（每天高强度运动或体力工作）
    }
    
    def calculate_age(self, date_of_birth: Optional[date]) -> int:
        """计算年龄"""
        if not date_of_birth:
            return self.DEFAULT_AGE
        
        today = date.today()
        age = today.year - date_of_birth.year
        if today.month < date_of_birth.month or (today.month == date_of_birth.month and today.day < date_of_birth.day):
            age -= 1
        return age
    
    def get_recommended_carbs_per_meal(
        self,
        diabetes_type: str,
        activity_level: Optional[str] = None
    ) -> float:
        """获取每餐建议的碳水摄入量（克）"""
        activity = activity_level or self.DEFAULT_ACTIVITY_LEVEL
        
        # 标准化活动水平
        if activity not in ["sedentary", "moderate", "active"]:
            if activity in ["light"]:
                activity = "sedentary"
            elif activity in ["very_active"]:
                activity = "active"
            else:
                activity = "moderate"
        
        diabetes = diabetes_type.lower() if diabetes_type else self.DEFAULT_DIABETES_TYPE
        if diabetes not in self.CARBS_PER_MEAL:
            diabetes = self.DEFAULT_DIABETES_TYPE
        
        return float(self.CARBS_PER_MEAL[diabetes].get(activity, 45))
    
    def calculate_recommended_food_amount(
        self,
        food_name: str,
        food_carbs_per_100g: float,
        food_gi_value: Optional[float],
        food_gl_value: Optional[float],
        current_weight: float,
        user_info: Dict[str, Any],
        user_params: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """计算建议的食物食用量
        
        Args:
            food_name: 食物名称
            food_carbs_per_100g: 每100g食物的碳水含量
            food_gi_value: 升糖指数
            food_gl_value: 血糖负荷
            current_weight: 当前识别出的食物重量（克）
            user_info: 用户信息（包含 age, gender, diabetes_type, date_of_birth 等）
            user_params: 用户参数（包含 target_bg_low, target_bg_high 等）
            
        Returns:
            dict: 包含建议食用量、原因等信息
        """
        try:
            # 获取用户信息（使用默认值填充缺失项）
            age = self.calculate_age(user_info.get("date_of_birth"))
            gender = user_info.get("gender", self.DEFAULT_GENDER) or self.DEFAULT_GENDER
            height = user_info.get("height") or self.DEFAULT_HEIGHT
            weight = user_info.get("weight") or self.DEFAULT_WEIGHT
            diabetes_type = user_info.get("diabetes_type", self.DEFAULT_DIABETES_TYPE)
            activity_level = user_info.get("activity_level", self.DEFAULT_ACTIVITY_LEVEL)
            
            # 获取每餐建议碳水摄入量
            recommended_carbs = self.get_recommended_carbs_per_meal(diabetes_type, activity_level)
            
            # 如果食物没有碳水或碳水很少，建议保持原量或适量增加
            if food_carbs_per_100g <= 0:
                return {
                    "recommended_weight": current_weight,
                    "recommended_carbs": 0.0,
                    "reason": "该食物不含或含极少碳水化合物，可以适量食用",
                    "adjustment_factor": 1.0,
                    "warning": None
                }
            
            # 根据GI值调整建议
            gi_adjustment = 1.0
            warning = None
            
            if food_gi_value:
                if food_gi_value > 70:  # 高GI食物
                    gi_adjustment = 0.8  # 减少20%建议量
                    warning = "高GI食物，建议减少食用量并搭配低GI食物"
                elif food_gi_value < 55:  # 低GI食物
                    gi_adjustment = 1.1  # 可以增加10%建议量
                # 中等GI (55-70) 保持原建议量
            
            # 根据GL值进一步调整
            if food_gl_value:
                if food_gl_value > 20:  # 高GL
                    gi_adjustment *= 0.85  # 进一步减少
                    if not warning:
                        warning = "高血糖负荷，建议减少食用量"
                elif food_gl_value < 10:  # 低GL
                    gi_adjustment *= 1.05  # 可以稍微增加
            
            # 计算建议的碳水摄入量（考虑GI调整）
            adjusted_recommended_carbs = recommended_carbs * gi_adjustment
            
            # 计算建议的食物重量（克）
            # 建议重量 = (建议碳水 / 每100g碳水) * 100
            recommended_weight = (adjusted_recommended_carbs / food_carbs_per_100g) * 100
            
            # 限制建议重量范围（避免极端值）
            min_weight = 10.0  # 最小10克
            max_weight = 500.0  # 最大500克（单次）
            
            if recommended_weight < min_weight:
                recommended_weight = min_weight
            elif recommended_weight > max_weight:
                recommended_weight = max_weight
                warning = "建议食用量较大，建议分次食用或咨询医生"
            
            # 计算实际建议的碳水摄入量
            actual_recommended_carbs = (recommended_weight / 100.0) * food_carbs_per_100g
            
            # 生成建议原因
            reason_parts = []
            if food_gi_value:
                if food_gi_value > 70:
                    reason_parts.append("高GI食物")
                elif food_gi_value < 55:
                    reason_parts.append("低GI食物")
                else:
                    reason_parts.append("中等GI食物")
            
            if diabetes_type == "type1":
                reason_parts.append("1型糖尿病患者")
            elif diabetes_type == "type2":
                reason_parts.append("2型糖尿病患者")
            
            reason = f"基于您的{'、'.join(reason_parts) if reason_parts else '个人情况'}，建议每餐摄入约{recommended_carbs:.0f}g碳水化合物"
            
            # 与当前识别重量比较
            adjustment_factor = recommended_weight / current_weight if current_weight > 0 else 1.0
            adjustment_percent = ((recommended_weight - current_weight) / current_weight * 100) if current_weight > 0 else 0
            
            return {
                "recommended_weight": round(recommended_weight, 1),
                "recommended_carbs": round(actual_recommended_carbs, 1),
                "reason": reason,
                "adjustment_factor": round(adjustment_factor, 2),
                "adjustment_percent": round(adjustment_percent, 1),
                "warning": warning,
                "current_weight": round(current_weight, 1),
                "gi_value": food_gi_value,
                "gl_value": food_gl_value
            }
            
        except Exception as e:
            logger.error(f"计算食物建议失败 {food_name}: {str(e)}")
            # 返回默认建议（保持原量）
            return {
                "recommended_weight": current_weight,
                "recommended_carbs": (current_weight / 100.0) * food_carbs_per_100g,
                "reason": "基于一般建议，保持当前食用量",
                "adjustment_factor": 1.0,
                "adjustment_percent": 0.0,
                "warning": None,
                "current_weight": current_weight,
                "gi_value": food_gi_value,
                "gl_value": food_gl_value
            }
    
    def calculate_batch_recommendation(
        self,
        foods_data: List[Dict[str, Any]],
        remaining_nutrition: Dict[str, float],
        user_info: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """批量计算食物推荐（整体分析）
        
        Args:
            foods_data: 食物列表，每项包含 {name, weight, carbs, protein, fat, calories, gi_value, gl_value}
            remaining_nutrition: 今日剩余营养额度 {calories, carbs, protein, fat}
            user_info: 用户信息
            
        Returns:
            List[Dict]: 每个食物的推荐信息
        """
        try:
            # 1. 计算所有食物的营养总和
            total_nutrition = {
                "calories": sum(food.get("calories", 0) for food in foods_data),
                "carbs": sum(food.get("carbs", 0) for food in foods_data),
                "protein": sum(food.get("protein", 0) for food in foods_data),
                "fat": sum(food.get("fat", 0) for food in foods_data),
            }
            
            logger.info(f"📊 本次食物营养总和: 热量={total_nutrition['calories']:.1f}kcal, "
                       f"碳水={total_nutrition['carbs']:.1f}g, 蛋白质={total_nutrition['protein']:.1f}g, "
                       f"脂肪={total_nutrition['fat']:.1f}g")
            logger.info(f"📊 今日剩余额度: 热量={remaining_nutrition.get('calories', 0):.1f}kcal, "
                       f"碳水={remaining_nutrition.get('carbs', 0):.1f}g, "
                       f"蛋白质={remaining_nutrition.get('protein', 0):.1f}g, "
                       f"脂肪={remaining_nutrition.get('fat', 0):.1f}g")
            
            # 2. 判断是否超标（任意一项超标则需要调整）
            is_over_calories = total_nutrition["calories"] > remaining_nutrition.get("calories", float('inf'))
            is_over_carbs = total_nutrition["carbs"] > remaining_nutrition.get("carbs", float('inf'))
            is_over_protein = total_nutrition["protein"] > remaining_nutrition.get("protein", float('inf'))
            is_over_fat = total_nutrition["fat"] > remaining_nutrition.get("fat", float('inf'))
            
            is_over_limit = is_over_calories or is_over_carbs or is_over_protein or is_over_fat
            
            if not is_over_limit:
                # 3. 所有指标都不超标 - 可以全部食用
                logger.info("✅ 所有营养指标均未超标，建议全部食用")
                recommendations = []
                for food in foods_data:
                    recommendations.append({
                        "recommended_weight": food["weight"],
                        "recommended_carbs": food.get("carbs", 0),
                        "reason": "本次食物营养未超出今日剩余额度，可以全部食用",
                        "adjustment_factor": 1.0,
                        "adjustment_percent": 0.0,
                        "warning": None,
                        "current_weight": food["weight"],
                        "gi_value": food.get("gi_value"),
                        "gl_value": food.get("gl_value"),
                        "can_eat_all": True  # 标记可以全部食用
                    })
                return recommendations
            
            # 4. 有指标超标 - 需要按比例缩减
            logger.warning(f"⚠️ 营养指标超标: 热量={is_over_calories}, 碳水={is_over_carbs}, "
                          f"蛋白质={is_over_protein}, 脂肪={is_over_fat}")
            
            # 计算最严格的缩减比例（取最小的比例）
            ratios = []
            if is_over_calories and total_nutrition["calories"] > 0:
                ratios.append(remaining_nutrition.get("calories", 0) / total_nutrition["calories"])
            if is_over_carbs and total_nutrition["carbs"] > 0:
                ratios.append(remaining_nutrition.get("carbs", 0) / total_nutrition["carbs"])
            if is_over_protein and total_nutrition["protein"] > 0:
                ratios.append(remaining_nutrition.get("protein", 0) / total_nutrition["protein"])
            if is_over_fat and total_nutrition["fat"] > 0:
                ratios.append(remaining_nutrition.get("fat", 0) / total_nutrition["fat"])
            
            # 使用最小比例确保所有指标都不超
            reduction_ratio = min(ratios) if ratios else 1.0
            reduction_ratio = max(0.3, min(1.0, reduction_ratio))  # 限制在30%-100%之间
            
            logger.info(f"📉 建议缩减比例: {reduction_ratio:.1%}")
            
            # 生成超标原因
            over_items = []
            if is_over_calories:
                over_items.append(f"热量超出{total_nutrition['calories'] - remaining_nutrition.get('calories', 0):.0f}kcal")
            if is_over_carbs:
                over_items.append(f"碳水超出{total_nutrition['carbs'] - remaining_nutrition.get('carbs', 0):.1f}g")
            if is_over_protein:
                over_items.append(f"蛋白质超出{total_nutrition['protein'] - remaining_nutrition.get('protein', 0):.1f}g")
            if is_over_fat:
                over_items.append(f"脂肪超出{total_nutrition['fat'] - remaining_nutrition.get('fat', 0):.1f}g")
            
            warning = f"本次食物{' '.join(over_items)}，建议按比例减少食用量"
            
            # 5. 为每个食物生成缩减后的建议
            recommendations = []
            for food in foods_data:
                recommended_weight = food["weight"] * reduction_ratio
                adjustment_percent = (reduction_ratio - 1.0) * 100
                
                recommendations.append({
                    "recommended_weight": round(recommended_weight, 1),
                    "recommended_carbs": round(food.get("carbs", 0) * reduction_ratio, 1),
                    "reason": f"基于今日剩余营养额度，建议按{reduction_ratio:.0%}比例食用",
                    "adjustment_factor": round(reduction_ratio, 2),
                    "adjustment_percent": round(adjustment_percent, 1),
                    "warning": warning if food == foods_data[0] else None,  # 只在第一个食物上显示警告
                    "current_weight": food["weight"],
                    "gi_value": food.get("gi_value"),
                    "gl_value": food.get("gl_value"),
                    "can_eat_all": False  # 标记不能全部食用
                })
            
            return recommendations
            
        except Exception as e:
            logger.error(f"批量计算推荐失败: {str(e)}", exc_info=True)
            # 返回保持原量的默认建议
            return [{
                "recommended_weight": food["weight"],
                "recommended_carbs": food.get("carbs", 0),
                "reason": "计算出错，建议保持当前量",
                "adjustment_factor": 1.0,
                "adjustment_percent": 0.0,
                "warning": None,
                "current_weight": food["weight"],
                "gi_value": food.get("gi_value"),
                "gl_value": food.get("gl_value"),
                "can_eat_all": None
            } for food in foods_data]

