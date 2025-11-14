# AWS S3 存储配置指南

## 📋 概述

AWS S3 用于存储用户上传的食物图片，提供可扩展的云存储解决方案。

## 🔧 配置步骤

### 1. 创建 AWS 账户

1. 访问 [AWS 官网](https://aws.amazon.com/)
2. 注册或登录 AWS 账户
3. 完成账户验证

### 2. 创建 S3 存储桶（Bucket）

1. 登录 [AWS Console](https://console.aws.amazon.com/)
2. 进入 **S3** 服务
3. 点击 **创建存储桶**
4. 配置存储桶：
   - **存储桶名称**：例如 `diabeat-ai-images`（必须全局唯一）
   - **AWS 区域**：选择离用户最近的区域（例如：`ap-southeast-1`）
   - **阻止所有公共访问**：建议启用（使用预签名 URL 访问）
   - **版本控制**：可选
   - **加密**：建议启用（SSE-S3 或 SSE-KMS）

5. 点击 **创建存储桶**

### 3. 创建 IAM 用户和访问密钥

1. 进入 **IAM** 服务
2. 点击 **用户** → **创建用户**
3. 用户名：`diabeat-s3-user`
4. 选择 **访问类型**：**编程访问**
5. 点击 **下一步：权限**

#### 创建策略

1. 点击 **直接附加现有策略** → **创建策略**
2. 选择 **JSON** 标签
3. 输入以下策略（替换 `YOUR_BUCKET_NAME`）：

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:GetObject",
                "s3:DeleteObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::YOUR_BUCKET_NAME",
                "arn:aws:s3:::YOUR_BUCKET_NAME/*"
            ]
        }
    ]
}
```

4. 策略名称：`DiabEatS3Policy`
5. 点击 **创建策略**
6. 返回用户创建页面，搜索并选择刚创建的策略
7. 完成用户创建

#### 获取访问密钥

1. 创建用户后，保存 **访问密钥 ID** 和 **秘密访问密钥**
2. ⚠️ **重要**：秘密访问密钥只显示一次，请妥善保存

### 4. 配置服务器端

#### 方法 1：使用环境变量（推荐）

```bash
export AWS_ACCESS_KEY_ID="your-access-key-id"
export AWS_SECRET_ACCESS_KEY="your-secret-access-key"
export AWS_REGION="ap-southeast-1"
export AWS_S3_BUCKET="diabeat-ai-images"
export S3_URL="https://diabeat-ai-images.s3.ap-southeast-1.amazonaws.com"
```

#### 方法 2：在 .env 文件中配置

创建或编辑 `config/.env`：

```env
AWS_ACCESS_KEY_ID=your-access-key-id
AWS_SECRET_ACCESS_KEY=your-secret-access-key
AWS_REGION=ap-southeast-1
AWS_S3_BUCKET=diabeat-ai-images
S3_URL=https://diabeat-ai-images.s3.ap-southeast-1.amazonaws.com
```

#### 方法 3：在 config.py 中配置

已在 `app/config.py` 中定义：

```python
class Settings(BaseSettings):
    # AWS 配置（可选，用于生产环境）
    AWS_ACCESS_KEY_ID: str = os.getenv("AWS_ACCESS_KEY_ID", "")
    AWS_SECRET_ACCESS_KEY: str = os.getenv("AWS_SECRET_ACCESS_KEY", "")
    AWS_REGION: str = os.getenv("AWS_REGION", "")
    AWS_S3_BUCKET: str = os.getenv("AWS_S3_BUCKET", "")
    S3_URL: str = os.getenv("S3_URL", "")
```

### 5. 安装依赖

```bash
pip install boto3
```

### 6. 配置 CORS（如果需要）

如果需要在浏览器中直接访问 S3 资源：

1. 在 S3 控制台中，选择存储桶
2. 进入 **权限** → **跨源资源共享 (CORS)**
3. 添加以下配置：

```json
[
    {
        "AllowedHeaders": ["*"],
        "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
        "AllowedOrigins": ["*"],
        "ExposeHeaders": [],
        "MaxAgeSeconds": 3000
    }
]
```

## 🔒 安全最佳实践

### 1. 使用 IAM 角色（生产环境推荐）

- 在 EC2 实例上使用 IAM 角色，而不是访问密钥
- 更安全，无需管理密钥

### 2. 限制访问权限

- 只授予必要的权限（PutObject, GetObject, DeleteObject）
- 使用存储桶策略进一步限制访问

### 3. 启用加密

- 在存储桶级别启用加密
- 使用 SSE-S3（简单）或 SSE-KMS（更安全）

### 4. 使用预签名 URL（推荐）

对于私有文件，使用预签名 URL 而不是公开访问：

```python
from botocore.client import Config
import boto3

s3_client = boto3.client(
    's3',
    config=Config(signature_version='s3v4')
)

# 生成预签名 URL（有效期 1 小时）
url = s3_client.generate_presigned_url(
    'get_object',
    Params={'Bucket': 'diabeat-ai-images', 'Key': 'path/to/file.jpg'},
    ExpiresIn=3600
)
```

## 🧪 测试

### 测试 S3 连接

```python
import boto3
from app.config import settings

# 创建 S3 客户端
s3_client = boto3.client(
    's3',
    aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
    aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
    region_name=settings.AWS_REGION
)

# 测试上传
try:
    s3_client.put_object(
        Bucket=settings.AWS_S3_BUCKET,
        Key='test/test.txt',
        Body=b'Hello S3!'
    )
    print("✅ S3 连接成功")
except Exception as e:
    print(f"❌ S3 连接失败: {e}")
```

### 使用存储服务

代码中已实现自动切换：

```python
# 如果配置了 AWS，使用 S3；否则使用本地存储
if settings.AWS_S3_BUCKET and settings.AWS_ACCESS_KEY_ID:
    from app.storage.s3 import S3StorageProvider
    storage = S3StorageProvider(...)
else:
    from app.storage.local import LocalStorageProvider
    storage = LocalStorageProvider()
```

## 💰 成本估算

### S3 存储成本（示例：新加坡区域）

- **存储费用**：$0.023/GB/月
- **PUT 请求**：$0.005/1000 次
- **GET 请求**：$0.0004/1000 次

**示例**：
- 1000 张图片，每张 2MB = 2GB
- 存储费用：$0.046/月
- 上传 1000 次：$0.005
- 查看 10000 次：$0.004
- **总计**：约 $0.055/月

### 免费套餐

- AWS 免费套餐：12 个月免费
- 5GB 标准存储
- 20,000 GET 请求
- 2,000 PUT 请求

## 📝 注意事项

1. **存储桶名称**：必须全局唯一
2. **区域选择**：选择离用户最近的区域以减少延迟
3. **访问控制**：建议使用私有存储桶 + 预签名 URL
4. **成本控制**：设置生命周期策略自动删除旧文件
5. **备份**：重要数据建议启用版本控制

## 🔗 相关资源

- [AWS S3 文档](https://docs.aws.amazon.com/s3/)
- [AWS IAM 文档](https://docs.aws.amazon.com/iam/)
- [boto3 文档](https://boto3.amazonaws.com/v1/documentation/api/latest/index.html)
- [AWS 定价计算器](https://calculator.aws/)

