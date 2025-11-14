"""FastAPI应用主入口"""
from fastapi import FastAPI, Request, HTTPException, Depends
from contextlib import asynccontextmanager
import uvicorn
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import os
import uuid
import datetime

# 日志模块
from app.utils.logger import setup_logging, get_logger, info, error, warning, debug, critical

# 数据库和配置
from app.database import database, connect_db, disconnect_db, Base, engine, init_redis
from app.config import settings

# 安全模块
from app.security.cors import configure_cors, create_cors_config_for_environment
from app.security.rate_limiting import rate_limiter, rate_limit, DEFAULT_RATE_LIMIT, STRICT_RATE_LIMIT

# 初始化日志配置
setup_logging(
    log_level=getattr(settings, "LOG_LEVEL", "INFO"),
    log_file=getattr(settings, "LOG_FILE", "app.log"),
    structured=getattr(settings, "USE_STRUCTURED_LOGGING", False),
    log_dir=getattr(settings, "LOG_DIR", "./logs")
)

logger = get_logger(__name__)

# 路由
from app.user.router import router as user_router
from app.food.router import router as food_router
from app.nutrition.router import router as nutrition_router
from app.insulin.router import router as insulin_router
from app.prediction.router import router as prediction_router
from app.records.router import router as records_router
from app.notification.router import router as notification_router
from app.user.device_router import router as device_router



# 导入监控模块
from app.monitoring.health_check import health_check_router
from app.monitoring.metrics import setup_metrics

# 工具
from app.utils import fastmcp_client
from fastapi.staticfiles import StaticFiles



@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    try:
        info(logger, "应用启动开始")
        
        # 初始化数据库连接
        info(logger, "正在连接数据库...")
        try:
            await connect_db()
            info(logger, "✅ 数据库连接成功")
            
            # 创建数据库表
            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)
            info(logger, "✅ 数据库表创建完成")
        except Exception as e:
            error(logger, f"⚠️  数据库连接失败: {str(e)}")
            warning(logger, "⚠️  某些功能可能不可用（用户注册、数据记录等）")
            info(logger, "💡 提示：如果需要数据库功能，请启动 PostgreSQL 数据库")
        
        # 初始化Redis缓存
        info(logger, "正在初始化Redis缓存...")
        try:
            await init_redis()
            info(logger, "✅ Redis缓存初始化完成")
        except Exception as e:
            error(logger, f"⚠️ Redis初始化失败: {str(e)}")
            warning(logger, "⚠️  缓存功能将不可用，系统性能可能受到影响")
        
        # 确保 static 目录存在
        os.makedirs("static", exist_ok=True)
        os.makedirs("static/uploads", exist_ok=True)
        
        # 检查MCP服务健康状态
        info(logger, "正在检查MCP服务健康状态...")
        try:
            is_healthy = await fastmcp_client.health_check()
            if is_healthy:
                info(logger, "✅ MCP服务连接正常")
                # 尝试列出可用代理
                try:
                    agents = await fastmcp_client.list_agents()
                    info(logger, f"✅ 发现 {len(agents)} 个可用MCP代理")
                except Exception as e:
                    warning(logger, f"⚠️  无法获取MCP代理列表: {str(e)}")
            else:
                warning(logger, "⚠️  MCP服务连接失败")
                warning(logger, "⚠️  MCP增强功能将不可用，系统将使用回退方案")
        except Exception as e:
            error(logger, f"⚠️  MCP服务健康检查失败: {str(e)}")
            warning(logger, "⚠️  MCP相关功能可能不可用")
        
        # 启动后台任务（通知处理）
        try:
            from app.notification.background_tasks import NotificationBackgroundTask
            import asyncio
            
            # 创建后台任务（在后台运行，不阻塞主应用）
            async def start_background_task():
                try:
                    await NotificationBackgroundTask.run_periodic_task()
                except asyncio.CancelledError:
                    info(logger, "后台任务已取消")
                except Exception as e:
                    error(logger, f"后台任务错误: {str(e)}")
            
            # 在后台启动任务
            background_task = asyncio.create_task(start_background_task())
            info(logger, "✅ 后台任务已启动（通知处理）")
        except Exception as e:
            warning(logger, f"⚠️ 后台任务启动失败（可选功能）: {str(e)}")
            warning(logger, "   通知功能仍可用，但需要手动触发或使用外部任务队列")
        
        info(logger, "✅ 应用启动完成，服务就绪")
        yield
        
    finally:
        # 关闭时的清理操作
        info(logger, "正在断开数据库连接...")
        try:
            await disconnect_db()
            info(logger, "✅ 数据库连接已断开")
        except Exception as e:
            error(logger, f"⚠️  关闭数据库连接时出错: {str(e)}")

