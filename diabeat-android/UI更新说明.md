# DiabEat Android UI 更新说明

## 更新内容

已成功将 diabeat-android 的 UI 更新为类似 rock-android 的设计风格。

## 主要变更

### 1. 底部导航栏 (BottomTab.kt)
- ✅ 创建了新的底部导航组件
- ✅ 包含两个 Tab：首页和我的
- ✅ 参考了 rock-android 的样式

**文件位置**: `app/src/main/kotlin/com/diabeat/ui/BottomTab.kt`

### 2. 主屏幕 (MainScreen.kt)
- ✅ 创建了新的主屏幕布局
- ✅ 集成了底部导航栏
- ✅ 中间添加了圆形相机按钮（悬浮在底部导航栏上方）
- ✅ 根据选中的 Tab 切换内容区域（首页/我的）

**文件位置**: `app/src/main/kotlin/com/diabeat/ui/main/MainScreen.kt`

### 3. 我的页面 (MineScreen.kt)
- ✅ 创建了新的"我的"页面
- ✅ 包含用户信息卡片
- ✅ 包含功能菜单项（用户参数、餐食历史、设置、关于）

**文件位置**: `app/src/main/kotlin/com/diabeat/ui/mine/MineScreen.kt`

### 4. MainActivity 更新
- ✅ 添加了新的 Screen.Main 导航状态
- ✅ 更新了导航逻辑，默认显示主屏幕
- ✅ 完成拍照/识别后返回主屏幕

**文件位置**: `app/src/main/kotlin/com/diabeat/ui/MainActivity.kt`

### 5. 字符串资源
- ✅ 添加了底部导航栏相关的字符串
- ✅ 添加了"我的"页面相关的字符串
- ✅ 中英文翻译均已完成

**文件位置**: 
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en/strings.xml`

## 需要添加的资源文件

### 图标资源（drawable）

请在 `app/src/main/res/drawable/` 目录下添加以下图标：

#### 底部导航图标
1. `ic_home_selected.xml` - 首页选中图标
2. `ic_home_unselected.xml` - 首页未选中图标
3. `ic_mine_selected.xml` - 我的选中图标
4. `ic_mine_unselected.xml` - 我的未选中图标

#### 相机按钮
5. `icon_camera.xml` 或 `icon_camera.png` - 中间的相机按钮图标

#### 底部导航背景
6. `bg_tab_group.xml` 或 `bg_tab_group.png` - 底部导航栏背景图（可选，如果不需要特殊背景可以删除）

### 创建图标的建议

**方式1：使用 Vector Asset**
1. 在 Android Studio 中右键点击 `drawable` 文件夹
2. 选择 `New` -> `Vector Asset`
3. 选择 Material Icons 或导入 SVG 文件

**方式2：从 rock-android 复制**
可以参考 rock-android 项目中的图标：
- `taber_home_press.png/xml`
- `taber_home_normal.png/xml`
- `taber_personal_press.png/xml`
- `taber_personal_normal.png/xml`
- `icon_camera.png/xml`
- `bg_tab_group.png/xml`

**方式3：使用在线图标库**
- Material Icons: https://fonts.google.com/icons
- Flaticon: https://www.flaticon.com/
- Icons8: https://icons8.com/

### 临时解决方案

如果暂时无法添加图标，可以：
1. 使用 Material Icons 的默认图标
2. 修改代码使用文本标签代替图标
3. 使用 emoji 表情符号作为临时图标

## UI 结构对比

### rock-android
```
MainActivity
└── MainScreen
    ├── 底部导航背景图
    ├── BottomTab (首页, 我的)
    └── 中间圆形相机按钮
        ├── HomeScreen (首页内容)
        └── MineScreen (我的内容)
```

### diabeat-android (更新后)
```
MainActivity
└── MainScreen (新增)
    ├── 底部导航背景图
    ├── BottomTab (首页, 我的)
    └── 中间圆形相机按钮
        ├── HomeScreen (首页内容)
        └── MineScreen (我的内容)
```

## 相机流程

### 当前流程
1. 点击中间相机按钮 → CameraScreen
2. 拍照或选择图片 → FoodRecognitionScreen
3. 识别完成 → 返回 MainScreen (首页)

### 与 rock-android 的区别
- rock-android: 使用更复杂的导航系统（NavHostController）
- diabeat-android: 使用简单的状态管理（Screen enum）

## 编译和运行

### 1. 添加图标资源后编译
```bash
cd /Users/conalin/500-AI-Agents-Projects/diabeat-android
./gradlew assembleDebug
```

### 2. 如果遇到编译错误
检查以下几点：
- 所有引用的 drawable 资源是否存在
- strings.xml 文件是否正确
- 包名和导入是否正确

### 3. 运行应用
```bash
./gradlew installDebug
```

## 下一步工作

### 必须完成
- [ ] 添加所有必需的图标资源
- [ ] 测试底部导航切换功能
- [ ] 测试相机拍照流程
- [ ] 测试"我的"页面的各个菜单项

### 可选改进
- [ ] 添加页面切换动画
- [ ] 优化相机按钮的视觉效果（阴影、渐变等）
- [ ] 实现"我的"页面的完整功能（设置、关于等）
- [ ] 添加用户资料编辑功能
- [ ] 实现餐食历史记录查看

## 参考文件

如需进一步了解 rock-android 的实现，可以查看：
- `/Users/conalin/500-AI-Agents-Projects/rock-android/app/src/main/kotlin/com/lingjuetech/rock/ui/BottomTab.kt`
- `/Users/conalin/500-AI-Agents-Projects/rock-android/app/src/main/kotlin/com/lingjuetech/rock/ui/main/MainScreen.kt`
- `/Users/conalin/500-AI-Agents-Projects/rock-android/app/src/main/kotlin/com/lingjuetech/rock/ui/camera/CameraScreen.kt`

## 注意事项

1. **图标尺寸**: 建议使用 24dp 的标准尺寸
2. **颜色主题**: 确保图标颜色与 MaterialTheme 兼容
3. **导航逻辑**: 当前使用简单的状态管理，如果需要更复杂的导航可以考虑使用 Jetpack Navigation

---

**更新日期**: 2025-11-13  
**作者**: Cascade AI Assistant
