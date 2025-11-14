# AWS S3 操作步骤 - 图文指南

## 🎯 目标

创建 S3 存储桶和 IAM 访问密钥，用于存储 DiabEat AI 的食物图片。

---

## 第一部分：创建 S3 存储桶

### 步骤 1：登录 AWS Console

1. 访问 [https://console.aws.amazon.com/](https://console.aws.amazon.com/)
2. 使用您的 AWS 账户登录

### 步骤 2：进入 S3 服务

1. 在顶部搜索栏输入 **"S3"**
2. 点击搜索结果中的 **"S3"** 服务

### 步骤 3：创建存储桶

1. 点击页面右上角的 **"创建存储桶"** 按钮（蓝色按钮）

2. **填写存储桶配置**：

   **存储桶名称**：
   ```
   diabeat-ai-images
   ```
   - ⚠️ 如果提示名称已被占用，尝试：
     - `diabeat-ai-images-2025`
     - `diabeat-ai-images-prod`
     - `diabeat-ai-images-abc123`

   **AWS 区域**：
   - 下拉选择：**"亚太地区（新加坡）ap-southeast-1"**
   - 或选择其他离您最近的区域

3. **对象所有权**：
   - 选择：**"ACL 已禁用（推荐）"**

4. **阻止所有公共访问设置**：
   - ✅ 保持所有选项勾选（默认）
   - 这样存储桶是私有的，更安全

5. **存储桶版本控制**：
   - 选择：**"禁用"**

6. **默认加密**：
   - ✅ 勾选 **"启用"**
   - 加密类型：**"Amazon S3 托管密钥 (SSE-S3)"**

7. **高级设置**：
   - 可以跳过，使用默认设置

8. 滚动到页面底部，点击 **"创建存储桶"** 按钮

9. ✅ **完成**：您会看到 "存储桶创建成功" 的提示

10. **记录信息**：
    ```
    存储桶名称: diabeat-ai-images
    AWS 区域: ap-southeast-1
    S3 URL: https://diabeat-ai-images.s3.ap-southeast-1.amazonaws.com
    ```

---

## 第二部分：创建 IAM 用户和访问密钥

### 步骤 1：进入 IAM 服务

1. 在顶部搜索栏输入 **"IAM"**
2. 点击搜索结果中的 **"IAM"** 服务

### 步骤 2：创建 IAM 用户

1. 在左侧菜单，点击 **"用户"**
2. 点击右上角的 **"创建用户"** 按钮

### 步骤 3：填写用户信息

1. **用户名称**：
   ```
   diabeat-s3-user
   ```

2. **提供 AWS 访问权限**：
   - ✅ 勾选 **"提供对 AWS 服务和资源的编程访问"**
   - 这会自动勾选 **"访问密钥 - 编程访问"**

3. 点击 **"下一步"** 按钮

### 步骤 4：设置权限 - 创建策略

1. 在 **"设置权限"** 页面，点击 **"直接附加现有策略"** 标签

2. 点击 **"创建策略"** 按钮（会打开新标签页）

3. **在策略编辑器中**：

   a. 点击 **"JSON"** 标签

   b. 删除编辑器中的所有内容

   c. 复制并粘贴以下 JSON（⚠️ 将 `diabeat-ai-images` 替换为您实际创建的存储桶名称）：

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

   d. 点击 **"下一步"** 按钮

4. **策略名称和描述**：

   **策略名称**：
   ```
   DiabEatS3Policy
   ```

   **描述**（可选）：
   ```
   允许 DiabEat AI 访问 S3 存储桶
   ```

   e. 点击 **"创建策略"** 按钮

   f. ✅ 您会看到 "策略创建成功" 的提示

5. **返回用户创建页面**：
   - 关闭策略编辑器标签页
   - 回到用户创建页面

6. **附加策略到用户**：
   - 点击 **"刷新"** 按钮（刷新策略列表）
   - 在搜索框输入：`DiabEatS3Policy`
   - ✅ 勾选刚创建的策略

7. 点击 **"下一步"** 按钮

### 步骤 5：审核和创建

1. **审核信息**页面会显示：
   - 用户名：`diabeat-s3-user`
   - 访问类型：编程访问
   - 权限：`DiabEatS3Policy`

2. 点击 **"创建用户"** 按钮

### 步骤 6：获取访问密钥 ⚠️ 重要

1. **创建成功后，页面会显示访问密钥信息**：

   **访问密钥 ID**：
   ```
   AKIAIOSFODNN7EXAMPLE
   ```
   （这是示例，您的会不同）

   **秘密访问密钥**：
   ```
   wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
   ```
   （这是示例，您的会不同）

2. ⚠️ **立即保存**：
   - 点击 **"下载 .csv 文件"** 按钮（推荐）
   - 或立即复制并保存到安全位置
   - **秘密访问密钥只显示一次，如果丢失需要重新创建**

3. **记录信息**：
   ```
   访问密钥 ID: AKIAIOSFODNN7EXAMPLE
   秘密访问密钥: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
   ```

4. 点击 **"完成"** 按钮

---

## 第三部分：配置到项目

### 步骤 1：编辑配置文件

编辑 `diabeat-server/config/.env` 文件：

```env
# AWS S3 配置
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_REGION=ap-southeast-1
AWS_S3_BUCKET=diabeat-ai-images
S3_URL=https://diabeat-ai-images.s3.ap-southeast-1.amazonaws.com
```

⚠️ **替换为您的实际值**：
- `AWS_ACCESS_KEY_ID`：替换为您在第6步获取的访问密钥 ID
- `AWS_SECRET_ACCESS_KEY`：替换为您在第6步获取的秘密访问密钥
- `AWS_REGION`：替换为您选择的区域（例如：`ap-southeast-1`）
- `AWS_S3_BUCKET`：替换为您创建的存储桶名称
- `S3_URL`：根据存储桶名称和区域生成

### 步骤 2：安装依赖

```bash
cd diabeat-server
pip install boto3
```

### 步骤 3：测试配置

```bash
# 测试 S3 连接
python test_s3.py
```

---

## 📋 操作检查清单

### ✅ S3 存储桶

- [ ] 已登录 AWS Console
- [ ] 已进入 S3 服务
- [ ] 已创建存储桶：`diabeat-ai-images`
- [ ] 已选择区域：`ap-southeast-1`
- [ ] 已记录存储桶名称
- [ ] 已生成 S3 URL

### ✅ IAM 用户

- [ ] 已进入 IAM 服务
- [ ] 已创建用户：`diabeat-s3-user`
- [ ] 已创建策略：`DiabEatS3Policy`
- [ ] 已获取访问密钥 ID
- [ ] 已获取秘密访问密钥
- [ ] 已下载 CSV 文件（备份）

### ✅ 项目配置

- [ ] 已在 `.env` 文件中配置所有信息
- [ ] 已安装 `boto3` 依赖
- [ ] 已测试 S3 连接成功

---

## 🆘 遇到问题？

### 存储桶名称已被占用

**解决方案**：
- 尝试添加后缀：`diabeat-ai-images-2025`
- 或使用随机字符串：`diabeat-ai-images-abc123`

### 找不到创建的策略

**解决方案**：
1. 点击 **"刷新"** 按钮
2. 在搜索框输入策略名称：`DiabEatS3Policy`
3. 确保策略已创建成功

### 访问密钥丢失

**解决方案**：
1. 进入 IAM → 用户 → 选择 `diabeat-s3-user`
2. 点击 **"安全凭证"** 标签
3. 在 **"访问密钥"** 部分，点击 **"创建访问密钥"**
4. 重新获取并保存

---

## 📚 相关文档

- [AWS S3 详细配置](./AWS_S3_SETUP.md)
- [完整配置指南](./CONFIGURATION_GUIDE.md)

---

**提示**：如果遇到任何问题，可以参考 AWS 官方文档或联系 AWS 支持。

