# Firebase Cloud Messaging (FCM) 配置指南

## 📋 概述

FCM 用于向 Android 客户端发送推送通知，提醒用户打胰岛素或用餐。

## 🔧 配置步骤

### 1. 创建 Firebase 项目

1. 访问 [Firebase Console](https://console.firebase.google.com/)
2. 点击 "添加项目" 或选择现有项目
3. 按照向导完成项目创建

### 2. 添加 Android 应用

1. 在 Firebase 项目中，点击 "添加应用" → 选择 Android
2. 填写应用信息：
   - **Android 包名**：`com.diabeat`
   - **应用昵称**：DiabEat AI（可选）
   - **调试签名证书 SHA-1**：可选（用于测试）

3. 下载 `google-services.json` 文件
4. 将文件放到 Android 项目的 `app/` 目录下

### 3. 获取服务账号密钥

1. 在 Firebase Console 中，进入 **项目设置** → **服务账号**
2. 点击 **生成新的私钥**
3. 下载 JSON 格式的私钥文件（例如：`diabeat-firebase-adminsdk.json`）
4. 保存到服务器安全位置

### 4. 配置服务器端

#### 方法 1：使用环境变量

```bash
# 设置 Firebase 凭证文件路径
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/diabeat-firebase-adminsdk.json"
```

#### 方法 2：在代码中指定

编辑 `app/config.py`：

```python
class Settings(BaseSettings):
    # ... 其他配置
    
    # Firebase 配置
    FIREBASE_CREDENTIALS_PATH: str = os.getenv(
        "FIREBASE_CREDENTIALS_PATH", 
        "/path/to/diabeat-firebase-adminsdk.json"
    )
```

#### 方法 3：在 .env 文件中配置

创建或编辑 `config/.env`：

```env
FIREBASE_CREDENTIALS_PATH=/path/to/diabeat-firebase-adminsdk.json
```

### 5. 安装依赖

```bash
pip install firebase-admin
```

### 6. 初始化 FCM 服务

FCM 服务会在首次使用时自动初始化（见 `app/notification/fcm_service.py`）。

## 📱 Android 客户端配置

### 1. 添加依赖

在 `app/build.gradle.kts` 中已包含：

```kotlin
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-messaging")
```

### 2. 配置 google-services.json

确保 `google-services.json` 文件在 `app/` 目录下。

### 3. 实现 FCM 服务

已创建 `app/src/main/kotlin/com/diabeat/service/FCMService.kt`，需要：

1. 在 `AndroidManifest.xml` 中注册服务：

```xml
<service
    android:name=".service.FCMService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

2. 获取 FCM Token 并发送到服务器：

```kotlin
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        // 发送 token 到服务器
        // POST /api/users/devices/register
    }
}
```

## 🧪 测试

### 测试 FCM 连接

```python
from app.notification.fcm_service import FCMService

# 初始化
FCMService.initialize()

# 发送测试通知
success = FCMService.send_notification(
    fcm_token="用户的FCM_TOKEN",
    title="测试通知",
    body="这是一条测试消息",
    data={"type": "test"}
)
```

### 使用 Firebase Console 测试

1. 在 Firebase Console 中，进入 **云消息传递**
2. 点击 **发送测试消息**
3. 输入 FCM 注册令牌
4. 编写消息并发送

## 📝 注意事项

1. **安全性**：
   - 不要将 `google-services.json` 和私钥文件提交到 Git
   - 使用环境变量或安全的配置管理

2. **权限**：
   - Android 13+ 需要通知权限
   - 在 AndroidManifest.xml 中添加：
     ```xml
     <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
     ```

3. **测试环境**：
   - 开发环境可以使用测试设备
   - 生产环境需要配置正确的包名和签名

## 🔗 相关资源

- [Firebase Console](https://console.firebase.google.com/)
- [FCM 文档](https://firebase.google.com/docs/cloud-messaging)
- [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)

