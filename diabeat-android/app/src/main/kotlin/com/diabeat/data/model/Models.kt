package com.diabeat.data.model

import kotlinx.serialization.Serializable
import java.util.Date

// 用户相关
@Serializable
data class DeviceAuthRequest(
    val device_id: String,
    val name: String? = null,
    val diabetes_type: String? = null
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null,
    val phone: String? = null,
    val diabetes_type: String
)

@Serializable
data class RegisterResponse(
    val id: String,
    val email: String,
    val name: String? = null,
    val diabetes_type: String,
    val created_at: String
)

@Serializable
data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val device_id: String, // 新增 device_id 字段
    val email: String? = null, // email 现在是可选的
    val name: String? = null,
    val diabetes_type: String,
    val created_at: String
)

@Serializable
data class UserParametersRequest(
    val insulin_type: String? = null,
    val isf: Float? = null,
    val icr: Float? = null,
    val target_bg_low: Float = 4.0f,
    val target_bg_high: Float = 7.8f,
    val max_insulin_dose: Float? = null,
    val min_insulin_dose: Float = 0.5f
)

@Serializable
data class UserParametersResponse(
    val id: String,
    val user_id: String,
    val insulin_type: String? = null,
    val isf: Float? = null,
    val icr: Float? = null,
    val target_bg_low: Float,
    val target_bg_high: Float,
    val max_insulin_dose: Float? = null,
    val min_insulin_dose: Float,
    val created_at: String,
    val updated_at: String
)

// 食物识别
@Serializable
data class FoodRecommendation(
    val recommended_weight: Float,      // 建议食用量（克）
    val recommended_carbs: Float,       // 建议碳水摄入量（克）
    val reason: String,                 // 建议原因
    val adjustment_factor: Float,       // 调整系数（建议量/当前量）
    val adjustment_percent: Float,     // 调整百分比（%）
    val warning: String? = null,        // 警告信息
    val current_weight: Float,          // 当前识别重量（克）
    val gi_value: Float? = null,        // 升糖指数
    val gl_value: Float? = null,        // 血糖负荷
    val can_eat_all: Boolean? = null    // 是否可以全部食用
)

@Serializable
data class FoodItem(
    val id: String? = null,
    val name: String,
    val calories: Float? = null,
    val weight: Float? = null,
    val confidence: Float? = null,
    val cooking_method: String? = null,
    // 营养成分（对糖尿病人重要）
    val carbs: Float? = null,           // 碳水化合物（克）
    val net_carbs: Float? = null,       // 净碳水化合物（克，扣除纤维）
    val protein: Float? = null,         // 蛋白质（克）
    val fat: Float? = null,             // 脂肪（克）
    val fiber: Float? = null,           // 膳食纤维（克）
    val gi_value: Float? = null,        // 升糖指数（GI）
    val gl_value: Float? = null,        // 血糖负荷（GL）
    // 建议信息
    val recommendation: FoodRecommendation? = null  // 建议食用量信息
)

@Serializable
data class FoodRecognitionResponse(
    val recognition_id: String,
    val foods: List<FoodItem>,
    val total_confidence: Float,
    val image_url: String
)

// 营养计算
@Serializable
data class FoodItemInput(
    val name: String,
    val weight: Float,
    val cooking_method: String? = null
)

@Serializable
data class NutritionCalculationRequest(
    val foods: List<FoodItemInput>
)

@Serializable
data class NutritionCalculationDetail(
    val name: String,
    val weight: Float,
    val carbs: Float,
    val net_carbs: Float,
    val protein: Float,
    val fat: Float,
    val fiber: Float,
    val calories: Float,
    val gi_value: Float? = null
)

@Serializable
data class NutritionCalculationResponse(
    val total_carbs: Float,
    val net_carbs: Float,
    val protein: Float,
    val fat: Float,
    val fiber: Float,
    val calories: Float,
    val gi_value: Float? = null,
    val gl_value: Float? = null,
    val calculation_details: List<NutritionCalculationDetail>? = null,
    val nutrition_record_id: String? = null
)

// 胰岛素计算
@Serializable
data class InsulinCalculationRequest(
    val total_carbs: Float,
    val current_bg: Float,
    val activity_level: String = "sedentary",
    val meal_time: String? = null
)

@Serializable
data class InsulinCalculationResponse(
    val recommended_dose: Float,
    val carb_insulin: Float,
    val correction_insulin: Float,
    val activity_adjustment: Float,
    val injection_timing: String,
    val split_dose: Boolean,
    val risk_level: String,
    val warnings: List<String> = emptyList()
)

// 血糖预测
@Serializable
data class BloodGlucosePrediction(
    val time_minutes: Int,
    val bg_value: Float,
    val confidence: Float
)

@Serializable
data class BloodGlucosePredictionRequest(
    val total_carbs: Float,
    val insulin_dose: Float,
    val current_bg: Float,
    val gi_value: Float? = null,
    val activity_level: String = "sedentary"
)

@Serializable
data class BloodGlucosePredictionResponse(
    val predictions: List<BloodGlucosePrediction>,
    val peak_time: Int,
    val peak_value: Float,
    val risk_level: String,
    val recommendations: List<String> = emptyList()
)

