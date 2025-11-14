#!/bin/bash

# DiabEat 服务器启动脚本

echo "🚀 启动 DiabEat API 服务器..."
echo ""

# 检查虚拟环境
if [ ! -d "venv" ]; then
    echo "❌ 虚拟环境不存在，正在创建..."
    python3 -m venv venv
fi

# 激活虚拟环境
source venv/bin/activate

# 检查依赖
echo "📦 检查依赖..."
pip install -q -r requirements.txt

# 检查 .env 文件
if [ ! -f ".env" ]; then
    echo "⚠️  .env 文件不存在，正在创建..."
    cat > .env << 'EOF'
# 数据库配置 - 本地开发
DATABASE_URL="postgresql+asyncpg://diabeat:diabeat123@localhost:5432/diabeat"

# JWT配置
SECRET_KEY="development-secret-key-change-in-production"
ALGORITHM="HS256"
ACCESS_TOKEN_EXPIRE_MINUTES=30

# 应用配置
DEBUG=True
ENVIRONMENT=dev
HOST=localhost:8000

# AI配置
OPENAI_API_KEY=""
DASHSCOPE_API_KEY=""

# AWS S3配置
AWS_ACCESS_KEY_ID="AKIAW5WU5C4HN6FSSOFC"
AWS_SECRET_ACCESS_KEY="UP61KnmqnaNSip0BBij/5HW7taNtyeb3ArupsDR"
AWS_REGION="us-east-2"
AWS_S3_BUCKET="diabeat-ai-images"
S3_URL="https://diabeat-ai-images.s3.us-east-2.amazonaws.com"

# Redis配置
REDIS_URL="redis://localhost:6379/0"

# MCP配置
FASTMCP_URL="http://localhost:8001"
MCP_ENABLED=False
EOF
fi

# 检查数据库连接
echo "🔍 检查数据库连接..."
pg_isready -h localhost -p 5432 > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ PostgreSQL 数据库未运行"
    echo "💡 请确保 PostgreSQL 已启动: brew services start postgresql@15"
    exit 1
fi

echo "✅ 数据库已连接"
echo ""

# 清理旧进程
echo "🧹 清理旧进程..."
pkill -f "uvicorn app.main:app" 2>/dev/null

# 启动服务器
echo "🚀 启动 FastAPI 服务器..."
echo "📍 服务器地址: http://localhost:8000"
echo "📚 API 文档: http://localhost:8000/docs"
echo ""
echo "按 Ctrl+C 停止服务器"
echo ""

uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
