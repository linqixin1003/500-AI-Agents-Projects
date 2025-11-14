# AWS S3 配置完成指南

## ✅ 已完成的配置

根据您提供的信息：
- ✅ 访问密钥 ID：`AKIAW5WU5C4HN6FSSOFC`
- ✅ 秘密访问密钥：`UP61KnmqnaNSip0BBij/5HW7taNtyeb3ArupsDR`
- ✅ 存储桶名称：`diabeat-ai-images`
- ✅ AWS 区域：`us-east-2`
- ✅ S3 URL：`https://diabeat-ai-images.s3.us-east-2.amazonaws.com`

## 📝 配置已保存

访问密钥已配置到 `config/.env` 文件中。

## 🧪 测试配置

### 运行测试脚本

```bash
cd diabeat-server
python test_s3_config.py
```

### 预期结果

如果配置正确，您会看到：

```
=== S3 配置测试 ===

配置信息：
  AWS_ACCESS_KEY_ID: 已设置
  AWS_SECRET_ACCESS_KEY: 已设置
  AWS_REGION: us-east-2
  AWS_S3_BUCKET: diabeat-ai-images
  S3_URL: https://diabeat-ai-images.s3.us-east-2.amazonaws.com

正在测试 S3 连接...
✅ 连接成功！
   可访问的存储桶: ['diabeat-ai-images', ...]
✅ 目标存储桶 'diabeat-ai-images' 存在

正在测试上传...
✅ 测试文件上传成功: test/connection-test.txt

正在测试下载...
✅ 测试文件下载成功: Hello S3! This is a connection test.

正在清理测试文件...
✅ 测试文件已删除

🎉 S3 配置测试通过！
```

## 🔒 安全提示

⚠️ **重要**：
1. ✅ 已保存访问密钥到 `config/.env` 文件
2. ⚠️ 确保 `.env` 文件已添加到 `.gitignore`（不要提交到 Git）
3. ⚠️ 不要将访问密钥分享给他人
4. ⚠️ 如果密钥泄露，立即在 AWS Console 中删除并重新创建

## 🚀 使用 S3 存储

配置完成后，系统会自动使用 S3 存储：

- ✅ 用户上传的食物图片会保存到 S3
- ✅ 图片 URL 会使用 S3 URL
- ✅ 不再使用本地存储

### 验证存储功能

1. **启动服务器**：
   ```bash
   uvicorn app.main:app --reload
   ```

2. **测试上传图片**：
   - 访问 `http://localhost:8000/docs`
   - 使用 `/api/food/recognize` 端点
   - 上传一张食物图片
   - 检查返回的 `image_url` 是否以 S3 URL 开头

## 📋 配置检查清单

- [x] 访问密钥 ID 已配置
- [x] 秘密访问密钥已配置
- [x] AWS 区域已配置：`us-east-2`
- [x] 存储桶名称已配置：`diabeat-ai-images`
- [x] S3 URL 已配置
- [ ] 已运行测试脚本
- [ ] 测试通过

## 🆘 如果测试失败

### 错误：AccessDenied

**原因**：IAM 策略权限不足

**解决方案**：
1. 检查策略是否正确附加到用户
2. 检查策略中的存储桶名称是否为 `diabeat-ai-images`
3. 检查策略中的操作权限

### 错误：NoSuchBucket

**原因**：存储桶不存在或名称错误

**解决方案**：
1. 在 S3 控制台检查存储桶是否存在
2. 确认存储桶名称：`diabeat-ai-images`
3. 确认区域：`us-east-2`

### 错误：InvalidAccessKeyId

**原因**：访问密钥 ID 错误

**解决方案**：
1. 检查 `AWS_ACCESS_KEY_ID` 是否正确
2. 确认没有多余的空格或换行

### 错误：SignatureDoesNotMatch

**原因**：秘密访问密钥错误

**解决方案**：
1. 检查 `AWS_SECRET_ACCESS_KEY` 是否正确
2. 确认没有多余的空格或换行
3. 如果密钥包含特殊字符，确保正确转义

---

## ✅ 完成

配置完成后，您的项目将使用 AWS S3 存储图片，提供：
- ✅ 可扩展的云存储
- ✅ 高可用性
- ✅ 自动备份
- ✅ CDN 加速（可选）

---

**配置日期**：2025-11-06

