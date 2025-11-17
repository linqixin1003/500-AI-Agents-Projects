package com.diabeat.data.api

import retrofit2.Response
import retrofit2.http.*

/**
 * AI Agents API服务
 * 新增的AI功能接口，不影响现有功能
 */
interface AIAgentsService {
    
    /**
     * 获取健康建议
     */
    @POST("api/v1/ai/health-advice")
    suspend fun getHealthAdvice(
        @Body request: HealthAdviceRequest
    ): Response<HealthAdviceResponse>
    
    /**
     * 分析血糖预测
     */
    @POST("api/v1/ai/analyze-prediction")
    suspend fun analyzePrediction(
        @Body request: PredictionAnalysisRequest
    ): Response<PredictionAnalysisResponse>
    
    /**
     * 获取健康分析
     */
    @POST("api/v1/ai/health-analysis")
    suspend fun getHealthAnalysis(
        @Body request: HealthAnalysisRequest
    ): Response<HealthAnalysisResponse>
    
    /**
     * 获取AI Agents状态
     */
    @GET("api/v1/ai/agents/status")
    suspend fun getAgentsStatus(): Response<AgentsStatusResponse>
    
    /**
     * 获取AI Agents统计
     */
    @GET("api/v1/ai/agents/stats")
    suspend fun getAgentsStats(): Response<Map<String, AgentStats>>
}

// 请求模型
data class HealthAdviceRequest(
    val question: String,
    val context: String? = null,
    val role: String = "diabetes_advisor" // diabetes_advisor, nutrition_expert, insulin_specialist
)

data class PredictionAnalysisRequest(
    val peak_value: Float,
    val current_bg: Float,
    val activity_level: String = "sedentary",
    val gi_value: Float? = null,
    val total_carbs: Float = 0f,
    val insulin_dose: Float = 0f
)

data class HealthAnalysisRequest(
    val user_id: String,
    val time_range: String = "last_30_days",
    val bg_readings: List<BGReading>
)

data class BGReading(
    val value: Float,
    val timestamp: String
)

// 响应模型
data class HealthAdviceResponse(
    val answer: String,
    val confidence: Float,
    val sources: List<String>,
    val recommendations: List<String>,
    val disclaimer: String
)

data class PredictionAnalysisResponse(
    val confidence_score: Float,
    val risk_assessment: RiskAssessment,
    val trend_analysis: Map<String, String>
)

data class RiskAssessment(
    val severity: String, // low, medium, high
    val risk_score: Float,
    val probability: Float,
    val factors: List<String>,
    val recommendations: List<String>
)

data class HealthAnalysisResponse(
    val summary: HealthSummary,
    val trends: List<Trend>,
    val anomalies: List<Anomaly>,
    val risk_assessment: HealthRiskAssessment,
    val recommendations: List<String>
)

data class HealthSummary(
    val bg_control_status: String,
    val avg_bg: Float,
    val min_bg: Float,
    val max_bg: Float,
    val time_in_range: Float,
    val time_below_range: Float,
    val time_above_range: Float,
    val hypoglycemia_episodes: Int,
    val hyperglycemia_episodes: Int,
    val glucose_variability: Float,
    val total_readings: Int
)

data class Trend(
    val metric: String,
    val trend: String,
    val change: String? = null,
    val description: String
)

data class Anomaly(
    val type: String,
    val count: Int? = null,
    val value: Float? = null,
    val severity: String,
    val description: String
)

data class HealthRiskAssessment(
    val overall_risk: String,
    val complications: Map<String, String>,
    val factors: List<String>
)

data class AgentsStatusResponse(
    val ai_enabled: Boolean,
    val agents: Map<String, AgentInfo>,
    val configuration: Map<String, Any>
)

data class AgentInfo(
    val enabled: Boolean,
    val status: String
)

data class AgentStats(
    val agent: String,
    val version: String,
    val created_at: String,
    val execution_count: Int,
    val success_count: Int,
    val error_count: Int,
    val success_rate: Float
)
