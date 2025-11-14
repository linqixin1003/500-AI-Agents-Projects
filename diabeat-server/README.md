# DiabEat AI Server

智能餐前管理助手后端服务

## 📋 项目概述

DiabEat AI Server 是智能餐前管理助手的后端 API 服务，提供：
- 食物图像识别
- 营养成分计算
- 胰岛素剂量建议
- 血糖预测，支持MCP增强预测
- 数据记录和管理
- **MCP集成**：提供健康咨询和智能决策支持，增强血糖预测精度
  - 健康咨询：获取专业的健康建议和指导
  - 增强预测：提供更准确的血糖预测结果和风险评估
  - 智能建议：基于用户数据生成个性化的健康建议

## 🏗️ 项目结构

```
diabeat-server/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI 主应用
│   ├── config.py            # 配置管理
│   ├── database.py          # 数据库连接
│   ├── food/                # 食物识别模块
│   │   ├── __init__.py
│   │   ├── router.py
│   │   ├── service.py
│   │   ├── schemas.py
│   │   └── classifiers/     # AI 分类器
│   ├── user/                # 用户模块
│   │   ├── __init__.py
│   │   ├── router.py
│   │   ├── service.py
│   │   ├── schemas.py
│   │   └── auth_service.py
│   ├── nutrition/           # 营养计算模块
│   │   ├── __init__.py
│   │   ├── router.py
│   │   ├── service.py
│   │   ├── schemas.py
│   │   └── calculator.py
│   ├── insulin/             # 胰岛素计算模块
│   │   ├── __init__.py
│   │   ├── router.py
│   │   ├── service.py
│   │   ├── schemas.py
│   │   └── calculator.py
│   ├── prediction/          # 血糖预测模块
│   │   ├── __init__.py
│   │   ├── router.py
│   │   ├── service.py
│   │   ├── schemas.py
│   │   └── predictor.py
│   ├── storage/             # 存储服务
│   │   ├── base.py
│   │   ├── local.py
│   │   └── s3.py
│   └── middleware/          # 中间件
│       ├── auth.py
│       └── rate_limit.py
├── sql/                     # SQL 脚本
├── scripts/                 # 工具脚本
├── config/                  # 配置文件
├── requirements.txt         # Python 依赖
├── Dockerfile              # Docker 配置
└── README.md               # 本文件
```

## 🚀 快速开始

### 环境要求

- Python 3.10+
- PostgreSQL 15+
- MongoDB 6+ (可选，用于食物数据)
- Redis (可选，用于缓存)

### 安装步骤

1. **克隆项目**
```bash
cd diabeat-server
```

2. **创建虚拟环境**
```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
# 或
venv\Scripts\activate  # Windows
```

3. **安装依赖**
```bash
pip install -r requirements.txt
```

4. **配置环境变量**
```bash
cp config/.env.example config/.env
# 编辑 config/.env 文件，设置数据库连接等配置
```

5. **初始化数据库**
```bash
# 运行 SQL 脚本创建表结构
psql -U postgres -d diabeat < sql/user_schema.sql
psql -U postgres -d diabeat < sql/nutrition_schema.sql
# ... 其他表
```

6. **启动服务**
```bash
python -m app.main
# 或
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### 使用 Docker

```bash
# 构建镜像
docker build -t diabeat-server .

# 运行容器
docker run -p 8000:8000 diabeat-server
```

## 📚 API 文档

启动服务后，访问：
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## 🔧 开发指南

### 参考代码

本项目参考了相关的存储服务和认证服务实现，确保了系统的安全性和数据管理能力。

### 开发规范

请遵循 `../diabeat-ai-spec/` 目录下的规范文档：
- [项目规范原则](../diabeat-ai-spec/memory/constitution.md)
- [功能规范](../diabeat-ai-spec/specs/001-smart-meal-management/spec.md)
- [实施计划](../diabeat-ai-spec/specs/001-smart-meal-management/plan.md)
- [任务分解](../diabeat-ai-spec/specs/001-smart-meal-management/tasks.md)

### 任务执行

按照 `tasks.md` 中的任务顺序执行开发：
1. Phase 1: 基础架构 (Week 1-2)
2. Phase 2: 核心功能开发 (Week 3-5)
3. Phase 3: 预测和数据管理 (Week 6-7)
4. Phase 4: 测试和部署 (Week 8)

## 🧪 测试

```bash
# 运行测试
pytest

# 运行测试并生成覆盖率报告
pytest --cov=app --cov-report=html
```

## 📝 环境变量

主要环境变量（在 `config/.env` 中配置）：

```env
# 数据库
DATABASE_URL=postgresql://user:password@localhost:5432/diabeat

# AI 服务
OPENAI_API_KEY=your-openai-key
DASHSCOPE_API_KEY=your-dashscope-key

# JWT
SECRET_KEY=your-secret-key
ALGORITHM=HS256

# AWS (可选)
AWS_ACCESS_KEY_ID=your-key
AWS_SECRET_ACCESS_KEY=your-secret
AWS_REGION=us-east-1
AWS_S3_BUCKET=your-bucket
```

## 🚨 重要提示

### 医疗安全

- 所有胰岛素剂量建议必须包含安全限制
- 异常情况必须预警
- 所有 API 必须包含免责声明

### 代码质量

- 测试覆盖率必须 > 80%
- 所有代码必须经过审查
- 遵循规范文档中的原则

## 📖 相关文档

- [项目规范](../diabeat-ai-spec/README.md)
- [API 规范](../diabeat-ai-spec/specs/001-smart-meal-management/contracts/api-spec.json)
- [数据模型](../diabeat-ai-spec/specs/001-smart-meal-management/data-model.md)

---

**维护者**：DiabEat AI 开发团队  
**最后更新**：2025-11-06