# 创建FastAPI应用实例
app = FastAPI(
    title="DiabEat API",
    description="智能餐前管理助手 API 服务",
    version=settings.api_version,
    lifespan=lifespan,
)

# 配置CORS
environment = os.getenv("ENVIRONMENT", "development")
configure_cors(app, create_cors_config_for_environment(environment))

# 添加请求速率限制中间件
from app.security.rate_limiting import rate_limiter_middleware
app.middleware("http")(rate_limiter_middleware)

# 配置Prometheus监控指标
setup_metrics(app)

# 注册监控路由
app.include_router(health_check_router)

# 全局请求头依赖
async def get_headers(request: Request):
    """
    获取请求头信息
    用于后续的认证和请求跟踪
    """
    return request.headers


# 注册路由
app.include_router(
    user_router,
    prefix="/api/users",
    tags=["users"]
)

app.include_router(
    food_router,
    prefix="/api/food",
    tags=["food"]
)

app.include_router(
    nutrition_router,
    prefix="/api/nutrition",
    tags=["nutrition"]
)

app.include_router(
    insulin_router,
    prefix="/api/insulin",
    tags=["insulin"]
)

# 注册预测服务路由，包含MCP增强功能支持
app.include_router(
    prediction_router,
    prefix="/api/prediction",
    tags=["prediction"]
)

app.include_router(
    records_router,
    prefix="/api/records",
    tags=["records"]
)

app.include_router(
    notification_router,
    prefix="/api/notifications",
    tags=["notifications"]
)

# 导入并注册智能提醒路由
from app.reminders import router as reminders_router
app.include_router(
    reminders_router,
    prefix="/api/reminders",
    tags=["reminders"]
)

app.include_router(
    device_router,
    prefix="/api/users",
    tags=["devices"]
)



# 静态文件服务
app.mount("/static", StaticFiles(directory="static"), name="static")

# 异常处理器
@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """自定义HTTP异常处理"""
    request_id = getattr(request.state, "request_id", "-")
    user_id = "-"
    try:
        if hasattr(request.state, "user") and request.state.user:
            user_id = str(request.state.user.id)
    except Exception:
        pass
    client_ip = request.client.host if request.client else "unknown"
    
    # 根据状态码级别记录不同级别的日志
    if exc.status_code >= 500:
        error(
            logger,
            f"HTTP异常 {exc.status_code}: {exc.detail}",
            request_id=request_id,
            user_id=user_id,
            ip=client_ip,
            extra={
                "path": request.url.path,
                "method": request.method,
                "detail": exc.detail
            }
        )
    elif exc.status_code >= 400:
        warning(
            logger,
            f"HTTP异常 {exc.status_code}: {exc.detail}",
            request_id=request_id,
            user_id=user_id,
            ip=client_ip,
            extra={
                "path": request.url.path,
                "method": request.method,
                "detail": exc.detail
            }
        )
    
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "error": "http_error",
            "message": exc.detail,
            "status_code": exc.status_code,
            "path": request.url.path,
            "request_id": request_id,
            "timestamp": datetime.datetime.utcnow().isoformat() + "Z"
        }
    )

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """全局异常处理器"""
    request_id = getattr(request.state, "request_id", "-")
    user_id = "-"
    try:
        if hasattr(request.state, "user") and request.state.user:
            user_id = str(request.state.user.id)
    except Exception:
        pass
    client_ip = request.client.host if request.client else "unknown"
    
    # 记录详细的错误信息和堆栈
    error(
        logger,
        f"未捕获的异常: {str(exc)}",
        request_id=request_id,
        user_id=user_id,
        ip=client_ip,
        extra={
            "path": request.url.path,
            "method": request.method
        }
    )
    
    return JSONResponse(
        status_code=500,
        content={
            "error": "internal_server_error",
            "message": "服务器内部错误" if settings.ENVIRONMENT != "dev" else str(exc),
            "status_code": 500,
            "path": request.url.path,
            "request_id": request_id,
            "timestamp": datetime.datetime.utcnow().isoformat() + "Z"
        }
    )

