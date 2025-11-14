# AWS S3 存储桶和 IAM 访问密钥 - 详细操作指南

## 📋 准备工作

1. **AWS 账户**
   - 如果没有账户，访问 [AWS 官网](https://aws.amazon.com/) 注册
   - 如果已有账户，直接登录

2. **登录 AWS Console**
   - 访问 [AWS Console](https://console.aws.amazon.com/)
   - 使用您的 AWS 账户登录

---

## 第一步：创建 S3 存储桶

### 1. 进入 S3 服务

1. 登录 AWS Console 后，在顶部搜索栏输入 **"S3"**
2. 点击 **S3** 服务（或从服务列表中选择）

### 2. 创建存储桶

1. 在 S3 控制台页面，点击右上角的 **"创建存储桶"** 按钮

2. **配置存储桶设置**：

   **基本信息**：
   - **存储桶名称**：输入 `diabeat-ai-images`
     - ⚠️ 注意：存储桶名称必须全局唯一（全 AWS 范围内）
     - 如果名称已被占用，尝试添加后缀，如：`diabeat-ai-images-2025`
   - **AWS 区域**：选择 `ap-southeast-1`（新加坡）或其他离您最近的区域
     - 推荐区域：
       - `ap-southeast-1` - 新加坡（适合亚洲用户）
       - `us-east-1` - 美国东部（适合全球用户）
       - `eu-west-1` - 欧洲西部（适合欧洲用户）

3. **对象所有权**：
   - 选择 **"ACL 已禁用"**（推荐）

4. **阻止所有公共访问设置**：
   - ✅ **建议启用**（勾选所有选项）
   - 这样存储桶是私有的，更安全
   - 可以通过预签名 URL 访问文件

5. **存储桶版本控制**：
   - 选择 **"禁用"**（除非需要版本控制）

6. **默认加密**：
   - 选择 **"启用"**
   - 加密类型：**"Amazon S3 托管密钥 (SSE-S3)"**

7. **高级设置**（可选）：
   - 可以跳过，使用默认设置

8. **点击 "创建存储桶"**

9. ✅ **完成**：存储桶创建成功

---

## 第二步：创建 IAM 用户和访问密钥

### 1. 进入 IAM 服务

1. 在 AWS Console 顶部搜索栏输入 **"IAM"**
2. 点击 **IAM** 服务

### 2. 创建 IAM 用户

1. 在左侧菜单，点击 **"用户"**
2. 点击右上角的 **"创建用户"** 按钮

3. **指定用户详细信息**：
   - **用户名称**：输入 `diabeat-s3-user`
   - **提供 AWS 访问权限**：选择 **"提供对 AWS 服务和资源的编程访问"**
     - ✅ 勾选 **"访问密钥 - 编程访问"**

4. 点击 **"下一步"**

### 3. 设置权限

#### 方式 1：创建自定义策略（推荐）

1. 点击 **"直接附加现有策略"** 标签
2. 点击 **"创建策略"** 按钮（会打开新标签页）

3. **在策略编辑器中**：
   - 选择 **"JSON"** 标签
   - 删除默认内容，粘贴以下策略：

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
                "arn:aws:s3:::diabeat-ai-images",
                "arn:aws:s3:::diabeat-ai-images/*"
            ]
        }
    ]
}
```

   ⚠️ **重要**：将 `diabeat-ai-images` 替换为您实际创建的存储桶名称

4. 点击 **"下一步"**

5. **策略名称**：输入 `DiabEatS3Policy`
6. **描述**（可选）：`允许 DiabEat AI 访问 S3 存储桶`

7. 点击 **"创建策略"**

8. **返回用户创建页面**（关闭策略编辑器标签页，回到用户创建页面）

9. 点击 **"刷新"** 按钮（刷新策略列表）

10. 在搜索框输入 `DiabEatS3Policy`
11. ✅ 勾选刚创建的策略

12. 点击 **"下一步"**

#### 方式 2：使用现有策略（简单但不推荐）

如果不想创建自定义策略，可以使用：
- `AmazonS3FullAccess`（不推荐，权限过大）
- 或创建更细粒度的策略（推荐方式 1）

### 4. 审核和创建

1. **审核信息**：
   - 用户名：`diabeat-s3-user`
   - 访问类型：编程访问
   - 权限：`DiabEatS3Policy`

2. 点击 **"创建用户"**

### 5. 获取访问密钥 ⚠️ 重要

1. **创建成功后，会显示访问密钥信息**：
   - **访问密钥 ID**：例如 `AKIAIOSFODNN7EXAMPLE`
   - **秘密访问密钥**：例如 `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`

2. ⚠️ **重要提示**：
   - **秘密访问密钥只显示一次**
   - **立即下载 CSV 文件**（点击 "下载 .csv 文件"）
   - 或**立即复制并保存**到安全位置
   - 如果丢失，需要重新创建访问密钥

3. ✅ **保存信息**：
   ```
   访问密钥 ID: AKIAIOSFODNN7EXAMPLE
   秘密访问密钥: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
   ```

4. 点击 **"完成"**

---

## 第三步：配置到 diabeat-server

### 1. 获取存储桶信息

确认以下信息：
- **存储桶名称**：`diabeat-ai-images`（或您创建的名称）
- **AWS 区域**：`ap-southeast-1`（或您选择的区域）
- **S3 URL**：`https://diabeat-ai-images.s3.ap-southeast-1.amazonaws.com`
  - 格式：`https://[存储桶名称].s3.[区域].amazonaws.com`

