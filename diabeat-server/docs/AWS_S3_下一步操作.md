# AWS S3 配置 - 下一步操作

## ✅ 已完成

根据您的信息，您已经完成：
- ✅ S3 存储桶：`diabeat-ai-images`
- ✅ AWS 区域：`us-east-2`（美国东部俄亥俄）
- ✅ IAM 用户：`diabeat-s3-user`
- ✅ IAM 策略：`DiabEatS3Policy`

## 🔑 下一步：获取访问密钥

### 步骤 1：进入 IAM 用户页面

1. 在 AWS Console 顶部搜索栏输入 **"IAM"**
2. 点击 **IAM** 服务
3. 在左侧菜单，点击 **"用户"**
4. 找到并点击用户：`diabeat-s3-user`

### 步骤 2：创建访问密钥

1. 在用户详情页面，点击 **"安全凭证"** 标签
2. 滚动到 **"访问密钥"** 部分
3. 点击 **"创建访问密钥"** 按钮

### 步骤 3：选择使用案例

1. 选择 **"应用程序在 AWS 外部运行"**
2. 点击 **"下一步"**

### 步骤 4：获取访问密钥 ⚠️ 重要

1. **页面会显示访问密钥信息**：
   - **访问密钥 ID**：例如 `AKIAIOSFODNN7EXAMPLE`
   - **秘密访问密钥**：例如 `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`

2. ⚠️ **立即保存**：
   - 点击 **"下载 .csv 文件"** 按钮（推荐）
   - 或立即复制并保存到安全位置
   - **秘密访问密钥只显示一次**

3. 点击 **"完成"**

---

## 📝 配置到项目

### 步骤 1：生成 S3 URL

根据您的信息：
- 存储桶名称：`diabeat-ai-images`
- 区域：`us-east-2`

S3 URL 格式：
```
https://diabeat-ai-images.s3.us-east-2.amazonaws.com
```

### 步骤 2：编辑配置文件

编辑 `diabeat-server/config/.env` 文件：

```env
# AWS S3 配置
AWS_ACCESS_KEY_ID=您的访问密钥ID
AWS_SECRET_ACCESS_KEY=您的秘密访问密钥
AWS_REGION=us-east-2
AWS_S3_BUCKET=diabeat-ai-images
S3_URL=https://diabeat-ai-images.s3.us-east-2.amazonaws.com
```

⚠️ **替换为您的实际值**：
- `AWS_ACCESS_KEY_ID`：替换为您获取的访问密钥 ID
- `AWS_SECRET_ACCESS_KEY`：替换为您获取的秘密访问密钥

### 步骤 3：安装依赖

```bash
cd diabeat-server
pip install boto3
```

### 步骤 4：测试配置

创建测试文件 `test_s3_config.py`：

```python
#!/usr/bin/env python3
"""测试 S3 配置"""

import boto3
import sys
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent))

from app.config import settings

print("=== S3 配置测试 ===\n")

# 检查配置
print("配置信息：")
print(f"  AWS_ACCESS_KEY_ID: {'已设置' if settings.AWS_ACCESS_KEY_ID else '未设置'}")
print(f"  AWS_SECRET_ACCESS_KEY: {'已设置' if settings.AWS_SECRET_ACCESS_KEY else '未设置'}")
print(f"  AWS_REGION: {settings.AWS_REGION}")
print(f"  AWS_S3_BUCKET: {settings.AWS_S3_BUCKET}")
print(f"  S3_URL: {settings.S3_URL}")

if not settings.AWS_ACCESS_KEY_ID or not settings.AWS_SECRET_ACCESS_KEY:
    print("\n❌ 请先配置 AWS 访问密钥")
    sys.exit(1)

# 测试连接
try:
    print("\n正在测试 S3 连接...")
    
    s3_client = boto3.client(
        's3',
        aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
        aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
        region_name=settings.AWS_REGION
    )
    
    # 测试列出存储桶
    response = s3_client.list_buckets()
    buckets = [b['Name'] for b in response['Buckets']]
    print(f"✅ 连接成功！")
    print(f"   可访问的存储桶: {buckets}")
    
    # 检查目标存储桶是否存在
    if settings.AWS_S3_BUCKET in buckets:
        print(f"✅ 目标存储桶 '{settings.AWS_S3_BUCKET}' 存在")
    else:
        print(f"⚠️  目标存储桶 '{settings.AWS_S3_BUCKET}' 不在列表中")
    
    # 测试上传
    print("\n正在测试上传...")
    test_key = 'test/connection-test.txt'
    s3_client.put_object(
        Bucket=settings.AWS_S3_BUCKET,
        Key=test_key,
        Body=b'Hello S3! This is a connection test.'
    )
    print(f"✅ 测试文件上传成功: {test_key}")
    
    # 测试下载
    print("正在测试下载...")
    response = s3_client.get_object(
        Bucket=settings.AWS_S3_BUCKET,
        Key=test_key
    )
    content = response['Body'].read()
    print(f"✅ 测试文件下载成功: {content.decode()}")
    
    # 清理测试文件
    print("正在清理测试文件...")
    s3_client.delete_object(
        Bucket=settings.AWS_S3_BUCKET,
        Key=test_key
    )
    print("✅ 测试文件已删除")
    
    print("\n🎉 S3 配置测试通过！")
    
except Exception as e:
    print(f"\n❌ S3 连接失败: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
```

运行测试：

```bash
cd diabeat-server
python test_s3_config.py
```

---

## 📋 配置检查清单

- [ ] 已获取访问密钥 ID
- [ ] 已获取秘密访问密钥（已保存）
- [ ] 已在 `.env` 文件中配置所有信息
- [ ] 已安装 `boto3` 依赖
- [ ] 已测试 S3 连接成功

---

## 🔍 如果遇到问题

### 问题 1：找不到"创建访问密钥"按钮

**解决方案**：
- 确保您在用户详情页面的 **"安全凭证"** 标签
- 如果用户刚创建，可能需要刷新页面

### 问题 2：权限不足错误

**解决方案**：
- 检查策略是否正确附加到用户
- 检查策略中的存储桶名称是否正确（应该是 `diabeat-ai-images`）
- 检查策略中的区域是否正确

### 问题 3：存储桶不存在错误

**解决方案**：
- 确认存储桶名称：`diabeat-ai-images`
- 确认区域：`us-east-2`
- 在 S3 控制台检查存储桶是否存在

---

## ✅ 完成后的配置示例

您的 `config/.env` 文件应该包含：

```env
# AWS S3 配置
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_REGION=us-east-2
AWS_S3_BUCKET=diabeat-ai-images
S3_URL=https://diabeat-ai-images.s3.us-east-2.amazonaws.com
```

---

**提示**：配置完成后，系统会自动使用 S3 存储，而不是本地存储。

