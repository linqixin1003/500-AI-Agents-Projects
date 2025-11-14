"""应用配置模块"""
from pydantic_settings import BaseSettings
import os
from pathlib import Path
from functools import lru_cache

# 获取项目根目录
BASE_DIR = Path(__file__).resolve().parent.parent


class Settings(BaseSettings):
    """应用配置类"""
    # 应用配置
    api_version: str = "1.0.0"
    debug: bool = True
    
    # 数据库配置
    DATABASE_URL: str = "postgresql+asyncpg://admin:password@localhost:5432/example_db"
    
    # AI模型配置
    OPENAI_API_KEY: str = ""
    DASHSCOPE_API_KEY: str = ""  # 通义千问
    ai_service_url: str = ""
    ai_service_api_key: str = ""
    FOOD_CLASSIFIER_PROVIDER: str = "qwen"
    
    # 环境配置
    ENVIRONMENT: str = "dev"
    HOST: str = "localhost:8000"
    
    # 静态资源配置
    STATIC_ASSETS_URL: str = ""
    
    # 存储配置
    storage_type: str = "local"  # local 或 s3
    local_storage_path: str = "./uploads"
    
    # AWS 配置（可选，用于生产环境）
    AWS_ACCESS_KEY_ID: str = ""
    AWS_SECRET_ACCESS_KEY: str = ""
    AWS_REGION: str = ""
    AWS_S3_BUCKET: str = ""
    S3_URL: str = ""
    
    # Firebase 配置（可选，用于 FCM 推送）
    FIREBASE_CREDENTIALS_PATH: str = ""
    
    # JWT配置
    SECRET_KEY: str = "development-secret-key-change-in-production"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30  # 修改为更合理的默认值
    
    # FastMCP 配置
    FASTMCP_URL: str = "http://localhost:8001"
    FASTMCP_API_KEY: str = ""
    MCP_ENABLED: bool = True  # 是否启用MCP集成
    MCP_TIMEOUT: int = 30  # MCP请求超时时间（秒）
    MCP_RETRY_COUNT: int = 3  # MCP请求重试次数
    
    # MCP配置
    mcp_api_key: str = ""
    mcp_service_url: str = "http://localhost:8080/api"
    
    # 限流配置
    rate_limit_per_minute: int = 60
    rate_limit_per_hour: int = 1000
    
    # CORS配置
    backend_cors_origins: list[str] = ["*"]
    
    class Config:
        env_file = os.getenv("ENV_FILE", ".env")
        env_file_encoding = 'utf-8'
        case_sensitive = False
        extra = 'ignore'
        

@lru_cache()
def get_settings() -> Settings:
    """
    获取配置实例（单例模式）
    使用lru_cache确保配置只被加载一次
    """
    return Settings()


# 创建全局配置实例
settings = get_settings()

