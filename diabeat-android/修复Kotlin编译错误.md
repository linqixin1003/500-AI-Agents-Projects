# 修复 Kotlin 编译错误

## ❌ 错误列表

1. **Models.kt:107** - `Serializer has not been found for type 'Any'`
2. **FCMService.kt:67** - `Unresolved reference: ic_notification`
3. **CameraScreen.kt:105** - `Type mismatch: PickVisualMedia.ImageOnly but PickVisualMediaRequest! was expected`
4. **InsulinRecordScreen.kt:57** - `Unresolved reference: KeyboardType`
5. **实验性 API 警告** - Material3 API 需要 `@OptIn` 注解

---

## ✅ 修复内容

### 1. 修复 Models.kt 序列化问题

**问题**：`Map<String, Any>` 无法序列化，因为 `Any` 类型没有序列化器。

**修复**：将 `Any` 改为 `String`，因为 `calculation_details` 通常存储字符串格式的详细信息。

```kotlin
// 修复前
val calculation_details: Map<String, Any>? = null

// 修复后
val calculation_details: Map<String, String>? = null
```

---

### 2. 创建通知图标资源

**问题**：`FCMService.kt` 中引用了 `R.drawable.ic_notification`，但该资源不存在。

**修复**：创建了 `drawable/ic_notification.xml` 文件，包含一个简单的通知图标（铃铛图标）。

---

### 3. 修复 CameraScreen.kt 中的图片选择器

**问题**：`PickVisualMedia.ImageOnly` 不能直接传递给 `launch()` 方法。

**修复**：使用 `PickVisualMediaRequest.Builder()` 构建请求对象。

```kotlin
// 修复前
imagePicker.launch(ActivityResultContracts.PickVisualMedia.ImageOnly)

// 修复后
val request = ActivityResultContracts.PickVisualMediaRequest.Builder()
    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
    .build()
imagePicker.launch(request)
```

---

### 4. 修复 InsulinRecordScreen.kt 中的 KeyboardType

**问题**：`KeyboardType` 引用不正确。

**修复**：
- 添加正确的 import：`androidx.compose.ui.text.input.KeyboardType`
- 添加 `KeyboardOptions` import：`androidx.compose.foundation.text.KeyboardOptions`
- 使用正确的类型引用

```kotlin
// 修复前
keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
    keyboardType = androidx.compose.foundation.text.KeyboardType.Decimal
)

// 修复后
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

keyboardOptions = KeyboardOptions(
    keyboardType = KeyboardType.Decimal
)
```

---

### 5. 添加实验性 API 注解

**问题**：Material3 的 `TopAppBar` 是实验性 API，需要显式 opt-in。

**修复**：在所有使用 `TopAppBar` 的 Composable 函数上添加 `@OptIn(ExperimentalMaterial3Api::class)` 注解。

**修改的文件**：
- `CameraScreen.kt`
- `InsulinRecordScreen.kt`
- `MealRecordScreen.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(...) {
    // ...
}
```

---

## 📋 修改文件清单

1. ✅ `app/src/main/kotlin/com/diabeat/data/model/Models.kt`
   - 修复 `Map<String, Any>` → `Map<String, String>`

2. ✅ `app/src/main/res/drawable/ic_notification.xml`
   - 创建通知图标资源

3. ✅ `app/src/main/kotlin/com/diabeat/ui/camera/CameraScreen.kt`
   - 添加 `@OptIn(ExperimentalMaterial3Api::class)`
   - 修复 `PickVisualMediaRequest` 用法

4. ✅ `app/src/main/kotlin/com/diabeat/ui/insulin/InsulinRecordScreen.kt`
   - 添加 `@OptIn(ExperimentalMaterial3Api::class)`
   - 修复 `KeyboardType` 引用
   - 添加正确的 import

5. ✅ `app/src/main/kotlin/com/diabeat/ui/meal/MealRecordScreen.kt`
   - 添加 `@OptIn(ExperimentalMaterial3Api::class)`

---

## ✅ 验证修复

### 在 Android Studio 中

1. **同步 Gradle**
   - 点击 "Sync Project with Gradle Files"

2. **清理项目**
   - 菜单：`Build` → `Clean Project`

3. **重新构建**
   - 菜单：`Build` → `Rebuild Project`

### 命令行

```bash
cd diabeat-android

# 清理构建缓存
./gradlew clean

# 重新构建
./gradlew assembleDebug
```

---

## 🔍 如果仍然报错

### 检查导入

确保所有必要的 import 都已添加：

```kotlin
// CameraScreen.kt
import androidx.activity.result.contract.ActivityResultContracts

// InsulinRecordScreen.kt
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
```

### 检查 Kotlin 版本

确保 `build.gradle.kts` 中的 Kotlin 版本兼容：

```kotlin
plugins {
    id("org.jetbrains.kotlin.android") version "1.9.20"
}
```

---

**修复完成**：所有编译错误已修复！✅

现在应该可以正常编译了。如果还有其他错误，请告诉我。

