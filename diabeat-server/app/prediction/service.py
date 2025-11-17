"""
血糖预测服务

TODO: 数据库表结构需要优化
当前 actual_blood_glucose 表结构：
  - id, user_id, bg_value, source, measured_at, created_at

需要添加的列用于支持纠正功能：
  - prediction_id (关联到预测记录)
  - prediction_time_minutes (预测时间点)
  - predicted_value (预测值)
  - difference (实测值与预测值的差异)
  - note (备注)

或创建单独的 blood_glucose_corrections 表
"""
from typing import Optional, List, Tuple
from uuid import uuid4
from datetime import datetime
from app.prediction.schemas import (
    BloodGlucosePredictionRequest,
    BloodGlucosePredictionResponse,
    BloodGlucosePrediction,
    BloodGlucoseCorrectionRequest,
    BloodGlucoseCorrectionResponse,
)
from app.prediction.predictor import BloodGlucosePredictor
from app.prediction.ai_predictor import AIBloodGlucosePredictor
from app.database import database
from app.utils.fastmcp_client import enhance_glucose_prediction, is_mcp_available
from app.config import settings
import logging
import json

logger = logging.getLogger(__name__)

class PredictionService:
    """血糖预测服务"""
    
    def __init__(self):
        # 使用AI预测器（自动包含fallback到规则引擎）
        self.predictor = AIBloodGlucosePredictor()
        logger.info(f"✅ 预测器初始化完成，AI模式: {self.predictor.is_enabled()}")
        
        # Initialize AI agents if enabled
        self._diabetes_analyst = None
        self._ml_predictor = None
        
        if settings.AI_ENABLED:
            try:
                from app.ai_agents.diabetes_analyst import DiabetesAnalystAgent
                from app.ai_agents.ml_predictor import MLGlucosePredictorAgent
                
                if settings.USE_DIABETES_ANALYST:
                    self._diabetes_analyst = DiabetesAnalystAgent()
                    logger.info("DiabetesAnalystAgent initialized")
                
                if settings.USE_ML_PREDICTION:
                    self._ml_predictor = MLGlucosePredictorAgent()
                    logger.info("MLGlucosePredictorAgent initialized")
            except Exception as e:
                logger.warning(f"Failed to initialize AI agents: {e}")
    
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
            # 获取用户历史偏差（AI预测器会使用这个信息）
            bias = await self._get_user_bias(user_id)
            
            # 获取纠正记录数（用于评估可信度）
            correction_count = await self._get_correction_count(user_id)
            
            # 执行AI增强预测（支持时间感知 + 个性化 + 历史记录）
            result = await self.predictor.predict(
                total_carbs=request.total_carbs,
                insulin_dose=request.insulin_dose,
                current_bg=request.current_bg,
                gi_value=request.gi_value,
                activity_level=request.activity_level or "sedentary",
                user_bias=bias,
                correction_count=correction_count,
                # 时间上下文
                meal_time=request.meal_time,
                medication_time=request.medication_time,
                current_time=request.current_time,
                # 用户基础信息（个性化）
                weight=request.weight,
                height=request.height,
                age=request.age,
                gender=request.gender,
                diabetes_type=request.diabetes_type,
                # 历史记录（AI上下文）
                recent_meals=request.recent_meals,
                recent_medications=request.recent_medications,
                recent_exercises=request.recent_exercises,
                recent_water=request.recent_water
            )
            
            # 检查是否需要提醒用餐/用药
            reminders = await self._check_reminders(request, result)
            if reminders:
                result['reminders'] = reminders  # 添加到结果中
            
            # AI预测器已经内部处理了偏差调整，这里不再需要手动调整
            # if abs(bias) >= 0.05:
            #     result = self._apply_bias_adjustment(result, bias, request)
            
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
            
            # 绑定预测ID，方便后续纠正
            response_data.prediction_id = prediction_id
            
            # AI增强：使用DiabetesAnalystAgent分析预测结果
            if self._diabetes_analyst:
                try:
                    logger.info("使用DiabetesAnalystAgent增强预测分析")
                    
                    analyst_result = await self._diabetes_analyst.execute({
                        "peak_value": result["peak_value"],
                        "current_bg": request.current_bg,
                        "activity_level": request.activity_level or "sedentary",
                        "gi_value": request.gi_value,
                        "total_carbs": request.total_carbs,
                        "insulin_dose": request.insulin_dose,
                        "predictions": result["predictions"]
                    })
                    
                    if analyst_result["success"]:
                        analysis = analyst_result["result"]
                        # 添加AI分析结果到响应
                        if hasattr(response_data, 'ai_analysis'):
                            response_data.ai_analysis = analysis
                        logger.info("AI分析增强完成")
                except Exception as e:
                    logger.warning(f"AI分析增强失败: {e}")
            
            # 如果启用MCP且MCP可用，尝试增强预测（保持向后兼容）
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

    async def submit_correction(
        self,
        request: BloodGlucoseCorrectionRequest,
        user_id: str
    ) -> BloodGlucoseCorrectionResponse:
        """提交实测血糖用于纠正"""
        # 如果有prediction_id，获取预测记录；否则使用默认值
        predicted_value = 0.0
        if request.prediction_id and request.prediction_id.strip():
            try:
                prediction = await self._get_prediction_record(request.prediction_id, user_id)
                prediction_data = prediction["prediction_data"]
                predicted_value = self._extract_predicted_value(
                    prediction_data,
                    request.prediction_time_minutes
                )
            except Exception as e:
                logger.warning(f"无法获取预测记录 {request.prediction_id}: {e}，使用默认值")
                predicted_value = 0.0
        
        measured_at = request.measured_at or datetime.utcnow()
        difference = round(request.actual_value - predicted_value, 2) if predicted_value > 0 else 0.0
        
        # 生成correction_id
        correction_id = str(uuid4())
        
        correction_id, created_at = await self._save_actual_measurement(
            user_id=user_id,
            correction_id=correction_id,
            actual_value=request.actual_value,
            measured_at=measured_at,
            source=request.source or "manual",
            prediction_id=request.prediction_id,
            prediction_time_minutes=request.prediction_time_minutes,
            predicted_value=predicted_value,
            difference=difference,
            note=request.note
        )
        
        return BloodGlucoseCorrectionResponse(
            id=correction_id,
            prediction_id=request.prediction_id,
            predicted_value=predicted_value,
            actual_value=request.actual_value,
            difference=difference,
            prediction_time_minutes=request.prediction_time_minutes,
            measured_at=measured_at,
            source=request.source or "manual",
            note=request.note,
            created_at=created_at
        )

    async def get_corrections(
        self,
        user_id: str,
        limit: int = 20
    ) -> List[BloodGlucoseCorrectionResponse]:
        """获取最近的纠正记录"""
        # TODO: 需要创建blood_glucose_corrections表或扩展actual_blood_glucose表
        # 当前表结构不支持纠正记录查询，暂时返回空列表
        return []
        
        # 原始代码（待表结构优化后启用）:
        # query = """
        #     SELECT id, prediction_id, predicted_value, bg_value AS actual_value,
        #            difference, prediction_time_minutes, measured_at, source, note, created_at
        #     FROM actual_blood_glucose
        #     WHERE user_id = :user_id AND prediction_id IS NOT NULL
        #     ORDER BY measured_at DESC
        #     LIMIT :limit
        # """
        # rows = await database.fetch_all(query, values={"user_id": user_id, "limit": limit})
        # corrections: List[BloodGlucoseCorrectionResponse] = []
        # for row in rows:
        #     row_dict = dict(row)
        #     corrections.append(BloodGlucoseCorrectionResponse(
        #         id=str(row_dict["id"]),
        #         prediction_id=str(row_dict["prediction_id"]),
        #         predicted_value=float(row_dict.get("predicted_value") or 0),
        #         actual_value=float(row_dict.get("actual_value") or 0),
        #         difference=float(row_dict.get("difference") or 0),
        #         prediction_time_minutes=row_dict.get("prediction_time_minutes"),
        #         measured_at=row_dict["measured_at"],
        #         source=row_dict.get("source") or "manual",
        #         note=row_dict.get("note"),
        #         created_at=row_dict["created_at"]
        #     ))
        # return corrections

    async def _get_prediction_record(self, prediction_id: str, user_id: str) -> dict:
        query = """
            SELECT id, user_id, prediction_data, created_at
            FROM bg_predictions
            WHERE id = :id AND user_id = :user_id
        """
        record = await database.fetch_one(query, values={"id": prediction_id, "user_id": user_id})
        if not record:
            raise ValueError("未找到对应的预测记录")
        
        record_dict = dict(record)
        prediction_data = record_dict.get("prediction_data")
        if isinstance(prediction_data, str):
            prediction_data = json.loads(prediction_data)
        record_dict["prediction_data"] = prediction_data or {}
        return record_dict

    def _extract_predicted_value(self, prediction_data: dict, time_minutes: Optional[int]) -> float:
        predictions = prediction_data.get("predictions") or []
        if time_minutes is not None:
            for item in predictions:
                if item.get("time_minutes") == time_minutes:
                    return float(item.get("bg_value", prediction_data.get("peak_value", 0)))
        return float(prediction_data.get("peak_value") or 0)

    async def _save_actual_measurement(
        self,
        user_id: str,
        correction_id: str,
        actual_value: float,
        measured_at: datetime,
        source: str = "manual",
        prediction_id: Optional[str] = None,
        prediction_time_minutes: Optional[int] = None,
        predicted_value: Optional[float] = None,
        difference: Optional[float] = None,
        note: Optional[str] = None
    ) -> Tuple[str, datetime]:
        """保存实测血糖"""
        query = """
            INSERT INTO actual_blood_glucose
            (id, user_id, bg_value, source, measured_at, created_at)
            VALUES
            (:id, :user_id, :bg_value, :source, :measured_at, :created_at)
            RETURNING id, created_at
        """
        created_at = datetime.utcnow()
        values = {
            "id": correction_id,
            "user_id": user_id,
            "bg_value": actual_value,
            "source": source,
            "measured_at": measured_at,
            "created_at": created_at
        }
        result = await database.fetch_one(query=query, values=values)
        
        # TODO: 将预测相关数据保存到单独的corrections表
        # prediction_id, prediction_time_minutes, predicted_value, difference, note
        
        return str(result["id"]), result["created_at"]

    async def _get_user_bias(self, user_id: str) -> float:
        """计算用户最近纠正的平均偏差"""
        # TODO: 需要添加difference列到actual_blood_glucose表
        # 暂时返回0，表示没有偏差调整
        return 0.0
        
        # 原始代码（待表结构更新后启用）:
        # query = """
        #     SELECT difference
        #     FROM actual_blood_glucose
        #     WHERE user_id = :user_id AND difference IS NOT NULL
        #     ORDER BY measured_at DESC
        #     LIMIT 10
        # """
        # rows = await database.fetch_all(query=query, values={"user_id": user_id})
        # differences = [float(row["difference"]) for row in rows if row and row["difference"] is not None]
        # if not differences:
        #     return 0.0
        # return sum(differences) / len(differences)
    
    async def _get_correction_count(self, user_id: str) -> int:
        """获取用户的纠正记录数"""
        # TODO: 待表结构优化后从corrections表获取
        # 暂时返回0
        return 0
        
        # 待启用的代码:
        # query = """
        #     SELECT COUNT(*) as count
        #     FROM actual_blood_glucose
        #     WHERE user_id = :user_id
        # """
        # result = await database.fetch_one(query=query, values={"user_id": user_id})
        # return result["count"] if result else 0

    def _apply_bias_adjustment(
        self,
        result: dict,
        bias: float,
        request: BloodGlucosePredictionRequest
    ) -> dict:
        """根据历史偏差调整预测结果"""
        adjusted_predictions = []
        for pred in result.get("predictions", []):
            adjusted_value = max(3.0, min(20.0, float(pred.get("bg_value", 0)) + bias))
            adjusted_predictions.append({
                **pred,
                "bg_value": round(adjusted_value, 1)
            })
        
        if adjusted_predictions:
            result["predictions"] = adjusted_predictions
            peak_value = max(p["bg_value"] for p in adjusted_predictions)
            result["peak_value"] = round(peak_value, 1)
        else:
            result["peak_value"] = round(max(3.0, result.get("peak_value", 0) + bias), 1)
        
        return result
    
    async def _check_reminders(
        self,
        request: BloodGlucosePredictionRequest,
        prediction_result: dict
    ) -> List[dict]:
        """
        检查是否需要提醒用餐/用药
        
        Returns:
            List of reminder dictionaries
        """
        from .time_aware_predictor import time_aware_predictor
        
        reminders = []
        
        # 只有在有时间上下文时才检查提醒
        if not request.meal_time or not request.current_time:
            return reminders
        
        try:
            # 计算时间上下文
            time_context = time_aware_predictor.calculate_time_context(
                request.meal_time,
                request.medication_time,
                request.current_time
            )
            
            if not time_context['has_time_context']:
                return reminders
            
            # 获取胰岛素模型
            insulin_model = time_aware_predictor.model_insulin_effect(
                request.insulin_dose,
                time_context['minutes_since_medication']
            )
            
            # 检查用餐提醒
            meal_reminder = time_aware_predictor.check_meal_reminder(
                time_context['minutes_since_meal'],
                request.current_bg,
                insulin_model['remaining_duration']
            )
            if meal_reminder:
                reminders.append(meal_reminder)
            
            # 检查用药提醒
            medication_reminder = time_aware_predictor.check_medication_reminder(
                time_context['minutes_since_meal'],
                time_context['minutes_since_medication'],
                request.total_carbs
            )
            if medication_reminder:
                reminders.append(medication_reminder)
            
            if reminders:
                logger.info(f"⏰ 生成{len(reminders)}个提醒")
            
        except Exception as e:
            logger.error(f"检查提醒失败: {e}")
        
        return reminders
