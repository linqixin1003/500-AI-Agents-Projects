# 快速构建 APK 并在模拟器运行

## ✅ 已完成的配置

1. **API 地址已配置为模拟器地址**
   - `app/src/main/res/values/config.xml`
   - `api_base_url = http://10.0.2.2:8000`

2. **项目配置已优化**
   - settings.gradle.kts 已简化
   - build.gradle.kts 已添加 repositories

---

## 🚀 推荐方法：使用 Android Studio

### 步骤 1：打开项目

1. 打开 Android Studio
2. 选择 `Open` 或 `Open Project`
3. 选择 `diabeat-android` 目录
4. 等待项目同步完成

### 步骤 2：同步 Gradle

- 如果提示 "Sync Project with Gradle Files"，点击同步
- 等待依赖下载完成（首次可能需要几分钟）

### 步骤 3：构建 APK

**方法 A：直接运行（推荐）**
1. 点击工具栏的 "Run" 按钮 ▶️
2. 选择模拟器或连接设备
3. Android Studio 会自动构建并安装 APK

**方法 B：只构建 APK**
1. 菜单：`Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. 等待构建完成
3. 点击通知中的 "locate" 查看 APK 位置

**APK 位置**：`app/build/outputs/apk/debug/app-debug.apk`

### 步骤 4：启动服务器（必需）

在另一个终端运行：

```bash
cd diabeat-server
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

**重要**：
- 必须监听 `0.0.0.0`，不能只监听 `127.0.0.1`
- 模拟器使用 `10.0.2.2` 访问本地服务器

### 步骤 5：在模拟器运行

1. **创建/启动模拟器**
   - 点击工具栏的 "Device Manager"
   - 如果没有模拟器，点击 "Create Device"
   - 选择设备（推荐：Pixel 5）和系统镜像（API 33+）
   - 点击 "Play" 按钮启动模拟器

2. **运行应用**
   - 在 Android Studio 中选择模拟器
   - 点击 "Run" 按钮 ▶️
   - 应用会自动安装并启动

---

## 📱 使用命令行（备选）

### 如果命令行构建失败，使用 Android Studio

命令行构建可能遇到 Gradle 版本或缓存问题，建议使用 Android Studio。

### 清理并重试（如果必须使用命令行）

```bash
cd diabeat-android

# 清理缓存
rm -rf .gradle build app/build

# 使用系统 Gradle 构建
gradle assembleDebug

# 或创建 wrapper 后使用
gradle wrapper
./gradlew assembleDebug
```

---

## 🧪 验证运行

### 1. 检查服务器

```bash
# 测试服务器是否运行
curl http://localhost:8000/health

# 应该返回：{"status":"ok"}
```

### 2. 检查应用

- 应用应该能正常启动
- 显示相机界面
- 可以拍照

### 3. 检查网络连接

- 如果服务器未启动，应用会显示网络错误
- 启动服务器后，应该能正常调用 API

---

## 📋 完整流程

```bash
# 1. 启动服务器（终端 1）
cd diabeat-server
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 2. 在 Android Studio 中
# - 打开 diabeat-android 项目
# - 同步 Gradle
# - 创建/启动模拟器
# - 点击 Run 按钮

# 3. 验证
# - 应用启动
# - 相机界面显示
# - 可以拍照
```

---

## 🐛 常见问题

### 问题 1：构建失败

**解决**：
- 使用 Android Studio（推荐）
- 清理缓存：`rm -rf .gradle build app/build`
- 重新同步 Gradle

### 问题 2：模拟器无法连接服务器

**检查**：
1. 服务器是否启动：`curl http://localhost:8000/health`
2. 服务器是否监听 `0.0.0.0`：检查启动命令
3. 防火墙是否阻止

**解决**：
```bash
# 确保服务器监听所有接口
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 问题 3：APK 安装失败

**解决**：
```bash
# 卸载旧版本
adb uninstall com.diabeat

# 重新安装
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## ✅ 检查清单

- [x] API 地址已配置（`http://10.0.2.2:8000`）
- [ ] 项目已在 Android Studio 中打开
- [ ] Gradle 已同步
- [ ] 服务器已启动（`uvicorn app.main:app --reload --host 0.0.0.0 --port 8000`）
- [ ] 模拟器已启动
- [ ] APK 已构建并安装
- [ ] 应用已运行

---

**推荐**：使用 Android Studio 构建和运行，这是最简单可靠的方法。

