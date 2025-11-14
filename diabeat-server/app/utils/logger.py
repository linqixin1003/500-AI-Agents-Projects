"""日志管理模块

提供统一的日志配置和结构化日志记录功能
"""
import logging
import json
import os
import traceback
from datetime import datetime
from typing import Any, Dict, Optional, Union


class CustomFormatter(logging.Formatter):
    """自定义日志格式化器，支持结构化日志输出"""
    
    def __init__(self, fmt=None, datefmt=None, style='%', use_structured=False):
        super().__init__(fmt=fmt, datefmt=datefmt, style=style)
        self.use_structured = use_structured
        
        # 非结构化日志格式
        self.unstructured_fmt = fmt or '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        self.datefmt = datefmt or '%Y-%m-%d %H:%M:%S'
    
    def format(self, record):
        """格式化日志记录"""
        # 为日志添加请求ID等上下文信息
        if not hasattr(record, 'request_id'):
            record.request_id = "-"
        
        if not hasattr(record, 'user_id'):
            record.user_id = "-"
        
        if not hasattr(record, 'ip'):
            record.ip = "-"
        
        if self.use_structured:
            # 结构化日志格式
            log_data = {
                "timestamp": datetime.utcnow().isoformat() + "Z",
                "level": record.levelname,
                "name": record.name,
                "message": record.getMessage(),
                "request_id": record.request_id,
                "user_id": record.user_id,
                "ip": record.ip,
                "process_id": record.process,
                "thread_id": record.thread,
                "module": record.module,
                "line_no": record.lineno
            }
            
            # 如果是异常日志，添加异常信息
            if record.exc_info:
                log_data["exception"] = {
                    "type": record.exc_info[0].__name__ if record.exc_info[0] else None,
                    "message": str(record.exc_info[1]) if record.exc_info[1] else None,
                    "traceback": "".join(traceback.format_tb(record.exc_info[2])) if record.exc_info[2] else None
                }
            
            # 添加额外的自定义字段
            if hasattr(record, 'extra') and isinstance(record.extra, dict):
                log_data.update(record.extra)
            
            return json.dumps(log_data)
        else:
            # 非结构化日志
            self._style._fmt = self.unstructured_fmt
            return super().format(record)


def setup_logging(
    log_level: str = "INFO",
    log_file: Optional[str] = None,
    structured: bool = False,
    log_dir: str = "./logs"
) -> None:
    """设置日志配置
    
    Args:
        log_level: 日志级别 (DEBUG, INFO, WARNING, ERROR, CRITICAL)
        log_file: 日志文件路径，None表示只输出到控制台
        structured: 是否使用结构化日志格式
        log_dir: 日志目录
    """
    # 确保日志目录存在
    if log_file:
        os.makedirs(log_dir, exist_ok=True)
        log_file_path = os.path.join(log_dir, log_file)
    
    # 获取根logger
    root_logger = logging.getLogger()
    root_logger.setLevel(getattr(logging, log_level))
    
    # 清除现有的处理器
    for handler in root_logger.handlers[:]:
        root_logger.removeHandler(handler)
    
    # 创建控制台处理器
    console_handler = logging.StreamHandler()
    console_handler.setLevel(getattr(logging, log_level))
    
    # 创建格式化器
    if structured:
        formatter = CustomFormatter(use_structured=True)
    else:
        formatter = CustomFormatter(
            fmt='%(asctime)s - %(name)s - %(levelname)s - [%(request_id)s] [%(user_id)s] - %(message)s',
            use_structured=False
        )
    
    console_handler.setFormatter(formatter)
    root_logger.addHandler(console_handler)
    
    # 如果指定了日志文件，添加文件处理器
    if log_file:
        file_handler = logging.FileHandler(log_file_path, encoding='utf-8')
        file_handler.setLevel(getattr(logging, log_level))
        file_handler.setFormatter(formatter)
        root_logger.addHandler(file_handler)
    
    # 配置第三方库的日志级别
    logging.getLogger('uvicorn').setLevel(logging.WARNING)
    logging.getLogger('fastapi').setLevel(logging.WARNING)
    logging.getLogger('sqlalchemy').setLevel(logging.WARNING)
    logging.getLogger('asyncio').setLevel(logging.WARNING)


def get_logger(name: str) -> logging.Logger:
    """获取配置好的logger实例
    
    Args:
        name: logger名称，通常使用__name__
    
    Returns:
        配置好的logger实例
    """
    return logging.getLogger(name)


def log_with_context(
    logger: logging.Logger,
    level: int,
    message: str,
    request_id: Optional[str] = None,
    user_id: Optional[str] = None,
    ip: Optional[str] = None,
    extra: Optional[Dict[str, Any]] = None,
    **kwargs
) -> None:
    """带上下文的日志记录
    
    Args:
        logger: logger实例
        level: 日志级别
        message: 日志消息
        request_id: 请求ID
        user_id: 用户ID
        ip: IP地址
        extra: 额外的日志字段
        **kwargs: 其他参数
    """
    extra_dict = {}
    if request_id:
        extra_dict['request_id'] = request_id
    if user_id:
        extra_dict['user_id'] = user_id
    if ip:
        extra_dict['ip'] = ip
    if extra:
        extra_dict['extra'] = extra
    
    logger.log(level, message, extra=extra_dict, **kwargs)


def debug(
    logger: logging.Logger,
    message: str,
    request_id: Optional[str] = None,
    user_id: Optional[str] = None,
    ip: Optional[str] = None,
    extra: Optional[Dict[str, Any]] = None
) -> None:
    """记录debug级别日志"""
    log_with_context(logger, logging.DEBUG, message, request_id, user_id, ip, extra)


def info(
    logger: logging.Logger,
    message: str,
    request_id: Optional[str] = None,
    user_id: Optional[str] = None,
    ip: Optional[str] = None,
    extra: Optional[Dict[str, Any]] = None
) -> None:
    """记录info级别日志"""
    log_with_context(logger, logging.INFO, message, request_id, user_id, ip, extra)


def warning(
    logger: logging.Logger,
    message: str,
    request_id: Optional[str] = None,
    user_id: Optional[str] = None,
    ip: Optional[str] = None,
    extra: Optional[Dict[str, Any]] = None
) -> None:
    """记录warning级别日志"""
    log_with_context(logger, logging.WARNING, message, request_id, user_id, ip, extra)


def error(
    logger: logging.Logger,
    message: str,
    request_id: Optional[str] = None,
    user_id: Optional[str] = None,
    ip: Optional[str] = None,
    extra: Optional[Dict[str, Any]] = None,
    exc_info: bool = True
) -> None:
    """记录error级别日志"""
    log_with_context(logger, logging.ERROR, message, request_id, user_id, ip, extra, exc_info=exc_info)


def critical(
    logger: logging.Logger,
    message: str,
    request_id: Optional[str] = None,
    user_id: Optional[str] = None,
    ip: Optional[str] = None,
    extra: Optional[Dict[str, Any]] = None,
    exc_info: bool = True
) -> None:
    """记录critical级别日志"""
    log_with_context(logger, logging.CRITICAL, message, request_id, user_id, ip, extra, exc_info=exc_info)
