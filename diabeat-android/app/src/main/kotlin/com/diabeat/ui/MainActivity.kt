package com.diabeat.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // 导入 LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diabeat.data.model.FoodRecognitionResponse
import com.diabeat.ui.base.BaseActivity
import com.diabeat.ui.camera.CameraScreen
import com.diabeat.ui.home.HomeScreen
import com.diabeat.ui.recognition.FoodRecognitionScreen
import com.diabeat.ui.theme.DiabEatTheme
import com.diabeat.viewmodel.CameraViewModel
import com.diabeat.viewmodel.FoodRecognitionViewModel
import com.diabeat.ui.foodsearch.FoodSearchScreen
import com.diabeat.data.model.FoodItem
import com.diabeat.viewmodel.HomeViewModel // 导入 HomeViewModel
import android.app.Application // 导入 Application
import com.diabeat.ui.onboarding.OnboardingScreen
import com.diabeat.ui.onboarding.UserOnboardingData
import android.content.Context

/**
 * 主Activity
 * 继承自BaseActivity以获得多语言支持
 * 出海应用主入口点
 */
class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 TokenManager / Retrofit / 设备信息
        com.diabeat.utils.TokenManager.init(this)
        com.diabeat.network.RetrofitClient.init(this)
        com.diabeat.utils.DeviceIdUtil.init(this)
        
        setContent {
            DiabEatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current.applicationContext // 获取 ApplicationContext

    // 检查是否首次启动
    val initialScreen = if (isFirstLaunch(context)) Screen.Onboarding else Screen.Main
    var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var recognitionResult by remember { mutableStateOf<FoodRecognitionResponse?>(null) }
    var selectedMealType by remember { mutableStateOf<String?>(null) } // 记录选择的餐次类型
    
    val cameraViewModel: CameraViewModel = viewModel()
    val recognitionViewModel: FoodRecognitionViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(context as Application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    })
    
    when (currentScreen) {
        Screen.Onboarding -> {
            OnboardingScreen(
                onComplete = { onboardingData ->
                    // 保存用户信息到本地
                    saveUserOnboardingData(context, onboardingData)
                    
                    // 标记完成并进入主页
                    markOnboardingCompleted(context)
                    currentScreen = Screen.Main
                },
                onSkip = {
                    // 这个回调现在不会被调用，因为跳过改为跳过当前步骤
                    markOnboardingCompleted(context)
                    currentScreen = Screen.Main
                }
            )
        }
        
        Screen.Main -> {
            com.diabeat.ui.main.MainScreen(
                homeViewModel = homeViewModel,
                onNavigateToCamera = { mealType ->
                    selectedMealType = mealType
                    currentScreen = Screen.Camera
                },
                onNavigateToFoodSearch = { currentScreen = Screen.FoodSearch },
                onNavigateToSettings = { /* TODO: Navigate to settings */ },
                onNavigateToProfile = { /* TODO: Navigate to profile */ }
            )
        }
        
        Screen.Home -> {
            HomeScreen(
                homeViewModel = homeViewModel,
                onNavigateToCamera = { currentScreen = Screen.Camera },
                onNavigateToFoodSearch = { currentScreen = Screen.FoodSearch }
            )
        }
        
        Screen.Camera -> {
            CameraScreen(
                viewModel = cameraViewModel,
                onBack = { currentScreen = Screen.Main },
                onComplete = { uri: Uri ->
                    selectedImageUri = uri
                    currentScreen = Screen.Recognition
                }
            )
        }
        
        Screen.Recognition -> {
            selectedImageUri?.let { uri ->
                FoodRecognitionScreen(
                    viewModel = recognitionViewModel,
                    imageUri = uri,
                    mealType = selectedMealType, // 传递餐次类型
                    onBack = {
                        currentScreen = Screen.Camera
                        selectedImageUri = null
                        selectedMealType = null
                    },
                    onComplete = { result: FoodRecognitionResponse ->
                        recognitionResult = result
                        currentScreen = Screen.Main
                        selectedMealType = null
                    },
                    onRetakePhoto = {
                        currentScreen = Screen.Camera
                        selectedImageUri = null
                        // 不清空 selectedMealType，保持餐次选择
                    },
                    onSaveSuccess = {
                        // 保存成功后，刷新首页数据并返回首页
                        homeViewModel.refreshNutritionData()
                        currentScreen = Screen.Main
                        selectedImageUri = null
                        selectedMealType = null
                    }
                )
            }
        }
        Screen.FoodSearch -> {
            FoodSearchScreen(
                onBack = { currentScreen = Screen.Main },
                onFoodAdded = { foodItem: FoodItem ->
                    // TODO: Handle adding food item to meal record
                    currentScreen = Screen.Main
                }
            )
        }
    }
}

enum class Screen {
    Onboarding, // 引导页
    Main,      // 主屏幕（带底部导航）
    Home,      // 首页
    Camera,    // 相机
    Recognition,  // 识别结果
    FoodSearch    // 食物搜索
}

// 检查是否是首次启动
private fun isFirstLaunch(context: Context): Boolean {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return prefs.getBoolean("is_first_launch", true)
}

// 标记已完成引导
private fun markOnboardingCompleted(context: Context) {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("is_first_launch", false).apply()
}

// 保存用户引导数据
private fun saveUserOnboardingData(context: Context, data: UserOnboardingData) {
    val prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putFloat("height", data.height)
        putFloat("weight", data.weight)
        putInt("age", data.age)
        putString("gender", data.gender)
        putString("diabetes_type", data.diabetesType)
        apply()
    }
    
    // TODO: 调用API保存到服务器并获取每日营养建议
}

