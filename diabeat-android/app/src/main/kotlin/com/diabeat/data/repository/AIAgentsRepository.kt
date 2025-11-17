package com.diabeat.data.repository

import com.diabeat.data.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Agents数据仓库
 * 处理所有AI相关的数据操作
 */
@Singleton
class AIAgentsRepository @Inject constructor(
    private val aiAgentsService: AIAgentsService
) {
    
    /**
     * 获取健康建议
     */
    suspend fun getHealthAdvice(
        question: String,
        context: String? = null,
        role: String = "diabetes_advisor"
    ): Result<HealthAdviceResponse> = withContext(Dispatchers.IO) {
        try {
            val response = aiAgentsService.getHealthAdvice(
                HealthAdviceRequest(
                    question = question,
                    context = context,
                    role = role
                )
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get health advice: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 分析血糖预测
     */
    suspend fun analyzePrediction(
        peakValue: Float,
        currentBg: Float,
        activityLevel: String = "sedentary",
        giValue: Float? = null,
        totalCarbs: Float = 0f,
        insulinDose: Float = 0f
    ): Result<PredictionAnalysisResponse> = withContext(Dispatchers.IO) {
        try {
            val response = aiAgentsService.analyzePrediction(
                PredictionAnalysisRequest(
                    peak_value = peakValue,
                    current_bg = currentBg,
                    activity_level = activityLevel,
                    gi_value = giValue,
                    total_carbs = totalCarbs,
                    insulin_dose = insulinDose
                )
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to analyze prediction: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取健康分析
     */
    suspend fun getHealthAnalysis(
        userId: String,
        timeRange: String = "last_30_days",
        bgReadings: List<BGReading>
    ): Result<HealthAnalysisResponse> = withContext(Dispatchers.IO) {
        try {
            val response = aiAgentsService.getHealthAnalysis(
                HealthAnalysisRequest(
                    user_id = userId,
                    time_range = timeRange,
                    bg_readings = bgReadings
                )
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get health analysis: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取AI Agents状态
     */
    suspend fun getAgentsStatus(): Result<AgentsStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = aiAgentsService.getAgentsStatus()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get agents status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取AI Agents统计
     */
    suspend fun getAgentsStats(): Result<Map<String, AgentStats>> = withContext(Dispatchers.IO) {
        try {
            val response = aiAgentsService.getAgentsStats()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get agents stats: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
