"""性能指标收集模块，使用Prometheus收集和暴露应用性能数据"""

from fastapi import FastAPI, Request, Response
from prometheus_client import Counter, Histogram, Gauge, Summary, generate_latest, CONTENT_TYPE_LATEST
import time
from functools import wraps
from typing import Callable, Any, Optional
from app.utils.logger import get_logger

logger = get_logger(__name__)

# 创建Prometheus指标
REQUEST_COUNT = Counter(
    'fastapi_requests_total',
    'Total HTTP Requests',
    ['method', 'endpoint', 'status_code']
)

REQUEST_LATENCY = Histogram(
    'fastapi_request_duration_seconds',
    'HTTP Request latency in seconds',
    ['method', 'endpoint']
)

ACTIVE_REQUESTS = Gauge(
    'fastapi_active_requests',
    'Active HTTP requests'
)

MEMORY_USAGE = Gauge(
    'diabeat_memory_usage_bytes',
    'Memory usage in bytes'
)

CPU_USAGE = Gauge(
    'diabeat_cpu_usage_percent',
    'CPU usage percentage'
)

DB_CONNECTION_POOL_SIZE = Gauge(
    'diabeat_db_connection_pool_size',
    'Database connection pool size'
)

DB_CONNECTION_POOL_USED = Gauge(
    'diabeat_db_connection_pool_used',
    'Number of used connections in the database pool'
)

CACHE_HIT_RATIO = Summary(
    'diabeat_cache_hit_ratio',
    'Cache hit ratio'
)

# 指标中间件配置
_metrics_app: Optional[FastAPI] = None


def setup_metrics(app: FastAPI) -> None:
    """
    配置Prometheus指标收集中间件
    
    Args:
        app: FastAPI应用实例
    """
    global _metrics_app
    _metrics_app = app
    
    # 添加请求指标中间件
    @app.middleware("http")
    async def metrics_middleware(request: Request, call_next):
        # 增加活动请求计数
        ACTIVE_REQUESTS.inc()
        
        # 获取端点路径
        path = request.scope.get("path", "unknown")
        
        # 记录开始时间
        start_time = time.time()
        
        try:
            # 处理请求
            response = await call_next(request)
        except Exception as e:
            # 记录异常请求
            REQUEST_COUNT.labels(
                method=request.method,
                endpoint=path,
                status_code=500
            ).inc()
            raise e
        finally:
            # 计算请求延迟
            REQUEST_LATENCY.labels(
                method=request.method,
                endpoint=path
            ).observe(time.time() - start_time)
            
            # 减少活动请求计数
            ACTIVE_REQUESTS.dec()
        
        # 记录请求计数
        REQUEST_COUNT.labels(
            method=request.method,
            endpoint=path,
            status_code=response.status_code
        ).inc()
        
        return response
    
    # 添加Prometheus指标端点
    @app.get("/metrics", tags=["monitoring"])
    async def metrics_endpoint():
        """Prometheus指标端点"""
        return Response(
            content=generate_latest(),
            media_type=CONTENT_TYPE_LATEST
        )
    
    logger.info("性能指标收集中间件已配置")


def get_metrics_app() -> Optional[FastAPI]:
    """
    获取配置了指标的应用实例
    
    Returns:
        配置了指标的FastAPI应用实例，如果未配置则返回None
    """
    return _metrics_app


def metrics_decorator(metric_name: str, metric_description: str = ""):
    """
    用于记录函数执行时间的装饰器
    
    Args:
        metric_name: 指标名称
        metric_description: 指标描述
    """
    # 为每个装饰器创建独立的Histogram
    function_latency = Histogram(
        f"function_{metric_name}_duration_seconds",
        metric_description or f"Time spent in {metric_name} function"
    )
    
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        async def async_wrapper(*args, **kwargs) -> Any:
            with function_latency.time():
                return await func(*args, **kwargs)
        
        @wraps(func)
        def sync_wrapper(*args, **kwargs) -> Any:
            with function_latency.time():
                return func(*args, **kwargs)
        
        # 根据函数类型返回适当的包装器
        if hasattr(func, "__await__"):
            return async_wrapper
        return sync_wrapper
    
    return decorator
