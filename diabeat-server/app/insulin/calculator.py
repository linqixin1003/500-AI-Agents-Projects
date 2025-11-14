from typing import Dict, Any, Optional, List
from datetime import datetime
import logging

logger = logging.getLogger(__name__)

class InsulinCalculator:
    """胰岛素剂量计算器"""
    
    # 活动水平调整因子
    ACTIVITY_FACTORS = {
        "sedentary": 1.0,    # 久坐：无调整
        "light": 0.95,       # 轻度活动：减少5%
        "moderate": 0.90,    # 中度活动：减少10%
        "vigorous": 0.85     # 剧烈活动：减少15%
    }
    
    # 时间因子（考虑昼夜节律）
    def get_time_factor(self, meal_time: Optional[str] = None) -> float:
        """获取时间因子
        
        Args:
            meal_time: 用餐时间（ISO格式）
            
        Returns:
            float: 时间因子
        """
        if not meal_time:
            return 1.0
        
        try:
            meal_dt = datetime.fromisoformat(meal_time.replace('Z', '+00:00'))
            hour = meal_dt.hour
            
            # 早餐（6-9点）：可能需要更多胰岛素（黎明现象）
            if 6 <= hour < 9:
                return 1.1
            # 午餐（11-14点）：正常
            elif 11 <= hour < 14:
                return 1.0
            # 晚餐（17-20点）：可能需要更少胰岛素
            elif 17 <= hour < 20:
                return 0.95
            # 其他时间：正常
            else:
                return 1.0
        except:
            return 1.0
    
    def calculate_insulin_dose(
        self,
        total_carbs: float,
        current_bg: float,
        isf: float,  # Insulin Sensitivity Factor
        icr: float,  # Insulin-to-Carb Ratio
        target_bg: float,
        activity_level: str = "sedentary",
        meal_time: Optional[str] = None,
        gi_value: Optional[float] = None,
        max_dose: Optional[float] = None,
        min_dose: float = 0.5
    ) -> Dict[str, Any]:
        """计算胰岛素剂量
        
        Args:
            total_carbs: 总碳水化合物（克）
            current_bg: 当前血糖值（mmol/L）
            isf: 胰岛素敏感系数（每单位胰岛素降低的血糖值）
            icr: 碳水比例（每单位胰岛素覆盖的碳水克数）
            target_bg: 目标血糖值（mmol/L）
            activity_level: 活动水平
            meal_time: 用餐时间
            gi_value: 升糖指数（可选）
            max_dose: 最大剂量（可选）
            min_dose: 最小剂量
            
        Returns:
            Dict: 计算结果
        """
        warnings: List[str] = []
        
        # 1. 计算碳水胰岛素
        carb_insulin = total_carbs / icr if icr > 0 else 0
        
        # 2. 计算血糖校正剂量
        correction_insulin = 0.0
        if current_bg > target_bg:
            bg_diff = current_bg - target_bg
            correction_insulin = bg_diff / isf if isf > 0 else 0
        
        # 3. 活动水平调整
        activity_factor = self.ACTIVITY_FACTORS.get(activity_level.lower(), 1.0)
        activity_adjustment = (carb_insulin + correction_insulin) * (activity_factor - 1.0)
        
        # 4. 时间因子调整
        time_factor = self.get_time_factor(meal_time)
        
        # 5. GI值调整（高GI食物可能需要提前或增加剂量）
        gi_factor = 1.0
        if gi_value:
            if gi_value > 70:  # 高GI
                gi_factor = 1.05
                warnings.append("高GI食物，建议餐前15-30分钟注射")
            elif gi_value < 55:  # 低GI
                gi_factor = 0.95
                warnings.append("低GI食物，可以考虑餐时或餐后注射")
        
        # 6. 综合计算
        total_dose = (carb_insulin + correction_insulin) * activity_factor * time_factor * gi_factor
        
        # 7. 应用安全限制
        if max_dose and total_dose > max_dose:
            warnings.append(f"建议剂量超过最大限制，已调整为 {max_dose} 单位")
            total_dose = max_dose
        
        if total_dose < min_dose:
            total_dose = min_dose
        
        # 8. 确定注射时机
        injection_timing = self._get_injection_timing(gi_value)
        
        # 9. 判断是否需要分次注射
        split_dose = self._should_split_dose(gi_value, total_dose)
        if split_dose:
            warnings.append("建议分次注射以更好地控制血糖")
        
        # 10. 风险评估
        risk_level = self._assess_risk(total_dose, current_bg, target_bg)
        if risk_level == "high":
            warnings.append("高风险：建议咨询医生")
        
        return {
            "recommended_dose": round(total_dose, 2),
            "carb_insulin": round(carb_insulin, 2),
            "correction_insulin": round(correction_insulin, 2),
            "activity_adjustment": round(activity_adjustment, 2),
            "injection_timing": injection_timing,
            "split_dose": split_dose,
            "risk_level": risk_level,
            "warnings": warnings
        }
    
    def _get_injection_timing(self, gi_value: Optional[float]) -> str:
        """获取建议注射时机"""
        if not gi_value:
            return "餐前0-15分钟"
        
        if gi_value > 70:
            return "餐前15-30分钟"
        elif gi_value > 55:
            return "餐前0-15分钟"
        else:
            return "餐时或餐后"
    
    def _should_split_dose(self, gi_value: Optional[float], total_dose: float) -> bool:
        """判断是否需要分次注射"""
        # 高GI食物且剂量较大时，建议分次注射
        if gi_value and gi_value > 70 and total_dose > 8:
            return True
        return False
    
    def _assess_risk(self, dose: float, current_bg: float, target_bg: float) -> str:
        """评估风险等级"""
        # 如果剂量很大
        if dose > 15:
            return "high"
        
        # 如果当前血糖很高且剂量很大
        if current_bg > 15 and dose > 10:
            return "high"
        
        # 如果当前血糖很低
        if current_bg < 4.0:
            return "high"
        
        # 如果剂量较大
        if dose > 10:
            return "medium"
        
        return "low"

