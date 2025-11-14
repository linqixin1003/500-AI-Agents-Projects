"""健康检查模块，提供全面的系统健康状态检查"""

from fastapi import APIRouter, Depends, HTTPException
from typing import Dict, Any, List, Optional
import asyncio
import time
import psutil
from app.database import database, get_redis
from app.utils.logger import get_logger, info, warning, error
from app.monitoring.metrics import ACTIVE_REQUESTS, DB_CONNECTION_POOL_SIZE, DB_CONNECTION_POOL_USED

logger = get_logger(__name__)

# 创建健康检查路由器
health_check_router = APIRouter(prefix="/health", tags=["health"])


class HealthCheckResult:
    """健康检查结果类"""
    def __init__(self, component: str, status: str, message: Optional[str] = None, details: Optional[Dict[str, Any]] = None):
        self.component = component
        self.status = status  # "healthy", "degraded", "unhealthy"
        self.message = message
        self.details = details or {}
        self.timestamp = time.time()
        
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典格式"""
        result = {
            "component": self.component,
            "status": self.status,
            "timestamp": self.timestamp
        }
        if self.message:
            result["message"] = self.message
        if self.details:
            result["details"] = self.details
        return result


class HealthCheckService:
    """
    健康检查服务，提供系统各组件的健康状态检查功能
    
    支持检查的组件：
    - 系统资源（CPU、内存）
    - 数据库连接
    - Redis缓存
    - 应用状态
    - 外部服务连接
    """
    
    def __init__(self):
        self.health_checkers = {
            "system": self.check_system_resources,
            "database": self.check_database,
            "redis": self.check_redis,
            "application": self.check_application
        }
        self.external_service_checkers: Dict[str, callable] = {}
    
    async def check_system_resources(self) -> HealthCheckResult:
        """
        检查系统资源使用情况
        
        Returns:
            健康检查结果，包含CPU和内存使用情况
        """
        try:
            # 获取系统资源使用情况
            cpu_percent = psutil.cpu_percent(interval=0.1)
            memory = psutil.virtual_memory()
            memory_percent = memory.percent
            memory_used = memory.used
            
            # 定义资源阈值
            cpu_threshold = 90.0  # CPU使用率阈值
            memory_threshold = 90.0  # 内存使用率阈值
            
            # 确定状态
            status = "healthy"
            if cpu_percent > cpu_threshold or memory_percent > memory_threshold:
                status = "degraded"
            
            details = {
                "cpu_percent": cpu_percent,
                "memory_percent": memory_percent,
                "memory_used_bytes": memory_used,
                "memory_total_bytes": memory.total,
                "process_count": len(psutil.pids())
            }
            
            return HealthCheckResult(
                component="system",
                status=status,
                details=details
            )
        except Exception as e:
            return HealthCheckResult(
                component="system",
                status="degraded",
                message=f"无法获取系统资源信息: {str(e)}"
            )
    
    async def check_database(self) -> HealthCheckResult:
        """
        检查数据库连接状态
        
        Returns:
            健康检查结果，包含数据库连接状态
        """
        try:
            # 检查数据库连接
            if not database.is_connected:
                return HealthCheckResult(
                    component="database",
                    status="unhealthy",
                    message="数据库未连接"
                )
            
            # 执行简单查询测试连接
            start_time = time.time()
            await database.fetch_one("SELECT 1")
            query_time = (time.time() - start_time) * 1000  # 转换为毫秒
            
            # 更新数据库连接池指标
            try:
                # 获取数据库引擎信息（如果可用）
                if hasattr(database, "_engine") and database._engine:
                    # 注意：这部分可能需要根据实际使用的数据库引擎调整
                    # 这里仅作为示例，实际实现可能不同
                    pool_size = getattr(database._engine, "pool_size", 10)
                    DB_CONNECTION_POOL_SIZE.set(pool_size)
                    
                    # 尝试获取已使用连接数（取决于数据库引擎）
                    # 这是一个示例实现，可能需要根据实际情况调整
                    try:
                        used = getattr(database._engine.pool, "checkedout", 0)
                        DB_CONNECTION_POOL_USED.set(used)
                    except:
                        pass
            except Exception as e:
                warning(logger, f"更新数据库连接池指标失败: {str(e)}")
            
            # 定义查询时间阈值
            query_time_threshold = 500  # 毫秒
            
            # 确定状态
            status = "healthy"
            if query_time > query_time_threshold:
                status = "degraded"
                message = f"数据库查询响应缓慢: {query_time:.2f}ms"
            else:
                message = f"数据库连接正常，查询响应时间: {query_time:.2f}ms"
            
            details = {
                "connected": True,
                "query_response_time_ms": query_time
            }
            
            return HealthCheckResult(
                component="database",
                status=status,
                message=message,
                details=details
            )
        except Exception as e:
            return HealthCheckResult(
                component="database",
                status="unhealthy",
                message=f"数据库连接错误: {str(e)}"
            )
    
    async def check_redis(self) -> HealthCheckResult:
        """
        检查Redis缓存状态
        
        Returns:
            健康检查结果，包含Redis连接状态
        """
        try:
            redis = await get_redis()
            
            if not redis:
                return HealthCheckResult(
                    component="redis",
                    status="unhealthy",
                    message="Redis未初始化"
                )
            
            # 执行ping命令测试连接
            start_time = time.time()
            await redis.ping()
            ping_time = (time.time() - start_time) * 1000  # 转换为毫秒
            
            # 获取Redis信息（如果支持）
            try:
                info = await redis.info(section="stats")
                details = {
                    "ping_response_time_ms": ping_time,
                    "connected": True
                }
                if info:
                    details.update({
                        "total_connections_received": info.get("total_connections_received", 0),
                        "instantaneous_ops_per_sec": info.get("instantaneous_ops_per_sec", 0)
                    })
            except Exception:
                # 如果无法获取详细信息，返回基本状态
                details = {
                    "ping_response_time_ms": ping_time,
                    "connected": True
                }
            
            return HealthCheckResult(
                component="redis",
                status="healthy",
                message=f"Redis连接正常，响应时间: {ping_time:.2f}ms",
                details=details
            )
        except Exception as e:
            return HealthCheckResult(
                component="redis",
                status="unhealthy",
                message=f"Redis连接错误: {str(e)}"
            )
    
    async def check_application(self) -> HealthCheckResult:
        """
        检查应用状态
        
        Returns:
            健康检查结果，包含应用运行状态
        """
        try:
            # 获取活动请求数
            active_requests = ACTIVE_REQUESTS._value.get()
            
            # 获取应用启动时间（实际应用中可能需要调整）
            # 这里使用当前进程创建时间作为参考
            process = psutil.Process()
            uptime = time.time() - process.create_time()
            
            details = {
                "active_requests": active_requests,
                "uptime_seconds": uptime,
                "process_id": process.pid,
                "cpu_percent": process.cpu_percent(interval=0.1),
                "memory_used_bytes": process.memory_info().rss
            }
            
            return HealthCheckResult(
                component="application",
                status="healthy",
                details=details
            )
        except Exception as e:
            return HealthCheckResult(
                component="application",
                status="degraded",
                message=f"无法获取应用状态: {str(e)}"
            )
    
    def register_external_service_checker(self, service_name: str, checker: callable):
        """
        注册外部服务健康检查器
        
        Args:
            service_name: 服务名称
            checker: 检查函数，需要返回HealthCheckResult对象
        """
        self.external_service_checkers[service_name] = checker
        logger.info(f"已注册外部服务健康检查器: {service_name}")
    
    async def run_health_checks(self, components: Optional[List[str]] = None) -> Dict[str, Any]:
        """
        运行健康检查
        
        Args:
            components: 要检查的组件列表，如果为None则检查所有组件
        
        Returns:
            健康检查结果
        """
        try:
            # 确定要检查的组件
            if components:
                checkers = {k: v for k, v in self.health_checkers.items() if k in components}
                # 添加指定的外部服务检查器
                for service_name in components:
                    if service_name in self.external_service_checkers:
                        checkers[service_name] = self.external_service_checkers[service_name]
            else:
                # 检查所有组件
                checkers = {**self.health_checkers, **self.external_service_checkers}
            
            # 并发运行所有健康检查
            tasks = [checker() for checker in checkers.values()]
            results = await asyncio.gather(*tasks, return_exceptions=True)
            
            # 处理检查结果
            component_results = {}
            for i, (component_name, _) in enumerate(checkers.items()):
                result = results[i]
                if isinstance(result, Exception):
                    # 如果检查函数抛出异常，记录为失败
                    component_results[component_name] = {
                        "status": "unhealthy",
                        "message": f"检查失败: {str(result)}",
                        "timestamp": time.time()
                    }
                else:
                    # 正常处理检查结果
                    component_results[component_name] = result.to_dict()
            
            # 计算整体状态
            overall_status = "healthy"
            for result in component_results.values():
                if result["status"] == "unhealthy":
                    overall_status = "unhealthy"
                    break
                elif result["status"] == "degraded" and overall_status != "unhealthy":
                    overall_status = "degraded"
            
            # 构建完整的健康检查结果
            health_result = {
                "status": overall_status,
                "timestamp": time.time(),
                "components": component_results
            }
            
            # 记录健康检查结果
            info(
                logger,
                f"健康检查完成，整体状态: {overall_status}",
                extra={
                    "status": overall_status,
                    "component_count": len(component_results)
                }
            )
            
            return health_result
        except Exception as e:
            error(logger, f"运行健康检查时出错: {str(e)}")
            return {
                "status": "unhealthy",
                "timestamp": time.time(),
                "message": f"健康检查过程出错: {str(e)}"
            }


# 创建全局健康检查服务实例
health_check_service = HealthCheckService()


@health_check_router.get("/", response_model=Dict[str, Any])
async def health_check(components: Optional[str] = None):
    """
    健康检查端点
    
    Args:
        components: 要检查的组件，用逗号分隔，如 "database,redis"
    
    Returns:
        健康检查结果
    """
    # 解析组件列表
    component_list = None
    if components:
        component_list = [c.strip() for c in components.split(",")]
    
    # 运行健康检查
    result = await health_check_service.run_health_checks(component_list)
    
    # 根据整体状态设置HTTP状态码
    if result["status"] == "unhealthy":
        raise HTTPException(status_code=503, detail=result)
    elif result["status"] == "degraded":
        # 即使系统处于降级状态，也返回200，因为服务仍然可用
        pass
    
    return result


@health_check_router.get("/detailed", response_model=Dict[str, Any])
async def detailed_health_check():
    """
    详细健康检查端点，返回所有组件的详细状态信息
    
    Returns:
        详细的健康检查结果
    """
    return await health_check_service.run_health_checks()
