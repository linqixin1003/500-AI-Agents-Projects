"""速率限制模块，用于防止暴力攻击和DoS攻击"""

import time
import functools
from typing import Dict, Optional, Callable, Union
from fastapi import HTTPException, Request, Depends, FastAPI
from fastapi.responses import JSONResponse
from app.database import get_redis
from app.utils.logger import get_logger, warning

logger = get_logger(__name__)


class RateLimiter:
    """
    速率限制器类
    
    使用Redis实现基于IP或用户的速率限制
    """
    
    def __init__(self,
                 redis_client=None,
                 default_limit: int = 100,
                 default_period: int = 60):
        """
        初始化速率限制器
        
        Args:
            redis_client: Redis客户端实例
            default_limit: 默认的请求限制数
            default_period: 默认的时间窗口（秒）
        """
        self.redis_client = redis_client
        self.default_limit = default_limit
        self.default_period = default_period
    
    async def get_redis_client(self):
        """
        获取Redis客户端
        
        Returns:
            Redis客户端实例
        """
        if self.redis_client is None:
            self.redis_client = await get_redis()
        return self.redis_client
    
    async def _get_key(self, request: Request, identifier_type: str = 'ip') -> str:
        """
        获取速率限制的键
        
        Args:
            request: FastAPI请求对象
            identifier_type: 标识符类型，'ip'或'user'
        
        Returns:
            Redis键
        """
        if identifier_type == 'ip':
            # 使用IP地址作为标识符
            client_ip = request.client.host if request.client else 'unknown'
            identifier = client_ip
        elif identifier_type == 'user':
            # 使用用户ID作为标识符
            # 假设用户信息存储在request.state.user中
            # 这需要在认证中间件中设置
            user = getattr(request.state, 'user', None)
            if user and hasattr(user, 'id'):
                identifier = f"user:{user.id}"
            else:
                # 如果没有用户信息，回退到使用IP
                client_ip = request.client.host if request.client else 'unknown'
                identifier = client_ip
        else:
            raise ValueError("identifier_type必须是'ip'或'user'")
        
        # 获取路径
        path = request.url.path
        
        return f"rate_limit:{identifier}:{path}"
    
    async def check_rate_limit(
        self,
        request: Request,
        limit: Optional[int] = None,
        period: Optional[int] = None,
        identifier_type: str = 'ip'
    ) -> bool:
        """
        检查是否超出速率限制
        
        Args:
            request: FastAPI请求对象
            limit: 请求限制数
            period: 时间窗口（秒）
            identifier_type: 标识符类型，'ip'或'user'
        
        Returns:
            是否允许请求
        """
        # 使用默认值或提供的值
        limit = limit or self.default_limit
        period = period or self.default_period
        
        # 获取Redis客户端
        redis_client = await self.get_redis_client()
        
        # 如果Redis不可用，跳过速率限制检查
        if not redis_client:
            warning(logger, "Redis客户端不可用，跳过速率限制检查")
            return True
        
        try:
            # 获取速率限制键
            key = await self._get_key(request, identifier_type)
            
            # 获取当前时间
            current_time = int(time.time())
            
            # 使用Redis的有序集合来存储请求时间戳
            # 添加当前请求的时间戳
            await redis_client.zadd(key, {str(current_time): current_time})
            
            # 移除过期的请求记录
            await redis_client.zremrangebyscore(key, 0, current_time - period)
            
            # 设置键的过期时间，避免内存泄漏
            await redis_client.expire(key, period)
            
            # 获取时间窗口内的请求数
            request_count = await redis_client.zcard(key)
            
            # 检查是否超出限制
            if request_count > limit:
                warning(
                    logger,
                    "速率限制触发",
                    extra={
                        "identifier": key,
                        "request_count": request_count,
                        "limit": limit,
                        "period": period
                    }
                )
                return False
            
            return True
        except Exception as e:
            warning(logger, f"检查速率限制时出错: {str(e)}")
            # 出错时允许请求通过，避免Redis故障导致服务不可用
            return True
    
    def limit(
        self,
        limit: Optional[int] = None,
        period: Optional[int] = None,
        identifier_type: str = 'ip',
        error_message: str = "请求过于频繁，请稍后再试"
    ):
        """
        速率限制装饰器
        
        Args:
            limit: 请求限制数
            period: 时间窗口（秒）
            identifier_type: 标识符类型，'ip'或'user'
            error_message: 超出限制时的错误消息
        
        Returns:
            装饰器函数
        """
        def decorator(func: Callable) -> Callable:
            @functools.wraps(func)
            async def wrapper(request: Request, *args, **kwargs):
                # 检查速率限制
                is_allowed = await self.check_rate_limit(
                    request,
                    limit,
                    period,
                    identifier_type
                )
                
                if not is_allowed:
                    # 超出限制，返回429错误
                    raise HTTPException(
                        status_code=429,
                        detail={
                            "message": error_message,
                            "error_code": "RATE_LIMIT_EXCEEDED"
                        }
                    )
                
                # 允许请求继续处理
                return await func(request, *args, **kwargs)
            
            return wrapper
        
        return decorator


