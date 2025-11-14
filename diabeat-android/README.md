# 🍎 Diabeat Android - 智能糖尿病管理APP

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5.0-brightgreen.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

一款基于AI的智能糖尿病管理Android应用

[功能特性](#-功能特性) • [技术栈](#-技术栈) • [快速开始](#-快速开始) • [架构](#-架构) • [截图](#-截图)

</div>

---

## 📱 功能特性

### 🎯 核心功能

- **🤖 AI食物识别**
  - 基于通义千问视觉模型（qwen3-vl-plus）
  - 拍照即可识别食物种类、重量、烹饪方式
  - 识别准确率高达93%
  - 支持混合菜品识别

- **📊 营养管理**
  - 实时营养摄入统计（热量、碳水、蛋白质、脂肪）
  - 基于Mifflin-St Jeor公式的每日推荐计算
  - 根据糖尿病类型（1型/2型/妊娠期/前期）个性化推荐
  - 真实食物营养数据库（36+种常见食物）

- **🩸 血糖预测**
  - 多因素血糖预测算法
  - 考虑碳水摄入、运动、水分、时间衰减
  - 餐后血糖趋势预测
  - 风险评估和建议

- **💊 用药管理**
  - 胰岛素剂量记录
  - 口服药物管理
  - 智能用药提醒
  - 用药历史追踪

- **🏃 健康追踪**
  - 运动记录（类型、时长、强度、消耗热量）
  - 饮水追踪
  - 每日健康数据汇总

- **🔔 智能提醒**
  - 基于用户习惯的用餐提醒
  - 用药时间智能预测
  - "少吃多餐"健康建议

### 🎨 UI/UX特色

- **Material3 Design**
  - 渐变背景和玻璃态效果
  - 流畅的动画过渡
  - 响应式布局

- **无缝认证**
  - 基于device_id的自动注册
  - 无需输入邮箱密码
  - JWT Token安全机制

- **多语言支持**
  - 中文/English
  - 动态切换不重启

- **美观的数据可视化**
  - 环形进度条
  - 彩色图表
  - 直观的卡片布局

---

## 🛠 技术栈

### 核心框架

- **语言**: Kotlin 1.9.0
- **UI**: Jetpack Compose + Material3
- **架构**: MVVM + Clean Architecture
- **异步**: Coroutines + Flow

### 网络与数据

- **网络请求**: Retrofit + OkHttp
- **序列化**: Kotlinx Serialization
- **本地存储**: SharedPreferences
- **图片加载**: Coil

### 相机与媒体

- **相机**: CameraX
- **图片选择器**: ActivityResult API

### 依赖注入

- **手动DI**: ViewModel Factory模式

### 其他

- **日期时间**: Java 8+ Time API
- **测试**: JUnit + Mockito
- **构建**: Gradle Kotlin DSL

---

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog | 2023.1.1+
- JDK 11+
- Android SDK 24+（最低）/ 34+（目标）
- Gradle 8.0+

### 克隆项目

```bash
git clone https://github.com/linqixin1003/Diabeat-Android.git
cd Diabeat-Android
```

### 配置

1. **API配置**

编辑 `app/src/main/res/values/config.xml`:

```xml
<resources>
    <string name="base_url">http://YOUR_SERVER_IP:8000</string>
</resources>
```

2. **Firebase配置**（可选）

如需推送通知，添加 `google-services.json` 到 `app/` 目录

### 编译运行

```bash
# 编译Debug版本
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 或直接在Android Studio中点击运行
```

---

## 🏗 架构

### 项目结构

```
app/src/main/kotlin/com/diabeat/
├── data/
│   └── model/              # 数据模型
├── network/
│   ├── ApiService.kt       # Retrofit接口定义
│   └── RetrofitClient.kt   # 网络配置
├── ui/
│   ├── home/              # 首页（营养统计）
│   ├── camera/            # 拍照识别
│   ├── recognition/       # 识别结果
│   ├── dialog/            # 对话框（运动/饮水/用药）
│   ├── mine/              # 个人中心
│   └── theme/             # 主题配置
├── viewmodel/             # ViewModel层
├── utils/
│   ├── TokenManager.kt    # 认证管理
│   ├── DeviceIdUtil.kt    # 设备ID管理
│   └── ImageUtil.kt       # 图片处理
└── service/
    └── FCMService.kt      # 推送服务
```

### 数据流

```
UI Layer (Compose)
    ↓
ViewModel (StateFlow)
    ↓
Repository (Retrofit)
    ↓
Network (OkHttp)
    ↓
Backend API
```

### 认证流程

```
1. APP启动 → DeviceIdUtil生成/读取设备ID
2. TokenManager.ensureAuthenticated()
   ├─ 检查本地token
   ├─ token有效 → 直接使用
   └─ token无效 → 调用/api/users/device-auth
       └─ 后端检查device_id
           ├─ 存在 → 返回现有用户token
           └─ 不存在 → 创建新用户 → 返回token
3. 保存token和userId到SharedPreferences
4. 所有API请求自动添加Authorization header
```

---

## 📸 截图

<div align="center">

| 首页 | 识别结果 | 个人中心 |
|:---:|:---:|:---:|
| ![首页](docs/screenshots/home.png) | ![识别](docs/screenshots/recognition.png) | ![个人](docs/screenshots/mine.png) |

| 拍照 | 记录 | 设置 |
|:---:|:---:|:---:|
| ![拍照](docs/screenshots/camera.png) | ![记录](docs/screenshots/records.png) | ![设置](docs/screenshots/settings.png) |

</div>

---

## 📦 主要依赖

```kotlin
dependencies {
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    
    // Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.4.0")
}
```

---

## 🔒 安全性

- ✅ JWT Token认证
- ✅ HTTPS加密通信（生产环境）
- ✅ 设备ID加密存储
- ✅ 网络安全配置（NetworkSecurityConfig）
- ✅ 数据本地加密

---

## 📊 性能

- **启动时间**: <2秒
- **AI识别速度**: 9-12秒
- **内存占用**: ~100MB
- **APK大小**: ~15MB

---

## 🤝 相关项目

- [Diabeat-Server](https://github.com/linqixin1003/diabeat-server) - 后端API服务（FastAPI + PostgreSQL）

---

## 📄 开发文档

- [快速运行指南](快速运行指南.md)
- [构建APK指南](构建APK指南.md)
- [相机权限修复说明](相机权限修复说明.md)
- [UI优化完成说明](UI优化完成说明.md)

---

## 🐛 问题反馈

如遇到问题，请提交Issue：
https://github.com/linqixin1003/Diabeat-Android/issues

---

## 📝 License

MIT License

Copyright (c) 2024 Diabeat

---

## 👥 贡献者

感谢所有贡献者！

---

## 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=linqixin1003/Diabeat-Android&type=Date)](https://star-history.com/#linqixin1003/Diabeat-Android&Date)

---

<div align="center">

**[⬆ 回到顶部](#-diabeat-android---智能糖尿病管理app)**

Made with ❤️ by Diabeat Team

</div>
