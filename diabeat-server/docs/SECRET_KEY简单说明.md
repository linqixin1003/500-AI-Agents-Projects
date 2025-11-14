# SECRET_KEY 简单说明

## 🎯 一句话说明

**SECRET_KEY 是用来签名和验证用户登录 Token 的密钥。**

---

## 🔐 简单理解

### 类比：身份证防伪

- **身份证** = JWT Token（证明你是谁）
- **防伪印章** = SECRET_KEY（证明身份证是真的）

### 工作流程

```
1. 用户登录
   ↓
   服务器用 SECRET_KEY "盖章" 生成 Token
   ↓
   返回 Token 给用户

2. 用户访问 API
   ↓
   带上 Token
   ↓
   服务器用 SECRET_KEY "验章" 验证 Token
   ↓
   如果验证通过，允许访问
```

---

## 📝 在项目中的使用

### 1. 创建 Token（登录时）

```python
# 用户登录成功后
access_token = jwt.encode(
    {"user_id": "123"}, 
    SECRET_KEY,  # 用密钥签名
    algorithm="HS256"
)
```

### 2. 验证 Token（访问 API 时）

```python
# 用户请求 API 时
payload = jwt.decode(
    token, 
    SECRET_KEY,  # 用密钥验证
    algorithms=["HS256"]
)
# 如果验证通过，提取用户ID
```

---

## ⚠️ 为什么必须修改默认值？

### 默认值
```env
SECRET_KEY=your-secret-key-change-in-production  # ❌ 所有人都知道
```

### 问题
- 如果使用默认值，**任何人都可以伪造 Token**
- 攻击者可以冒充任何用户
- **非常不安全！**

### 正确的做法
```env
SECRET_KEY=2b4c9104f9a9ed7a29bb1c29ce09b1e828b12e298c0d03af151c3cd5c77053c5  # ✅ 随机生成
```

---

## 🎓 总结

| 项目 | 说明 |
|------|------|
| **作用** | 签名和验证 JWT Token |
| **何时使用** | 用户登录时创建 Token，访问 API 时验证 Token |
| **为什么重要** | 防止 Token 被伪造，保证系统安全 |
| **必须修改** | 默认值不安全，必须使用随机生成的密钥 |

---

**详细说明**：参考 `SECRET_KEY说明.md`