### 2. 配置环境变量

#### 方式 1：在 .env 文件中配置（推荐）

编辑 `diabeat-server/config/.env`：

```env
# AWS S3 配置
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_REGION=ap-southeast-1
AWS_S3_BUCKET=diabeat-ai-images
S3_URL=https://diabeat-ai-images.s3.ap-southeast-1.amazonaws.com
```

⚠️ **替换为您的实际值**：
- `AWS_ACCESS_KEY_ID`：替换为您创建的访问密钥 ID
- `AWS_SECRET_ACCESS_KEY`：替换为您创建的秘密访问密钥
- `AWS_REGION`：替换为您选择的区域
- `AWS_S3_BUCKET`：替换为您创建的存储桶名称
- `S3_URL`：根据存储桶名称和区域生成

#### 方式 2：使用环境变量

```bash
export AWS_ACCESS_KEY_ID="your-access-key-id"
export AWS_SECRET_ACCESS_KEY="your-secret-access-key"
export AWS_REGION="ap-southeast-1"
export AWS_S3_BUCKET="diabeat-ai-images"
export S3_URL="https://diabeat-ai-images.s3.ap-southeast-1.amazonaws.com"
```

### 3. 安装依赖

```bash
cd diabeat-server
pip install boto3
```

### 4. 测试配置

创建测试脚本 `test_s3.py`：

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

# 测试连接
try:
    # 列出存储桶
    response = s3_client.list_buckets()
    print("✅ S3 连接成功")
    print(f"存储桶列表: {[b['Name'] for b in response['Buckets']]}")
    
    # 测试上传
    test_key = 'test/test.txt'
    s3_client.put_object(
        Bucket=settings.AWS_S3_BUCKET,
        Key=test_key,
        Body=b'Hello S3! This is a test.'
    )
    print(f"✅ 测试文件上传成功: {test_key}")
    
    # 测试下载
    response = s3_client.get_object(
        Bucket=settings.AWS_S3_BUCKET,
        Key=test_key
    )
    content = response['Body'].read()
    print(f"✅ 测试文件下载成功: {content.decode()}")
    
    # 清理测试文件
    s3_client.delete_object(
        Bucket=settings.AWS_S3_BUCKET,
        Key=test_key
    )
    print("✅ 测试文件已删除")
    
except Exception as e:
    print(f"❌ S3 连接失败: {e}")
    import traceback
    traceback.print_exc()
```

运行测试：

```bash
python test_s3.py
```

---

## 🔒 安全最佳实践

### 1. 保护访问密钥

- ✅ **不要**将访问密钥提交到 Git
- ✅ **不要**在代码中硬编码密钥
- ✅ 使用环境变量或 `.env` 文件（并添加到 `.gitignore`）
- ✅ 定期轮换访问密钥

### 2. 限制权限

- ✅ 只授予必要的权限（PutObject, GetObject, DeleteObject, ListBucket）
- ✅ 只允许访问特定存储桶
- ✅ 不要使用 `AmazonS3FullAccess` 策略

### 3. 存储桶安全

- ✅ 启用加密
- ✅ 阻止公共访问
- ✅ 使用预签名 URL 访问私有文件

---

## 📝 操作检查清单

### S3 存储桶

- [ ] 已创建存储桶
- [ ] 存储桶名称已记录
- [ ] AWS 区域已记录
- [ ] S3 URL 已生成

### IAM 用户

- [ ] 已创建 IAM 用户：`diabeat-s3-user`
- [ ] 已创建策略：`DiabEatS3Policy`
- [ ] 已获取访问密钥 ID
- [ ] 已获取秘密访问密钥（已保存）
- [ ] 已下载 CSV 文件（备份）

### 配置

- [ ] 已在 `.env` 文件中配置所有信息
- [ ] 已安装 `boto3` 依赖
- [ ] 已测试 S3 连接

---

## 🆘 常见问题

### Q1: 存储桶名称已被占用

**A**: 存储桶名称必须全局唯一，尝试：
- 添加后缀：`diabeat-ai-images-2025`
- 添加项目标识：`diabeat-ai-images-prod`
- 使用随机字符串：`diabeat-ai-images-abc123`

### Q2: 找不到创建的策略

**A**: 
- 确保策略已创建成功
- 点击刷新按钮
- 在搜索框输入策略名称

### Q3: 访问密钥丢失

**A**: 
- 无法恢复，需要重新创建
- 进入 IAM → 用户 → 选择用户 → 安全凭证 → 创建新的访问密钥
- 删除旧的访问密钥

### Q4: 权限不足错误

**A**: 
- 检查策略是否正确附加到用户
- 检查策略中的存储桶名称是否正确
- 检查策略中的操作权限是否包含所需操作

---

## 📚 相关资源

- [AWS S3 文档](https://docs.aws.amazon.com/s3/)
- [AWS IAM 文档](https://docs.aws.amazon.com/iam/)
- [boto3 文档](https://boto3.amazonaws.com/v1/documentation/api/latest/index.html)

---

**最后更新**：2025-11-06

