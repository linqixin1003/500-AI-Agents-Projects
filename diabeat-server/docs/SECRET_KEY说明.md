# SECRET_KEY（JWT 密钥）详细说明

## 🔐 什么是 JWT？

**JWT（JSON Web Token）** 是一种用于身份认证的令牌（Token）。

### 简单理解

想象一下：
- **身份证**：证明你是谁
- **JWT Token**：证明你已登录，可以访问系统

---

## 🔑 SECRET_KEY 的作用

`SECRET_KEY` 是用于**签名和验证 JWT Token** 的密钥。

### 类比理解

把 JWT Token 想象成一张**防伪票据**：

1. **创建 Token（登录时）**：
   - 用户登录成功
   - 服务器用 `SECRET_KEY` **签名**生成 Token
   - 就像在票据上盖上**官方印章**

2. **验证 Token（访问 API 时）**：
   - 用户每次请求 API 时，都会带上 Token
   - 服务器用 `SECRET_KEY` **验证** Token 是否有效
   - 就像检查票据上的**印章**是否真实

---

## 📝 在项目中的实际使用

### 1. 用户登录（创建 Token）

```python
# app/user/router.py
@router.post("/login")
async def login(form_data: OAuth2PasswordRequestForm):
    # 验证用户名和密码
    user = await auth_service.authenticate_user(...)
    
    # 使用 SECRET_KEY 创建 Token
    access_token = auth_service.create_access_token(
        data={"sub": user.id}  # 用户ID
    )
    # 返回 Token 给客户端
    return {"access_token": access_token, ...}
```

**代码位置**：`app/user/auth_service.py:29`
```python
encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
```

### 2. 访问 API（验证 Token）

```python
# app/user/auth_service.py
async def get_current_user(token: str):
    # 使用 SECRET_KEY 验证 Token
    payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
    user_id = payload.get("sub")
    # 返回用户信息
    return user
```

**代码位置**：`app/user/auth_service.py:55`
```python
payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
```

### 3. 保护 API 端点

```python
# 例如：记录用餐时间
@router.post("/api/records/meal")
async def create_meal_record(
    request: schemas.MealRecordCreate,
    current_user: UserResponse = Depends(get_current_user_dependency)  # 需要 Token
):
    # 只有提供有效 Token 的用户才能访问
    return await record_service.create_meal_record(request, current_user.id)
```

---

## 🎯 为什么需要 SECRET_KEY？

### 1. **安全性**
- 防止 Token 被伪造
- 只有知道 `SECRET_KEY` 的服务器才能创建和验证 Token
- 如果密钥泄露，任何人都可以伪造 Token

### 2. **身份验证**
- 验证 Token 是否由本服务器签发
- 确保 Token 没有被篡改

### 3. **数据完整性**
- 如果 Token 被修改，验证会失败
- 保证 Token 中的数据（如用户ID）是可信的

---

## ⚠️ 为什么必须修改默认值？

### 默认值的问题

```env
SECRET_KEY=your-secret-key-change-in-production  # ❌ 不安全
```

**问题**：
1. **公开的秘密**：所有人都知道这个默认值
2. **容易被破解**：攻击者可以用这个密钥伪造 Token
3. **安全风险**：任何人都可以冒充任何用户

### 正确的做法

```env
SECRET_KEY=2b4c9104f9a9ed7a29bb1c29ce09b1e828b12e298c0d03af151c3cd5c77053c5  # ✅ 安全
```

**优点**：
1. **随机生成**：无法猜测
2. **足够长**：64 个字符，难以暴力破解
3. **唯一性**：每个项目使用不同的密钥

---

## 🔄 工作流程示例

### 场景：用户访问"记录用餐时间"API

```
1. 用户登录
   ↓
   服务器验证用户名密码
   ↓
   使用 SECRET_KEY 创建 Token
   ↓
   返回 Token 给客户端

2. 客户端保存 Token
   ↓
   每次请求 API 时，在 Header 中带上 Token
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

3. 服务器接收请求
   ↓
   从 Header 中提取 Token
   ↓
   使用 SECRET_KEY 验证 Token
   ↓
   如果验证通过，提取用户ID
   ↓
   执行 API 操作（记录用餐时间）
   ↓
   返回结果
```

---

## 📊 Token 的结构

JWT Token 由三部分组成：

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

1. **Header（头部）**：算法和类型
2. **Payload（载荷）**：用户信息（如用户ID）
3. **Signature（签名）**：使用 `SECRET_KEY` 生成的签名

**SECRET_KEY 用于生成和验证 Signature**

---

## 🔒 安全建议

### ✅ 应该做的

1. **使用随机生成的密钥**
   ```bash
   openssl rand -hex 32
   ```

2. **密钥长度至少 32 字符**
   - 推荐：64 字符

3. **不同环境使用不同密钥**
   - 开发环境：`dev-secret-key-xxx`
   - 生产环境：`prod-secret-key-xxx`

4. **定期更换密钥**（生产环境）
   - 如果密钥泄露，立即更换

### ❌ 不应该做的

1. **不要使用默认值**
   ```env
   SECRET_KEY=your-secret-key-change-in-production  # ❌
   ```

2. **不要将密钥提交到 Git**
   - 确保 `.env` 在 `.gitignore` 中

3. **不要分享密钥**
   - 不要通过邮件、聊天工具分享

4. **不要使用简单密钥**
   ```env
   SECRET_KEY=123456  # ❌ 太简单
   SECRET_KEY=password  # ❌ 太简单
   ```

---

## 🎓 总结

### SECRET_KEY 的作用

1. **创建 Token**：用户登录时，用密钥签名生成 Token
2. **验证 Token**：用户访问 API 时，用密钥验证 Token 是否有效
3. **保证安全**：防止 Token 被伪造或篡改

### 为什么必须修改

- 默认值是公开的，不安全
- 攻击者可以用默认密钥伪造 Token
- 必须使用随机生成的强密钥

### 在项目中的位置

- **创建 Token**：`app/user/auth_service.py:29`
- **验证 Token**：`app/user/auth_service.py:55`
- **配置位置**：`app/config.py:35`
- **环境变量**：`config/.env`

---

**最后更新**：2025-11-06

