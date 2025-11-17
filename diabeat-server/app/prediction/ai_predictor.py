"""
AI增强的血糖预测器
使用通义千问qwen-max模型进行专业的医学推理和预测
"""
from typing import Dict, Any, Optional, List
import logging
import json
from openai import OpenAI
from app.config import settings
from app.prediction.predictor import BloodGlucosePredictor

logger = logging.getLogger(__name__)

class AIBloodGlucosePredictor:
    """使用AI模型的血糖预测器"""
    
    def __init__(self):
        """初始化AI预测器"""
        self.model = "qwen3-max"  # 使用最强的qwen3-max模型
        self.rule_based_predictor = BloodGlucosePredictor()  # Fallback
        
        # 初始化通义千问客户端
        if settings.DASHSCOPE_API_KEY:
            self.client = OpenAI(
                api_key=settings.DASHSCOPE_API_KEY,
                base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
            )
            self.ai_enabled = True
            logger.info(f"✅ AI血糖预测器初始化成功，使用模型: {self.model}")
        else:
            self.client = None
            self.ai_enabled = False
            logger.warning("⚠️ DASHSCOPE_API_KEY未配置，将使用规则引擎")
    
    async def predict(
        self,
        total_carbs: float,
        insulin_dose: float,
        current_bg: float,
        gi_value: Optional[float] = None,
        activity_level: str = "sedentary",
        user_bias: float = 0.0,
        correction_count: int = 0,
        meal_time: Optional[str] = None,
        medication_time: Optional[str] = None,
        current_time: Optional[str] = None,
        # 用户基础信息（个性化预测）
        weight: Optional[float] = None,
        height: Optional[float] = None,
        age: Optional[int] = None,
        gender: Optional[str] = None,
        diabetes_type: Optional[str] = None,
        # 历史记录（AI上下文）
        recent_meals: Optional[List] = None,
        recent_medications: Optional[List] = None,
        recent_exercises: Optional[List] = None,
        recent_water: Optional[List] = None
    ) -> Dict[str, Any]:
        """
        预测餐后血糖变化
        
        Args:
            total_carbs: 总碳水化合物（克）
            insulin_dose: 胰岛素剂量（单位）
            current_bg: 当前血糖值（mmol/L）
            gi_value: 升糖指数（可选）
            activity_level: 活动水平
            user_bias: 用户历史预测偏差
            correction_count: 历史纠正记录数
            meal_time: 餐点时间（可选）
            medication_time: 药物服用时间（可选）
            current_time: 当前时间（可选）
            
        Returns:
            Dict: 预测结果
        """
        if not self.ai_enabled:
            logger.info("使用规则引擎进行预测")
            return self.rule_based_predictor.predict(
                total_carbs, insulin_dose, current_bg, gi_value, activity_level
            )
        
        try:
            # 使用AI模型进行预测
            logger.info(f"🤖 使用AI模型({self.model})进行血糖预测")
            ai_result = await self._predict_with_ai(
                total_carbs, insulin_dose, current_bg, 
                gi_value, activity_level, user_bias, correction_count,
                meal_time, medication_time, current_time,
                weight, height, age, gender, diabetes_type,
                recent_meals, recent_medications, recent_exercises, recent_water
            )
            logger.info("✅ AI预测成功")
            return ai_result
            
        except Exception as e:
            logger.error(f"❌ AI预测失败: {e}, 降级使用规则引擎")
            # 降级到规则引擎
            return self.rule_based_predictor.predict(
                total_carbs, insulin_dose, current_bg, gi_value, activity_level
            )
    
    async def _predict_with_ai(
        self,
        total_carbs: float,
        insulin_dose: float,
        current_bg: float,
        gi_value: Optional[float],
        activity_level: str,
        user_bias: float,
        correction_count: int,
        meal_time: Optional[str] = None,
        medication_time: Optional[str] = None,
        current_time: Optional[str] = None,
        weight: Optional[float] = None,
        height: Optional[float] = None,
        age: Optional[int] = None,
        gender: Optional[str] = None,
        diabetes_type: Optional[str] = None,
        recent_meals: Optional[List] = None,
        recent_medications: Optional[List] = None,
        recent_exercises: Optional[List] = None,
        recent_water: Optional[List] = None
    ) -> Dict[str, Any]:
        """使用AI模型进行预测（支持时间感知 + 个性化 + 历史记录）"""
        
        # 导入时间感知预测器
        from .time_aware_predictor import time_aware_predictor
        
        # 检查是否有时间上下文
        time_context = time_aware_predictor.calculate_time_context(
            meal_time, medication_time, current_time
        )
        
        # 如果有时间上下文，使用时间感知预测
        if time_context['has_time_context']:
            # 碳水吸收建模
            carb_model = time_aware_predictor.model_carb_absorption(
                total_carbs,
                time_context['minutes_since_meal'],
                gi_value
            )
            
            # 胰岛素作用建模
            insulin_model = time_aware_predictor.model_insulin_effect(
                insulin_dose,
                time_context['minutes_since_medication']
            )
            
            # 生成时间感知prompt
            prompt = time_aware_predictor.generate_time_aware_prompt(
                type('Request', (), {
                    'current_bg': current_bg,
                    'total_carbs': total_carbs,
                    'insulin_dose': insulin_dose,
                    'activity_level': activity_level
                })(),
                time_context,
                carb_model,
                insulin_model,
                user_bias,
                correction_count
            )
            
            logger.info(f"✅ 使用时间感知预测: 餐后{time_context['minutes_since_meal']}分钟")
        else:
            # 回退到传统prompt
            prompt = self._build_prediction_prompt(
                total_carbs, insulin_dose, current_bg,
                gi_value, activity_level, user_bias, correction_count,
                weight, height, age, gender, diabetes_type,
                recent_meals, recent_medications, recent_exercises, recent_water
            )
            logger.info("使用传统预测（无时间上下文，但有用户信息和历史记录）")
        
        # 调用AI模型
        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {
                    "role": "system",
                    "content": self._get_system_prompt()
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            temperature=0.3,  # 降低随机性，提高一致性
            max_tokens=2000
        )
        
        # 解析AI返回结果
        result_text = response.choices[0].message.content
        logger.debug(f"AI返回: {result_text[:200]}...")
        
        # 提取JSON
        result = self._parse_ai_response(result_text)
        
        return result
    
    def _get_system_prompt(self) -> str:
        """获取系统prompt"""
        return """你是一位资深的糖尿病管理专家和内分泌科医生，拥有20年以上临床经验。

你的专长包括：
1. 血糖动态变化预测
2. 碳水化合物代谢分析  
3. 胰岛素剂量效果评估
4. 个体化血糖管理方案

重要原则：
- 基于循证医学和临床经验
- 考虑个体差异和历史数据
- 提供准确的数值预测
- 给出可操作的医学建议
- 必须返回规范的JSON格式

你的预测将直接影响患者的血糖管理决策，请务必准确和负责任。"""
    
    def _build_prediction_prompt(
        self,
        total_carbs: float,
        insulin_dose: float,
        current_bg: float,
        gi_value: Optional[float],
        activity_level: str,
        user_bias: float,
        correction_count: int,
        weight: Optional[float] = None,
        height: Optional[float] = None,
        age: Optional[int] = None,
        gender: Optional[str] = None,
        diabetes_type: Optional[str] = None,
        recent_meals: Optional[List] = None,
        recent_medications: Optional[List] = None,
        recent_exercises: Optional[List] = None,
        recent_water: Optional[List] = None
    ) -> str:
        """构建预测prompt（包含个性化信息 + 历史记录）"""
        
        # 活动水平中文映射
        activity_labels = {
            "sedentary": "久坐/无运动",
            "light": "轻度活动",
            "moderate": "中等强度运动",
            "vigorous": "高强度运动"
        }
        activity_cn = activity_labels.get(activity_level, activity_level)
        
        # 构建用户profile（如果有信息）
        user_profile_section = ""
        if any([weight, height, age, gender, diabetes_type]):
            user_profile_section = "\n**患者基础信息** (重要！个性化预测关键):\n"
            if weight and height:
                bmi = weight / ((height / 100) ** 2)
                user_profile_section += f"- 体重: {weight}kg, 身高: {height}cm (BMI: {bmi:.1f})\n"
            elif weight:
                user_profile_section += f"- 体重: {weight}kg\n"
            elif height:
                user_profile_section += f"- 身高: {height}cm\n"
            
            if age:
                user_profile_section += f"- 年龄: {age}岁\n"
            if gender:
                gender_cn = "男性" if gender == "male" else "女性" if gender == "female" else gender
                user_profile_section += f"- 性别: {gender_cn}\n"
            if diabetes_type:
                type_cn = {
                    "type1": "1型糖尿病（胰岛素依赖）",
                    "type2": "2型糖尿病（胰岛素抵抗）",
                    "gestational": "妊娠期糖尿病",
                    "prediabetes": "糖尿病前期"
                }.get(diabetes_type, diabetes_type)
                user_profile_section += f"- 糖尿病类型: {type_cn}\n"
        
        # ✅ 构建历史记录部分（带时间戳）
        history_section = ""
        if recent_meals or recent_medications or recent_exercises or recent_water:
            history_section = "\n**最近记录** (重要！帮助理解当前状态):\n"
            
            if recent_meals:
                history_section += "- 最近进食:\n"
                for meal in recent_meals[:3]:
                    meal_dict = meal if isinstance(meal, dict) else meal.dict()
                    foods_desc = meal_dict.get('foods', '未指定')
                    history_section += f"  * {meal_dict['meal_time']}: {meal_dict['total_carbs']}g碳水 ({foods_desc})\n"
            
            if recent_medications:
                history_section += "- 最近用药:\n"
                for med in recent_medications[:3]:
                    med_dict = med if isinstance(med, dict) else med.dict()
                    history_section += f"  * {med_dict['medication_time']}: {med_dict['medication_type']} {med_dict['dosage']}单位\n"
            
            if recent_exercises:
                history_section += "- 最近运动:\n"
                for ex in recent_exercises[:3]:
                    ex_dict = ex if isinstance(ex, dict) else ex.dict()
                    history_section += f"  * {ex_dict['exercise_time']}: {ex_dict['exercise_type']} {ex_dict['duration']}分钟\n"
            
            if recent_water:
                history_section += "- 最近饮水:\n"
                for water in recent_water[:3]:
                    water_dict = water if isinstance(water, dict) else water.dict()
                    history_section += f"  * {water_dict['record_time']}: {water_dict['amount']}ml\n"
        
        prompt = f"""请基于以下信息，预测这位糖尿病患者的餐后血糖变化：
{user_profile_section}{history_section}
**当前状态**:
- 当前血糖: {current_bg} mmol/L
- 即将摄入碳水化合物: {total_carbs}g
- 计划胰岛素剂量: {insulin_dose}单位
- 食物GI值: {gi_value if gi_value else '未知（假设中等GI=65）'}
- 活动水平: {activity_cn}

**历史数据** (用于个性化调整):
- 用户历史预测偏差: {user_bias:+.1f} mmol/L
- 历史纠正记录数: {correction_count}次

**请提供**:

1. **详细预测曲线**: 餐后30分钟、1小时、1.5小时、2小时、3小时、4小时的血糖值和置信度
2. **峰值预测**: 预计血糖峰值、峰值时间
3. **风险评估**: 
   - high: 峰值 > 13.9 mmol/L
   - medium: 峰值 10.0-13.9 mmol/L  
   - low: 峰值 < 10.0 mmol/L
4. **专业建议**: 基于预测结果的个性化医学建议（3-5条）

**分析要点**:
- 碳水化合物吸收速度（受GI值影响）
- 胰岛素作用时间曲线
- 运动对血糖的影响
- 用户个体差异（基于历史偏差）

请严格按照以下JSON格式返回，不要包含其他内容：

```json
{{
  "predictions": [
    {{"time_minutes": 30, "bg_value": 7.2, "confidence": 0.85}},
    {{"time_minutes": 60, "bg_value": 8.5, "confidence": 0.90}},
    {{"time_minutes": 90, "bg_value": 9.2, "confidence": 0.92}},
    {{"time_minutes": 120, "bg_value": 8.8, "confidence": 0.88}},
    {{"time_minutes": 180, "bg_value": 7.5, "confidence": 0.80}},
    {{"time_minutes": 240, "bg_value": 6.8, "confidence": 0.75}}
  ],
  "peak_time": 90,
  "peak_value": 9.2,
  "risk_level": "medium",
  "recommendations": [
    "建议餐后2小时监测血糖，确认实际值与预测值的偏差",
    "预测峰值在正常范围内，胰岛素剂量较为合适",
    "建议餐后30-60分钟进行轻度活动，有助于降低血糖峰值"
  ],
  "reasoning": "基于{total_carbs}g碳水和{insulin_dose}单位胰岛素，预计净升糖效应适中。考虑到{activity_cn}，血糖峰值预计在餐后90分钟左右出现。"
}}
```

注意：
1. bg_value范围: 3.0-20.0 mmol/L
2. confidence范围: 0.70-0.95
3. 所有数值保留1位小数
4. recommendations为数组，3-5条建议
"""
        
        return prompt
    
    def _parse_ai_response(self, response_text: str) -> Dict[str, Any]:
        """解析AI返回的JSON"""
        try:
            # 尝试找到JSON块
            if "```json" in response_text:
                # 提取```json```之间的内容
                start = response_text.find("```json") + 7
                end = response_text.find("```", start)
                json_text = response_text[start:end].strip()
            elif "```" in response_text:
                # 提取```之间的内容
                start = response_text.find("```") + 3
                end = response_text.find("```", start)
                json_text = response_text[start:end].strip()
            else:
                # 尝试直接解析
                json_text = response_text.strip()
            
            # 解析JSON
            result = json.loads(json_text)
            
            # 验证必要字段
            required_fields = ["predictions", "peak_time", "peak_value", "risk_level", "recommendations"]
            for field in required_fields:
                if field not in result:
                    raise ValueError(f"缺少必要字段: {field}")
            
            # 验证predictions格式
            if not isinstance(result["predictions"], list) or len(result["predictions"]) == 0:
                raise ValueError("predictions必须是非空数组")
            
            logger.info(f"✅ AI预测解析成功: 峰值={result['peak_value']}, 风险={result['risk_level']}")
            return result
            
        except json.JSONDecodeError as e:
            logger.error(f"JSON解析失败: {e}, 原始响应: {response_text[:200]}")
            raise ValueError(f"AI返回格式错误: {e}")
        except Exception as e:
            logger.error(f"AI响应解析失败: {e}")
            raise
    
    def is_enabled(self) -> bool:
        """检查AI预测是否可用"""
        return self.ai_enabled
