# DiabEat AI 配置指南

## 📋 概述

本文档说明如何配置 DiabEat AI 项目的所有必需和可选服务。

## 🔑 通义千问 API Key 配置

### 从 rock-server 获取

通义千问的 API Key 与 rock-server 共享，配置方式相同。

#### 方法 1：从 rock-server 的配置文件读取

rock-server 的配置在 `config/.env.stage` 文件中：

```bash
# 查看 rock-server 的配置
cat /Users/conalin/rock-server/config/.env.stage | grep DASHSCOPE_API_KEY
```

#### 方法 2：使用环境变量（推荐）

如果 rock-server 已经配置了环境变量，可以直接使用：

```bash
# 检查环境变量
echo $DASHSCOPE_API_KEY

# 如果已设置，diabeat-server 会自动读取
# 如果未设置，需要手动设置
export DASHSCOPE_API_KEY="your-dashscope-api-key"
```

#### 方法 3：在 diabeat-server 的 .env 文件中配置

创建或编辑 `diabeat-server/config/.env`：

```env
# 从 rock-server 复制相同的 key
DASHSCOPE_API_KEY=your-dashscope-api-key-from-rock-server
```

#### 获取通义千问 API Key

如果还没有 key，可以：

1. 访问 [阿里云 DashScope](https://dashscope.console.aliyun.com/)
2. 登录阿里云账号
3. 进入 **API-KEY 管理**
4. 创建新的 API Key
5. 复制 key 并配置到环境变量或 .env 文件

### 配置验证

```python
# 测试配置
from app.config import settings
print(f"DASHSCOPE_API_KEY: {'已设置' if settings.DASHSCOPE_API_KEY else '未设置'}")
```

## 🔔 Firebase Cloud Messaging (FCM) 配置

详细配置指南请参考：[FCM_SETUP.md](./FCM_SETUP.md)

### 快速配置步骤

1. **创建 Firebase 项目**
   - 访问 [Firebase Console](https://console.firebase.google.com/)
   - 创建新项目或使用现有项目

2. **获取服务账号密钥**
   - 项目设置 → 服务账号 → 生成新的私钥
   - 下载 JSON 文件（例如：`diabeat-firebase-adminsdk.json`）

3. **配置服务器**
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/diabeat-firebase-adminsdk.json"
   ```
   或在 `.env` 文件中：
   ```env
   FIREBASE_CREDENTIALS_PATH=/path/to/diabeat-firebase-adminsdk.json
   ```

4. **安装依赖**
   ```bash
   pip install firebase-admin
   ```

5. **Android 客户端配置**
   - 在 Firebase Console 中添加 Android 应用
   - 下载 `google-services.json` 到 `diabeat-android/app/` 目录

### 需要的信息

- ✅ Firebase 项目 ID
- ✅ 服务账号私钥 JSON 文件
- ✅ Android 应用包名：`com.diabeat`

## ☁️ AWS S3 存储配置

详细配置指南请参考：[AWS_S3_SETUP.md](./AWS_S3_SETUP.md)

### 快速配置步骤

1. **创建 AWS 账户**
   - 访问 [AWS 官网](https://aws.amazon.com/)
   - 注册或登录

2. **创建 S3 存储桶**
   - 进入 S3 服务
   - 创建存储桶（名称必须全局唯一，例如：`diabeat-ai-images`）
   - 选择区域（建议选择离用户最近的区域）

3. **创建 IAM 用户和访问密钥**
   - 进入 IAM 服务
   - 创建用户：`diabeat-s3-user`
   - 附加策略（允许 S3 操作）
   - 创建访问密钥（保存 Access Key ID 和 Secret Access Key）

4. **配置服务器**
   ```bash
   export AWS_ACCESS_KEY_ID="your-access-key-id"
   export AWS_SECRET_ACCESS_KEY="your-secret-access-key"
   export AWS_REGION="ap-southeast-1"
   export AWS_S3_BUCKET="diabeat-ai-images"
   export S3_URL="https://diabeat-ai-images.s3.ap-southeast-1.amazonaws.com"
   ```
   或在 `.env` 文件中：
   ```env
   AWS_ACCESS_KEY_ID=your-access-key-id
   AWS_SECRET_ACCESS_KEY=your-secret-access-key
   AWS_REGION=ap-southeast-1
   AWS_S3_BUCKET=diabeat-ai-images
   S3_URL=https://diabeat-ai-images.s3.ap-southeast-1.amazonaws.com
   ```

5. **安装依赖**
   ```bash
   pip install boto3
   ```

### 需要的信息

- ✅ AWS Access Key ID
- ✅ AWS Secret Access Key
- ✅ AWS Region（例如：`ap-southeast-1`）
- ✅ S3 Bucket 名称（必须全局唯一）
- ✅ S3 URL（用于访问文件）

## 📝 完整配置示例

### config/.env 文件

```env
# 数据库配置
DATABASE_URL=postgresql://user:password@localhost:5432/diabeat

# AI 模型配置
OPENAI_API_KEY=your-openai-key
DASHSCOPE_API_KEY=your-dashscope-key-from-rock-server

# JWT 配置
SECRET_KEY=your-secret-key-change-in-production
ALGORITHM=HS256

# 环境配置
ENVIRONMENT=dev
HOST=localhost:8000

# Firebase 配置（可选）
FIREBASE_CREDENTIALS_PATH=/path/to/diabeat-firebase-adminsdk.json

# AWS S3 配置（可选）
AWS_ACCESS_KEY_ID=your-aws-access-key-id
AWS_SECRET_ACCESS_KEY=your-aws-secret-access-key
AWS_REGION=ap-southeast-1
AWS_S3_BUCKET=diabeat-ai-images
S3_URL=https://diabeat-ai-images.s3.ap-southeast-1.amazonaws.com
```

## 🔍 配置检查清单

### 必需配置

- [ ] `DATABASE_URL` - 数据库连接
- [ ] `SECRET_KEY` - JWT 密钥（必须修改默认值）
- [ ] `OPENAI_API_KEY` 或 `DASHSCOPE_API_KEY` - AI API 密钥（至少一个）

### 可选配置

- [ ] `DASHSCOPE_API_KEY` - 通义千问（如果使用）
- [ ] `FIREBASE_CREDENTIALS_PATH` - FCM 推送（如果使用）
- [ ] `AWS_ACCESS_KEY_ID` 等 - S3 存储（如果使用）

## 🧪 配置验证

### 测试脚本

```python
# test_config.py
from app.config import settings

print("=== 配置检查 ===")
print(f"数据库: {'✓' if settings.DATABASE_URL else '✗'}")
print(f"JWT密钥: {'✓' if settings.SECRET_KEY != 'your-secret-key-change-in-production' else '⚠ 使用默认值'}")
print(f"OpenAI: {'✓' if settings.OPENAI_API_KEY else '✗'}")
print(f"通义千问: {'✓' if settings.DASHSCOPE_API_KEY else '✗'}")
print(f"Firebase: {'✓' if hasattr(settings, 'FIREBASE_CREDENTIALS_PATH') and settings.FIREBASE_CREDENTIALS_PATH else '✗ (可选)'}")
print(f"AWS S3: {'✓' if settings.AWS_S3_BUCKET else '✗ (可选)'}")
```

运行：
```bash
python test_config.py
```

## 📚 相关文档

- [FCM 详细配置](./FCM_SETUP.md)
- [AWS S3 详细配置](./AWS_S3_SETUP.md)
- [项目 README](../README.md)

---

**最后更新**：2025-11-06

