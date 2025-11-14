from typing import List, Dict, Any, Optional
import logging

logger = logging.getLogger(__name__)

class BloodGlucosePredictor:
    """血糖预测器（MVP版本 - 基于规则引擎）"""
    
    def predict(
        self,
        total_carbs: float,
        insulin_dose: float,
        current_bg: float,
        gi_value: Optional[float] = None,
        activity_level: str = "sedentary"
    ) -> Dict[str, Any]:
        """预测餐后血糖变化
        
        Args:
            total_carbs: 总碳水化合物（克）
            insulin_dose: 胰岛素剂量（单位）
            current_bg: 当前血糖值（mmol/L）
            gi_value: 升糖指数（可选）
            activity_level: 活动水平
            
        Returns:
            Dict: 预测结果
        """
        # MVP版本：使用简化的规则引擎
        # 实际应该使用机器学习模型（LSTM、XGBoost等）
        
        # 基础参数
        base_carb_impact = total_carbs * 0.15  # 每克碳水约升高0.15 mmol/L（简化）
        insulin_effect = insulin_dose * 2.0  # 每单位胰岛素约降低2.0 mmol/L（简化）
        
        # GI值调整
        if gi_value:
            if gi_value > 70:  # 高GI
                carb_impact_multiplier = 1.2
                peak_time = 60  # 高GI食物峰值在1小时
            elif gi_value > 55:  # 中GI
                carb_impact_multiplier = 1.0
                peak_time = 90  # 中GI食物峰值在1.5小时
            else:  # 低GI
                carb_impact_multiplier = 0.8
                peak_time = 120  # 低GI食物峰值在2小时
        else:
            carb_impact_multiplier = 1.0
            peak_time = 90
        
        # 活动水平调整
        activity_factors = {
            "sedentary": 1.0,
            "light": 0.95,
            "moderate": 0.90,
            "vigorous": 0.85
        }
        activity_factor = activity_factors.get(activity_level.lower(), 1.0)
        
        # 计算峰值血糖
        carb_impact = base_carb_impact * carb_impact_multiplier * activity_factor
        net_impact = carb_impact - insulin_effect
        peak_value = current_bg + net_impact
        
        # 确保峰值不为负
        peak_value = max(peak_value, 3.0)
        
        # 生成预测点
        predictions = []
        time_points = [30, 60, 90, 120, 180, 240]  # 餐后30分钟、1小时、1.5小时、2小时、3小时、4小时
        
        for time_minutes in time_points:
            if time_minutes <= peak_time:
                # 上升阶段：线性上升
                progress = time_minutes / peak_time
                bg_value = current_bg + net_impact * progress
            else:
                # 下降阶段：指数衰减
                decay_factor = 0.95 ** ((time_minutes - peak_time) / 30)
                bg_value = peak_value - (peak_value - current_bg) * (1 - decay_factor)
            
            # 确保血糖值在合理范围内
            bg_value = max(3.0, min(bg_value, 20.0))
            
            # 计算置信度（越接近峰值时间，置信度越高）
            confidence = 0.85 if abs(time_minutes - peak_time) <= 30 else 0.75
            
            predictions.append({
                "time_minutes": time_minutes,
                "bg_value": round(bg_value, 1),
                "confidence": confidence
            })
        
        # 风险评估
        risk_level = self._assess_risk(peak_value)
        
        # 生成建议
        recommendations = self._generate_recommendations(
            peak_value, risk_level, gi_value, activity_level
        )
        
        return {
            "predictions": predictions,
            "peak_time": peak_time,
            "peak_value": round(peak_value, 1),
            "risk_level": risk_level,
            "recommendations": recommendations
        }
    
    def _assess_risk(self, peak_value: float) -> str:
        """评估风险等级"""
        if peak_value > 13.9:
            return "high"
        elif peak_value > 10.0:
            return "medium"
        else:
            return "low"
    
    def _generate_recommendations(
        self,
        peak_value: float,
        risk_level: str,
        gi_value: Optional[float],
        activity_level: str
    ) -> List[str]:
        """生成优化建议"""
        recommendations = []
        
        if risk_level == "high":
            recommendations.append("预测血糖偏高，建议增加胰岛素剂量或减少碳水摄入")
        elif risk_level == "medium":
            recommendations.append("预测血糖略高，建议适当增加胰岛素或增加运动")
        
        if gi_value and gi_value > 70:
            recommendations.append("高GI食物，建议餐后适当运动以降低血糖峰值")
        
        if activity_level == "sedentary":
            recommendations.append("久坐状态，建议餐后轻度活动以改善血糖控制")
        
        if peak_value > 7.8:
            recommendations.append("建议监测餐后2小时血糖，如超过目标范围需调整")
        
        return recommendations

