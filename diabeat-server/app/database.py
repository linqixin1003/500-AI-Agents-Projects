"""数据库连接和管理模块"""
from typing import AsyncGenerator, Optional
from databases import Database
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import declarative_base, sessionmaker
from app.config import settings
import aioredis
import os
import logging

logger = logging.getLogger(__name__)

# 数据库连接池配置
DB_POOL_SIZE = int(os.getenv("DB_POOL_SIZE", 10))
DB_MAX_OVERFLOW = int(os.getenv("DB_MAX_OVERFLOW", 5))

# 创建数据库实例，配置连接池
# 注意：asyncpg 不支持 pool_pre_ping 参数
database = Database(
    settings.DATABASE_URL,
    min_size=5,
    max_size=DB_POOL_SIZE + DB_MAX_OVERFLOW
)

# Redis缓存配置
REDIS_URL = os.getenv("REDIS_URL", "redis://redis:6379/0")
redis_pool: Optional[aioredis.ConnectionPool] = None

async def init_redis():
    """初始化Redis连接池"""
    global redis_pool
    try:
        redis_pool = aioredis.ConnectionPool.from_url(
            REDIS_URL,
            max_connections=20,
            decode_responses=True
        )
        logger.info("✅ Redis连接池初始化成功")
    except Exception as e:
        logger.warning(f"⚠️ Redis连接池初始化失败: {str(e)}")

async def get_redis():
    """获取Redis连接"""
    if not redis_pool:
        await init_redis()
    if redis_pool:
        return aioredis.Redis(connection_pool=redis_pool)
    return None

async def close_redis():
    """关闭Redis连接池"""
    global redis_pool
    if redis_pool:
        try:
            await redis_pool.disconnect()
            logger.info("✅ Redis连接池已关闭")
        except Exception as e:
            logger.warning(f"⚠️ Redis连接池关闭失败: {str(e)}")

# SQLAlchemy配置
engine = create_async_engine(
    settings.DATABASE_URL,
    echo=settings.debug,  # 在开发环境打印SQL语句
    future=True,
    pool_size=DB_POOL_SIZE,
    max_overflow=DB_MAX_OVERFLOW,
    pool_recycle=3600,  # 连接回收时间（秒）
    pool_timeout=30,    # 连接超时时间（秒）
)

AsyncSessionLocal = sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autocommit=False,
    autoflush=False,
)

# 创建基类
Base = declarative_base()


async def connect_db():
    """连接数据库"""
    await database.connect()
    print("数据库连接成功")


async def disconnect_db():
    """断开数据库连接"""
    await database.disconnect()
    # 关闭Redis连接池
    await close_redis()
    print("数据库连接已关闭")


async def get_database() -> Database:
    """获取数据库连接"""
    return database


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """
    获取SQLAlchemy异步会话
    用于ORM操作
    """
    async with AsyncSessionLocal() as session:
        try:
            yield session
        finally:
            await session.close()


async def execute_query(query, values=None, cache_key=None, cache_ttl=300):
    """
    执行SQL查询的通用函数，支持结果缓存
    
    Args:
        query: SQL查询语句
        values: 查询参数
        cache_key: 缓存键（可选）
        cache_ttl: 缓存过期时间（秒，默认5分钟）
        
    Returns:
        查询结果
    """
    # 如果提供了缓存键，尝试从缓存获取
    if cache_key:
        redis = await get_redis()
        if redis:
            try:
                cached_result = await redis.get(cache_key)
                if cached_result:
                    import json
                    logger.debug(f"从缓存获取查询结果: {cache_key}")
                    return json.loads(cached_result)
            except Exception as e:
                logger.warning(f"缓存读取失败: {str(e)}")
    
    # 执行数据库查询
    db = await get_database()
    if values:
        result = await db.execute(query, values)
    else:
        result = await db.execute(query)
    
    # 如果提供了缓存键，将结果存入缓存
    if cache_key:
        redis = await get_redis()
        if redis:
            try:
                import json
                # 将查询结果转换为可序列化的格式
                # 注意：这取决于查询结果的类型，可能需要根据实际情况调整
                if hasattr(result, '_mapping'):
                    # 单条记录
                    serializable_result = dict(result._mapping)
                elif hasattr(result, 'all'):
                    # 多条记录
                    serializable_result = [dict(row._mapping) for row in result.all()]
                else:
                    serializable_result = result
                
                await redis.setex(
                    cache_key,
                    cache_ttl,
                    json.dumps(serializable_result, default=str)
                )
                logger.debug(f"查询结果已缓存: {cache_key}, TTL: {cache_ttl}秒")
            except Exception as e:
                logger.warning(f"缓存写入失败: {str(e)}")
    
    return result


async def transaction(func):
    """
    事务装饰器，用于包装需要在事务中执行的函数
    
    Args:
        func: 要在事务中执行的函数
        
    Returns:
        包装后的函数
    """
    async def wrapper(*args, **kwargs):
        db = await get_database()
        async with db.transaction():
            return await func(*args, **kwargs)
    return wrapper

