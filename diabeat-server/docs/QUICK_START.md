# DiabEat AI 快速配置指南

## 🚀 快速开始

### 1. 通义千问 API Key（从 rock-server 获取）

#### 方式 1：从 rock-server 配置文件读取

```bash
# 查看 rock-server 的配置
cat /Users/conalin/rock-server/config/.env.stage | grep DASHSCOPE_API_KEY

# 复制 key 到 diabeat-server
export DASHSCOPE_API_KEY="从上面复制的key"
```

#### 方式 2：使用相同的环境变量

如果 rock-server 已经设置了环境变量，diabeat-server 会自动读取：

```bash
# 检查是否已设置
echo $DASHSCOPE_API_KEY

# 如果未设置，从 rock-server 的配置复制
```

#### 方式 3：在 .env 文件中配置

```bash
# 复制配置模板
cp config/.env.example config/.env

# 编辑配置文件
vim config/.env

# 添加（从 rock-server 复制相同的 key）
DASHSCOPE_API_KEY=your-dashscope-api-key-from-rock-server
```

### 2. Firebase Cloud Messaging (FCM) 配置

#### 需要的信息

1. **Firebase 项目**
   - 访问 [Firebase Console](https://console.firebase.google.com/)
   - 创建项目或使用现有项目

2. **服务账号密钥**
   - 项目设置 → 服务账号 → 生成新的私钥
   - 下载 JSON 文件

3. **配置方式**

```bash
# 方式1: 使用环境变量（推荐）
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/diabeat-firebase-adminsdk.json"

# 方式2: 在 .env 文件中
FIREBASE_CREDENTIALS_PATH=/path/to/diabeat-firebase-adminsdk.json
```

4. **Android 客户端**
   - 在 Firebase Console 中添加 Android 应用
   - 包名：`com.diabeat`
   - 下载 `google-services.json` 到 `diabeat-android/app/` 目录

**详细步骤**：参考 [FCM_SETUP.md](./FCM_SETUP.md)

### 3. AWS S3 存储配置

#### 需要的信息

1. **AWS 账户**
   - 访问 [AWS Console](https://console.aws.amazon.com/)
   - 创建账户或登录

2. **S3 存储桶**
   - 创建存储桶（名称必须全局唯一）
   - 例如：`diabeat-ai-images`

3. **IAM 访问密钥**
   - 创建 IAM 用户
   - 生成 Access Key ID 和 Secret Access Key

4. **配置方式**

```bash
# 在 .env 文件中配置
AWS_ACCESS_KEY_ID=your-aws-access-key-id
AWS_SECRET_ACCESS_KEY=your-aws-secret-access-key
AWS_REGION=ap-southeast-1
AWS_S3_BUCKET=diabeat-ai-images
S3_URL=https://diabeat-ai-images.s3.ap-southeast-1.amazonaws.com
```

**详细步骤**：参考 [AWS_S3_SETUP.md](./AWS_S3_SETUP.md)

## 📋 配置检查清单

### 必需配置 ✅

- [x] `DATABASE_URL` - 数据库连接
- [x] `SECRET_KEY` - JWT 密钥（修改默认值）
- [x] `OPENAI_API_KEY` 或 `DASHSCOPE_API_KEY` - AI API 密钥

### 可选配置 ⚙️

- [ ] `DASHSCOPE_API_KEY` - 通义千问（从 rock-server 获取）
- [ ] `FIREBASE_CREDENTIALS_PATH` - FCM 推送
- [ ] `AWS_ACCESS_KEY_ID` 等 - S3 存储

## 🔍 快速验证

```bash
# 1. 检查配置
python3 -c "from app.config import settings; print('DASHSCOPE:', '✓' if settings.DASHSCOPE_API_KEY else '✗')"

# 2. 测试数据库连接
python3 -c "import asyncio; from app.database import connect_db; asyncio.run(connect_db()); print('数据库连接成功')"

# 3. 启动服务
uvicorn app.main:app --reload
```

## 📚 详细文档

- [完整配置指南](./CONFIGURATION_GUIDE.md)
- [FCM 配置](./FCM_SETUP.md)
- [AWS S3 配置](./AWS_S3_SETUP.md)

---

**提示**：所有配置都可以通过环境变量或 `.env` 文件设置。

