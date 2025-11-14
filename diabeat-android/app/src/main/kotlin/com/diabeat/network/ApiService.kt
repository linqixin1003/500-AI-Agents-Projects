package com.diabeat.network

import com.diabeat.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // 用户相关
    @POST("api/users/device-auth")
    suspend fun deviceAuth(@Body request: DeviceAuthRequest): Response<LoginResponse>

    @POST("api/users/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>
    
    @POST("api/users/login")
    @FormUrlEncoded
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<LoginResponse>
    
    @GET("api/users/me")
    suspend fun getCurrentUser(): Response<UserResponse>
    
    @GET("api/users/{user_id}/parameters")
    suspend fun getUserParameters(@Path("user_id") userId: String): Response<UserParametersResponse>
    
    @POST("api/users/{user_id}/parameters")
    suspend fun createUserParameters(
        @Path("user_id") userId: String,
        @Body request: UserParametersRequest
    ): Response<UserParametersResponse>
    
    // 食物识别
    @Multipart
    @POST("api/food/recognize")
    suspend fun recognizeFood(
        @Part image: MultipartBody.Part
    ): Response<FoodRecognitionResponse>
    
    // 营养计算
    @POST("api/nutrition/calculate")
    suspend fun calculateNutrition(
        @Body request: NutritionCalculationRequest,
        @Query("food_recognition_id") foodRecognitionId: String? = null
    ): Response<NutritionCalculationResponse>
    
    // 胰岛素计算
    @POST("api/insulin/calculate")
    suspend fun calculateInsulin(
        @Body request: InsulinCalculationRequest,
        @Query("nutrition_record_id") nutritionRecordId: String? = null,
        @Query("gi_value") giValue: Float? = null
    ): Response<InsulinCalculationResponse>
    
    // 血糖预测
    @POST("api/prediction/blood-glucose")
    suspend fun predictBloodGlucose(
        @Body request: BloodGlucosePredictionRequest,
        @Query("insulin_record_id") insulinRecordId: String? = null,
        @Query("nutrition_record_id") nutritionRecordId: String? = null
    ): Response<BloodGlucosePredictionResponse>
    
    // 记录管理
    @POST("api/records/meals")
    suspend fun createMealRecord(@Body request: MealRecordRequest): Response<MealRecordResponse>
    
    @GET("api/records/meals")
    suspend fun getMealRecords(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("date") date: String? = null
    ): Response<List<MealRecordResponse>>
    
    @GET("api/records/insulin")
    suspend fun getInsulinRecords(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("date") date: String? = null
    ): Response<List<InsulinRecordResponse>>

    @POST("api/records/insulin")
    suspend fun createInsulinRecord(@Body request: InsulinRecordRequest): Response<InsulinRecordResponse>
    
    @GET("api/records/predict-next-insulin")
    suspend fun predictNextInsulin(): Response<NextInsulinPredictionResponse>
    
    // 营养统计
    @GET("api/nutrition/daily-recommendation")
    suspend fun getDailyRecommendation(): Response<DailyNutritionRecommendation>
    
    @GET("api/nutrition/today-intake")
    suspend fun getTodayIntake(): Response<TodayNutritionIntake>
    
    /**
     * 完成引导页，保存用户信息
     */
    @POST("/api/users/complete-onboarding")
    suspend fun completeOnboarding(
        @Query("height") height: Float,
        @Query("weight") weight: Float,
        @Query("age") age: Int,
        @Query("gender") gender: String,
        @Query("diabetes_type") diabetesType: String
    ): Response<OnboardingResponse>
    
    // ==================== 运动记录相关 ====================
    
    @POST("api/records/exercises")
    suspend fun createExerciseRecord(@Body request: ExerciseRecordRequest): Response<ExerciseRecordResponse>
    
    @GET("api/records/exercises/today")
    suspend fun getTodayExerciseSummary(): Response<TodayExerciseSummary>
    
    @GET("api/records/exercises")
    suspend fun getExerciseRecords(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<ExerciseRecordResponse>>
    
    // ==================== 水分记录相关 ====================
    
    @POST("api/records/water")
    suspend fun createWaterRecord(@Body request: WaterRecordRequest): Response<WaterRecordResponse>
    
    @GET("api/records/water/today")
    suspend fun getTodayWaterSummary(): Response<TodayWaterSummary>
    
    @GET("api/records/water")
    suspend fun getWaterRecords(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<WaterRecordResponse>>
    
    // ==================== 用药记录相关 ====================
    
    @POST("api/records/medications")
    suspend fun createMedicationRecord(@Body request: MedicationRecordRequest): Response<MedicationRecordResponse>
    
    @GET("api/records/medications/today")
    suspend fun getTodayMedicationSummary(): Response<TodayMedicationSummary>
    
    @GET("api/records/medications")
    suspend fun getMedicationRecords(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<MedicationRecordResponse>>
    
    // ==================== 智能提醒相关 ====================
    
    @GET("api/reminders/smart")
    suspend fun getSmartReminders(): Response<SmartReminderResponse>
}
