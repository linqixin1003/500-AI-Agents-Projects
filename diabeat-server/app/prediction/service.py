from typing import Optional
from uuid import uuid4
from datetime import datetime
from app.prediction.schemas import BloodGlucosePredictionRequest, BloodGlucosePredictionResponse, BloodGlucosePrediction
from app.prediction.predictor import BloodGlucosePredictor
from app.database import database
from app.utils.fastmcp_client import enhance_glucose_prediction, is_mcp_available
import logging
import json

logger = logging.getLogger(__name__)

class PredictionService:
    """血糖预测服务"""
    
    def __init__(self):
        self.predictor = BloodGlucosePredictor()
    
    async def predict_blood_glucose(
        self,
        request: BloodGlucosePredictionRequest,
        user_id: str,
        insulin_record_id: Optional[str] = None,
        nutrition_record_id: Optional[str] = None,
        use_mcp: bool = True
    ) -> BloodGlucosePredictionResponse:
        """预测餐后血糖变化，支持MCP增强
        
        Args:
            request: 预测请求
            user_id: 用户ID
            insulin_record_id: 胰岛素记录ID（可选）
            nutrition_record_id: 营养成分记录ID（可选）
            use_mcp: 是否使用MCP增强预测结果
            
        Returns:
            BloodGlucosePredictionResponse: 预测结果
        """
        try:
            # 执行预测
            result = self.predictor.predict(
                total_carbs=request.total_carbs,
                insulin_dose=request.insulin_dose,
                current_bg=request.current_bg,
                gi_value=request.gi_value,
                activity_level=request.activity_level or "sedentary"
            )
            
            # 保存预测记录到数据库
            prediction_id = await self._save_prediction_record(
                user_id=user_id,
                insulin_record_id=insulin_record_id,
                nutrition_record_id=nutrition_record_id,
                request=request,
                result=result
            )
            
            # 转换为响应模型
            predictions = [
                BloodGlucosePrediction(**pred) for pred in result["predictions"]
            ]
            
            # 创建基础响应
            response_data = BloodGlucosePredictionResponse(
                predictions=predictions,
                peak_time=result["peak_time"],
                peak_value=result["peak_value"],
                risk_level=result["risk_level"],
                recommendations=result["recommendations"]
            )
            
            # 如果启用MCP且MCP可用，尝试增强预测
            if use_mcp and is_mcp_available():
                try:
                    logger.info("尝试使用MCP增强血糖预测")
                    
                    # 准备发送给MCP的数据
                    prediction_data = {
                        "current_blood_glucose": request.current_bg,
                        "peak_value": result["peak_value"],
                        "peak_time": result["peak_time"],
                        "risk_level": result["risk_level"],
                        "total_carbs": request.total_carbs,
                        "insulin_dose": request.insulin_dose,
                        "gi_value": request.gi_value,
                        "activity_level": request.activity_level or "sedentary",
                        "predictions": result["predictions"]
                    }
                    
                    # 调用MCP获取增强结果
                    enhanced_result = await enhance_glucose_prediction(prediction_data)
                    
                    # 更新响应数据
                    if hasattr(response_data, 'confidence_score'):
                        response_data.confidence_score = enhanced_result.get("confidence_score", 0.85)
                    if hasattr(response_data, 'risk_assessment'):
                        response_data.risk_assessment = enhanced_result.get("risk_assessment", {})
                    if hasattr(response_data, 'note'):
                        response_data.note = enhanced_result.get("note", None)
                    
                    # 如果MCP提供了额外建议，合并它们
                    mcp_recommendations = enhanced_result.get("recommendations", [])
                    if mcp_recommendations:
                        response_data.recommendations.extend(mcp_recommendations)
                        # 去重
                        response_data.recommendations = list(set(response_data.recommendations))
                        
                    logger.info("MCP增强预测成功")
                    
                except Exception as e:
                    logger.warning(f"MCP增强预测失败: {str(e)}，使用基础预测结果")
                    # 保留基础预测结果，不抛出异常
            
            return response_data
            
        except Exception as e:
            logger.error(f"Blood glucose prediction error: {str(e)}")
            raise
    
    async def _save_prediction_record(
        self,
        user_id: str,
        insulin_record_id: Optional[str],
        nutrition_record_id: Optional[str],
        request: BloodGlucosePredictionRequest,
        result: dict
    ) -> str:
        """保存预测记录到数据库"""
        prediction_id = str(uuid4())
        
        # 构建预测数据 JSON
        prediction_data = {
            "predictions": result["predictions"],
            "peak_time": result["peak_time"],
            "peak_value": result["peak_value"],
            "risk_level": result["risk_level"]
        }
        
        query = """
            INSERT INTO bg_predictions 
            (id, user_id, insulin_record_id, nutrition_record_id, prediction_data, created_at)
            VALUES 
            (:id, :user_id, :insulin_record_id, :nutrition_record_id, CAST(:prediction_data AS JSONB), :created_at)
            RETURNING id
        """
        
        values = {
            "id": prediction_id,
            "user_id": user_id,
            "insulin_record_id": insulin_record_id,
            "nutrition_record_id": nutrition_record_id,
            "prediction_data": json.dumps(prediction_data),
            "created_at": datetime.utcnow()
        }
        
        db_result = await database.fetch_one(query=query, values=values)
        return str(db_result["id"])

