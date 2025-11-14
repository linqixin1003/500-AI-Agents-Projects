# 🚀 DiabEat 服务器 - 快速参考

## 📍 服务器地址

```
http://localhost:8000
```

## 🚀 启动服务器

```bash
cd /Users/conalin/500-AI-Agents-Projects/diabeat-server
./start_server.sh
```

## 📚 API 文档

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## 🧪 测试 API

### 1. 设备认证 (创建用户)

```bash
curl -X POST http://localhost:8000/api/users/device-auth \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "device-001",
    "diabetes_type": "type1",
    "name": "Test User"
  }'
```

**响应**: 获取 `access_token` 和 `user.id`

### 2. 获取用户信息

```bash
TOKEN="your-access-token"
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8000/api/users/me
```

### 3. 创建用户参数

```bash
TOKEN="your-access-token"
USER_ID="your-user-id"

curl -X POST "http://localhost:8000/api/users/$USER_ID/parameters" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "insulin_type": "rapid",
    "isf": 2.5,
    "icr": 10.0,
    "target_bg_low": 4.0,
    "target_bg_high": 7.8
  }'
```

### 4. 获取用户参数

```bash
TOKEN="your-access-token"
USER_ID="your-user-id"

curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8000/api/users/$USER_ID/parameters"
```

### 5. 更新用户参数

```bash
TOKEN="your-access-token"
USER_ID="your-user-id"

curl -X PUT "http://localhost:8000/api/users/$USER_ID/parameters" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "insulin_type": "long-acting",
    "isf": 3.0,
    "icr": 12.0
  }'
```

## 🔍 检查服务器状态

```bash
# 健康检查
curl http://localhost:8000/health

# 查看进程
ps aux | grep uvicorn

# 查看日志
tail -f server.log
```

## 🛑 停止服务器

```bash
pkill -f "uvicorn app.main:app"
```

## 🔧 故障排除

### 数据库连接失败

```bash
# 检查 PostgreSQL
pg_isready -h localhost -p 5432

# 启动 PostgreSQL
brew services start postgresql@15
```

### Redis 连接失败

```bash
# 检查 Redis
redis-cli ping

# 启动 Redis
brew services start redis
```

### 端口被占用

```bash
# 查找占用端口的进程
lsof -ti:8000

# 杀死进程
kill -9 <PID>
```

## 📊 API 端点列表

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | /health | 健康检查 |
| POST | /api/users/device-auth | 设备认证 |
| GET | /api/users/me | 获取当前用户 |
| POST | /api/users/{id}/parameters | 创建用户参数 |
| GET | /api/users/{id}/parameters | 获取用户参数 |
| PUT | /api/users/{id}/parameters | 更新用户参数 |

## 💾 数据库信息

```
主机: localhost
端口: 5432
用户: diabeat
密码: diabeat123
数据库: diabeat
```

## 🔐 认证

所有需要认证的端点都需要在请求头中包含:

```
Authorization: Bearer {access_token}
```

## 📝 环境配置

配置文件: `.env`

关键配置:
```env
DATABASE_URL=postgresql+asyncpg://diabeat:diabeat123@localhost:5432/diabeat
SECRET_KEY=development-secret-key-change-in-production
ACCESS_TOKEN_EXPIRE_MINUTES=30
MCP_ENABLED=False
```

## ✅ 服务器状态

```
✅ 运行中
✅ 数据库已连接
✅ Redis 已连接
✅ 所有 API 正常工作
```

---

**最后更新**: 2025-11-13  
**状态**: ✅ 就绪
