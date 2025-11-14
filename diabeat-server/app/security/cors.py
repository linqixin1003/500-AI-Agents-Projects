"""CORS配置模块，用于安全地处理跨域请求"""

from typing import List, Dict, Any, Optional
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.utils.logger import get_logger, info, warning

logger = get_logger(__name__)


def configure_cors(app: FastAPI, cors_settings: Optional[Dict[str, Any]] = None):
    """
    配置CORS中间件
    
    Args:
        app: FastAPI应用实例
        cors_settings: CORS配置字典，如果为None则使用默认配置
    """
    # 默认CORS配置
    default_settings = {
        "allow_origins": ["*"],  # 默认允许所有来源，生产环境应该更严格
        "allow_credentials": True,
        "allow_methods": ["*"],  # 默认允许所有HTTP方法
        "allow_headers": ["*"],  # 默认允许所有请求头
    }
    
    # 合并用户配置和默认配置
    settings = {**default_settings, **(cors_settings or {})}
    
    # 添加CORS中间件
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings["allow_origins"],
        allow_credentials=settings["allow_credentials"],
        allow_methods=settings["allow_methods"],
        allow_headers=settings["allow_headers"],
    )
    
    # 记录配置信息
    info(
        logger,
        "CORS中间件已配置",
        extra={
            "allow_origins": settings["allow_origins"][:5] + ("..." if len(settings["allow_origins"]) > 5 else []),
            "allow_methods": settings["allow_methods"],
            "allow_headers": settings["allow_headers"],
        }
    )


def create_cors_config_for_environment(environment: str) -> Dict[str, Any]:
    """
    根据环境创建适当的CORS配置
    
    Args:
        environment: 环境名称 (development, staging, production)
    
    Returns:
        CORS配置字典
    """
    # 开发环境配置
    if environment == "development":
        return {
            "allow_origins": ["*"],  # 开发环境允许所有来源
            "allow_credentials": True,
            "allow_methods": ["*"],
            "allow_headers": ["*"],
        }
    # 测试环境配置
    elif environment == "staging":
        return {
            "allow_origins": ["*"],  # 测试环境可以允许所有来源
            "allow_credentials": True,
            "allow_methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
            "allow_headers": ["*"],
        }
    # 生产环境配置
    elif environment == "production":
        return {
            "allow_origins": [
                # 这里应该配置具体的前端域名，而不是使用通配符
                "https://your-production-domain.com",
                "https://api.your-production-domain.com",
                # 可以添加多个允许的源
            ],
            "allow_credentials": True,
            "allow_methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
            "allow_headers": [
                "Content-Type",
                "Authorization",
                "Accept",
                "X-Requested-With",
                "X-API-Key",
            ],
        }
    # 默认配置
    else:
        return {
            "allow_origins": ["*"],
            "allow_credentials": True,
            "allow_methods": ["*"],
            "allow_headers": ["*"],
        }


def validate_cors_origins(origins: List[str]) -> bool:
    """
    验证CORS来源列表的安全性
    
    Args:
        origins: 允许的来源列表
    
    Returns:
        是否安全
    """
    # 检查是否在生产环境中使用通配符
    # 在生产环境中使用通配符是不安全的
    if "*" in origins:
        warning_message = "在CORS配置中使用通配符(*)可能会导致安全风险，建议在生产环境中指定具体的来源"
        logger.warning(warning_message)
        return False
    
    # 检查是否有不安全的来源
    for origin in origins:
        # 不允许使用IP地址，应该使用域名
        if origin.startswith("http://"):
            warning_message = f"在CORS配置中使用HTTP协议可能会导致安全风险，建议使用HTTPS: {origin}"
            logger.warning(warning_message)
            return False
    
    return True


def get_safe_origins(environment: str, origins: Optional[List[str]] = None) -> List[str]:
    """
    获取安全的CORS来源列表
    
    Args:
        environment: 环境名称
        origins: 原始来源列表
    
    Returns:
        安全的来源列表
    """
    # 如果没有提供来源列表，根据环境返回默认列表
    if not origins:
        config = create_cors_config_for_environment(environment)
        return config["allow_origins"]
    
    # 如果是开发环境，可以使用提供的列表
    if environment in ["development", "staging"]:
        return origins
    
    # 生产环境确保安全性
    if environment == "production":
        # 移除通配符和不安全的来源
        safe_origins = []
        for origin in origins:
            # 确保使用HTTPS
            if origin.startswith("https://"):
                safe_origins.append(origin)
        
        # 如果没有安全的来源，记录警告并使用默认配置
        if not safe_origins:
            logger.warning("提供的CORS来源列表中没有安全的来源，将使用默认配置")
            config = create_cors_config_for_environment(environment)
            return config["allow_origins"]
        
        return safe_origins
    
    # 默认返回提供的列表
    return origins


def update_cors_origins(app: FastAPI, new_origins: List[str]):
    """
    动态更新CORS来源列表
    
    Args:
        app: FastAPI应用实例
        new_origins: 新的来源列表
    """
    # 查找CORS中间件
    for middleware in app.user_middleware:
        if middleware.cls.__name__ == "CORSMiddleware":
            # 更新来源列表
            middleware.options["allow_origins"] = new_origins
            logger.info(f"CORS来源列表已更新: {new_origins}")
            return True
    
    logger.warning("未找到CORS中间件，无法更新来源列表")
    return False