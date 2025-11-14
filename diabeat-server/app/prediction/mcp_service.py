from typing import Optional, List, Dict, Any
from app.prediction.schemas import BloodGlucosePredictionRequest, BloodGlucosePredictionResponse, BloodGlucosePrediction
from app.utils import fastmcp_client
import logging

logger = logging.getLogger(__name__)

class MCPPredictionService:
    """使用FastMCP增强的血糖预测服务"""
    
    def __init__(self):
        self.mcp_client = fastmcp_client
    
    async def predict_with_agent(self,
                               request: BloodGlucosePredictionRequest,
                               user_id: str,
                               agent_name: str = "glucose_prediction_agent") -> BloodGlucosePredictionResponse:
        """使用MCP代理进行血糖预测
        
        Args:
            request: 预测请求
            user_id: 用户ID
            agent_name: MCP代理名称
            
        Returns:
            BloodGlucosePredictionResponse: 预测结果
        """
        try:
            # 构建查询参数
            query_params = {
                "user_id": user_id,
                "total_carbs": request.total_carbs,
                "insulin_dose": request.insulin_dose,
                "current_bg": request.current_bg,
                "gi_value": request.gi_value,
                "activity_level": request.activity_level or "sedentary"
            }
            
            # 调用MCP代理
            logger.info(f"调用MCP代理 {agent_name} 进行血糖预测")
            result = await self.mcp_client.query_agent(agent_name, query_params)
            
            # 解析结果
            predictions = self._parse_predictions(result.get("predictions", []))
            risk_assessment = result.get("risk_assessment", {})
            recommendations = result.get("recommendations", [])
            
            return BloodGlucosePredictionResponse(
                predictions=predictions,
                risk_assessment=risk_assessment,
                recommendations=recommendations,
                confidence_score=result.get("confidence_score", 0.85),
                note=f"预测由MCP代理 {agent_name} 生成"
            )
            
        except Exception as e:
            logger.error(f"MCP代理预测失败: {str(e)}")
            # 如果MCP调用失败，提供一个基本的回退响应
            return self._get_fallback_response(request)
    
    def _parse_predictions(self, prediction_data: List[Dict[str, Any]]) -> List[BloodGlucosePrediction]:
        """解析预测数据"""
        predictions = []
        for pred in prediction_data:
            predictions.append(BloodGlucosePrediction(
                time_minutes=pred.get("time_minutes", 0),
                bg_value=pred.get("bg_value", 0),
                confidence=pred.get("confidence", 0.8)
            ))
        return predictions
    
    def _get_fallback_response(self, request: BloodGlucosePredictionRequest) -> BloodGlucosePredictionResponse:
        """当MCP调用失败时的回退响应"""
        # 提供基本的预测数据作为回退
        fallback_predictions = [
            BloodGlucosePrediction(time_minutes=30, bg_value=request.current_bg + 0.5, confidence=0.7),
            BloodGlucosePrediction(time_minutes=60, bg_value=request.current_bg + 1.0, confidence=0.7),
            BloodGlucosePrediction(time_minutes=120, bg_value=request.current_bg + 0.5, confidence=0.7),
            BloodGlucosePrediction(time_minutes=180, bg_value=request.current_bg, confidence=0.7)
        ]
        
        return BloodGlucosePredictionResponse(
            predictions=fallback_predictions,
            risk_assessment={"level": "unknown", "message": "使用回退预测模型"},
            recommendations=["请检查MCP服务连接"],
            confidence_score=0.7,
            note="使用本地回退模型生成预测"
        )
    
    async def get_agent_insights(self, user_id: str, query: str, agent_name: str = "health_analyst_agent") -> Dict[str, Any]:
        """获取健康分析师代理的见解
        
        Args:
            user_id: 用户ID
            query: 查询问题
            agent_name: MCP代理名称
            
        Returns:
            Dict: 代理提供的见解
        """
        try:
            query_params = {
                "user_id": user_id,
                "query": query
            }
            
            result = await self.mcp_client.query_agent(agent_name, query_params)
            return result
        except Exception as e:
            logger.error(f"获取代理见解失败: {str(e)}")
            return {"error": "无法连接到健康分析代理"}

# 创建服务实例
mcp_prediction_service = MCPPredictionService()