# 创建全局速率限制器实例
rate_limiter = RateLimiter()


def rate_limit(
    limit: Optional[int] = None,
    period: Optional[int] = None,
    identifier_type: str = 'ip',
    error_message: str = "请求过于频繁，请稍后再试"
) -> Callable:
    """
    速率限制依赖函数
    
    Args:
        limit: 请求限制数
        period: 时间窗口（秒）
        identifier_type: 标识符类型，'ip'或'user'
        error_message: 超出限制时的错误消息
    
    Returns:
        依赖函数
    """
    async def rate_limit_dependency(request: Request):
        # 检查速率限制
        is_allowed = await rate_limiter.check_rate_limit(
            request,
            limit,
            period,
            identifier_type
        )
        
        if not is_allowed:
            # 超出限制，返回429错误
            raise HTTPException(
                status_code=429,
                detail={
                    "message": error_message,
                    "error_code": "RATE_LIMIT_EXCEEDED"
                }
            )
        
        return None
    
    return rate_limit_dependency


def create_route_rate_limit(
    limit: int,
    period: int,
    path_pattern: str,
    identifier_type: str = 'ip'
) -> Dict[str, Union[int, str]]:
    """
    创建路由级别的速率限制配置
    
    Args:
        limit: 请求限制数
        period: 时间窗口（秒）
        path_pattern: 路径模式
        identifier_type: 标识符类型，'ip'或'user'
    
    Returns:
        速率限制配置
    """
    return {
        "limit": limit,
        "period": period,
        "path_pattern": path_pattern,
        "identifier_type": identifier_type
    }


# 常用的速率限制配置
DEFAULT_RATE_LIMIT = create_route_rate_limit(100, 60, ".*")  # 每分钟100个请求
STRICT_RATE_LIMIT = create_route_rate_limit(10, 60, "/api/auth/.*")  # 认证接口每分钟10个请求
API_RATE_LIMIT = create_route_rate_limit(50, 60, "/api/.*")  # API接口每分钟50个请求


async def rate_limiter_middleware(request: Request, call_next):
    """
    全局速率限制中间件
    
    Args:
        request: FastAPI请求对象
        call_next: 下一个处理函数
    
    Returns:
        响应对象
    """
    path = request.url.path
    
    # 根据路径应用不同的速率限制规则
    if path.startswith("/api/auth/"):
        # 认证接口使用更严格的限制
        limit = STRICT_RATE_LIMIT["limit"]
        period = STRICT_RATE_LIMIT["period"]
    elif path.startswith("/api/"):
        # API接口使用中等限制
        limit = API_RATE_LIMIT["limit"]
        period = API_RATE_LIMIT["period"]
    else:
        # 其他接口使用默认限制
        limit = DEFAULT_RATE_LIMIT["limit"]
        period = DEFAULT_RATE_LIMIT["period"]
    
    # 健康检查和指标端点不限制
    if path.startswith("/health") or path == "/metrics":
        return await call_next(request)
    
    # 检查速率限制
    is_allowed = await rate_limiter.check_rate_limit(
        request,
        limit=limit,
        period=period,
        identifier_type="ip"
    )
    
    if not is_allowed:
        # 超出限制，返回429错误
        return JSONResponse(
            status_code=429,
            content={
                "message": "请求过于频繁，请稍后再试",
                "error_code": "RATE_LIMIT_EXCEEDED",
                "limit": limit,
                "period": period
            }
        )
    
    # 允许请求继续处理
    response = await call_next(request)
    
    # 添加速率限制信息到响应头
    response.headers["X-RateLimit-Limit"] = str(limit)
    response.headers["X-RateLimit-Period"] = str(period)
    
    return response


def apply_rate_limits(app: FastAPI):
    """
    应用速率限制到所有路由
    
    Args:
        app: FastAPI应用实例
    """
    # 为所有路由添加默认速率限制
    for route in app.routes:
        # 跳过健康检查和指标路由
        if route.path.startswith("/health") or route.path == "/metrics":
            continue
        
        # 获取路由的路径
        path = route.path
        
        # 根据路径设置不同的速率限制
        if path.startswith("/api/auth/"):
            # 认证接口使用严格限制
            route.dependencies.append(Depends(rate_limit(
                limit=STRICT_RATE_LIMIT["limit"],
                period=STRICT_RATE_LIMIT["period"],
                identifier_type="ip"
            )))
        elif path.startswith("/api/"):
            # API接口使用中等限制
            route.dependencies.append(Depends(rate_limit(
                limit=API_RATE_LIMIT["limit"],
                period=API_RATE_LIMIT["period"],
                identifier_type="ip"
            )))
        else:
            # 其他接口使用默认限制
            route.dependencies.append(Depends(rate_limit(
                limit=DEFAULT_RATE_LIMIT["limit"],
                period=DEFAULT_RATE_LIMIT["period"],
                identifier_type="ip"
            )))
    
    logger.info("速率限制已应用到所有路由")