// 记录管理
@Serializable
data class MealRecordRequest(
    val meal_time: String,
    val food_recognition_id: String? = null,
    val nutrition_record_id: String? = null,
    val notes: String? = null,
    val food_items: List<FoodItemInput>? = null, // 新增：手动添加食物
    val meal_type: String? = null // 新增：餐次类型 (breakfast/lunch/dinner/snack)
)

@Serializable
data class MealRecordResponse(
    val id: String,
    val user_id: String,
    val meal_time: String,
    val food_recognition_id: String? = null,
    val nutrition_record_id: String? = null,
    val notes: String? = null,
    val created_at: String,
    val food_items: List<FoodItemInput>? = null // 新增：手动添加食物
)

@Serializable
data class InsulinRecordRequest(
    val injection_time: String,
    val actual_dose: Float,
    val insulin_record_id: String? = null,
    val notes: String? = null
)

@Serializable
data class InsulinRecordResponse(
    val id: String,
    val user_id: String,
    val injection_time: String,
    val insulin_record_id: String? = null,
    val actual_dose: Float,
    val notes: String? = null,
    val created_at: String
)

@Serializable
data class NextInsulinPredictionResponse(
    val predicted_time: String,
    val predicted_dose: Float? = null,
    val confidence: Float,
    val reasoning: String,
    val notification_scheduled: Boolean
)

// 营养统计
@Serializable
data class DailyNutritionRecommendation(
    val daily_calories: Float,
    val daily_carbs: Float,
    val daily_protein: Float,
    val daily_fat: Float,
    val daily_fiber: Float,
    val carbs_per_meal: Float,
    val bmr: Float,
    val activity_level: String,
    val diabetes_type: String
)

@Serializable
data class TodayNutritionIntake(
    val total_calories: Float,
    val total_carbs: Float,
    val total_net_carbs: Float,
    val total_protein: Float,
    val total_fat: Float,
    val total_fiber: Float,
    val meal_count: Int,
    val date: String
)

// 引导页完成响应
@Serializable
data class OnboardingResponse(
    val message: String,
    val user_info: Map<String, String>? = null,
    val daily_recommendation: DailyNutritionRecommendation? = null
)

// ==================== 运动记录相关 ====================

@Serializable
data class ExerciseRecordRequest(
    val exercise_time: String, // ISO格式时间
    val exercise_type: String, // walking/running/cycling/swimming/gym/yoga/dancing/other
    val duration_minutes: Int,
    val intensity: String = "moderate", // light/moderate/vigorous
    val calories_burned: Float? = null, // 可选，系统会自动估算
    val notes: String? = null
)

@Serializable
data class ExerciseRecordResponse(
    val id: String,
    val user_id: String,
    val exercise_time: String,
    val exercise_type: String,
    val duration_minutes: Int,
    val intensity: String,
    val calories_burned: Float,
    val notes: String? = null,
    val created_at: String
)

@Serializable
data class TodayExerciseSummary(
    val total_calories: Float,
    val total_duration: Int,
    val exercise_count: Int,
    val exercises: List<ExerciseRecordResponse> = emptyList()
)

// ==================== 水分记录相关 ====================

@Serializable
data class WaterRecordRequest(
    val record_time: String, // ISO格式时间
    val amount_ml: Int, // 摄入量（毫升）
    val water_type: String = "water", // water/tea/coffee/juice/other
    val notes: String? = null
)

@Serializable
data class WaterRecordResponse(
    val id: String,
    val user_id: String,
    val record_time: String,
    val amount_ml: Int,
    val water_type: String,
    val notes: String? = null,
    val created_at: String
)

@Serializable
data class TodayWaterSummary(
    val total_ml: Int,
    val record_count: Int,
    val records: List<WaterRecordResponse> = emptyList(),
    val progress_percentage: Float
)

// ==================== 用药记录相关 ====================

@Serializable
data class MedicationRecordRequest(
    val medication_time: String, // ISO格式时间
    val medication_type: String, // insulin/oral_medication/other
    val medication_name: String, // 药物名称
    val dosage: Float, // 剂量
    val dosage_unit: String, // 单位：units(胰岛素单位)/mg/ml/tablets(片)
    val notes: String? = null
)

@Serializable
data class MedicationRecordResponse(
    val id: String,
    val user_id: String,
    val medication_time: String,
    val medication_type: String,
    val medication_name: String,
    val dosage: Float,
    val dosage_unit: String,
    val notes: String? = null,
    val created_at: String
)

@Serializable
data class TodayMedicationSummary(
    val total_count: Int,
    val insulin_count: Int,
    val oral_medication_count: Int,
    val medications: List<MedicationRecordResponse> = emptyList()
)

// ==================== 智能提醒相关 ====================

@Serializable
data class SmartReminderResponse(
    val next_meal_time: String?, // 下次用餐时间预测
    val next_medication_time: String?, // 下次用药时间预测
    val meal_reminder_message: String?,
    val medication_reminder_message: String?,
    val should_eat_soon: Boolean, // 是否应该尽快进食
    val should_take_medication_soon: Boolean, // 是否应该尽快用药
    val reasoning: String // 提醒依据
)