# 添加请求和响应中间件
@app.middleware("http")
async def log_requests(request: Request, call_next):
    """请求日志中间件"""
    # 生成或获取请求ID
    request_id = request.headers.get("X-Request-ID", str(uuid.uuid4()))
    request.state.request_id = request_id
    
    # 记录请求开始
    start_time = datetime.datetime.utcnow()
    client_ip = request.client.host if request.client else "unknown"
    
    # 提取用户信息（如果已认证）
    user_id = "-"
    try:
        if hasattr(request.state, "user") and request.state.user:
            user_id = str(request.state.user.id)
    except Exception:
        pass
    
    info(
        logger,
        f"请求开始: {request.method} {request.url.path}",
        request_id=request_id,
        user_id=user_id,
        ip=client_ip,
        extra={
            "method": request.method,
            "path": request.url.path,
            "query_params": dict(request.query_params)
        }
    )
    
    # 处理请求
    try:
        response = await call_next(request)
        
        # 记录请求结束
        process_time = (datetime.datetime.utcnow() - start_time).total_seconds() * 1000
        
        if response.status_code >= 500:
            error(
                logger,
                f"请求结束: {request.method} {request.url.path} {response.status_code}",
                request_id=request_id,
                user_id=user_id,
                ip=client_ip,
                extra={
                    "status_code": response.status_code,
                    "process_time_ms": round(process_time, 2)
                }
            )
        elif response.status_code >= 400:
            warning(
                logger,
                f"请求结束: {request.method} {request.url.path} {response.status_code}",
                request_id=request_id,
                user_id=user_id,
                ip=client_ip,
                extra={
                    "status_code": response.status_code,
                    "process_time_ms": round(process_time, 2)
                }
            )
        else:
            info(
                logger,
                f"请求结束: {request.method} {request.url.path} {response.status_code}",
                request_id=request_id,
                user_id=user_id,
                ip=client_ip,
                extra={
                    "status_code": response.status_code,
                    "process_time_ms": round(process_time, 2)
                }
            )
        
        # 添加请求ID到响应头
        response.headers["X-Request-ID"] = request_id
        response.headers["X-Process-Time"] = str(process_time / 1000)
        return response
    
    except Exception as e:
        # 记录未捕获的异常
        process_time = (datetime.datetime.utcnow() - start_time).total_seconds() * 1000
        error(
            logger,
            f"请求异常: {request.method} {request.url.path} - {str(e)}",
            request_id=request_id,
            user_id=user_id,
            ip=client_ip,
            extra={
                "error": str(e),
                "process_time_ms": round(process_time, 2)
            }
        )
        
        # 返回500错误
        return JSONResponse(
            status_code=500,
            content={
                "error": "internal_server_error",
                "message": "服务器内部错误",
                "request_id": request_id,
                "path": request.url.path,
                "timestamp": datetime.datetime.utcnow().isoformat() + "Z"
            }
        )

if __name__ == "__main__":
    info(logger, "直接运行应用")
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=True
    )

