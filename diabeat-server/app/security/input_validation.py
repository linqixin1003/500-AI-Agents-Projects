"""输入验证模块，提供输入清理和格式验证功能，防止XSS和其他注入攻击"""

import re
import html
from typing import Optional, Union, Any


def sanitize_input(text: Optional[str]) -> Optional[str]:
    """
    清理输入文本，防止XSS攻击
    
    Args:
        text: 要清理的文本
    
    Returns:
        清理后的文本
    """
    if text is None:
        return None
    
    # 转义HTML特殊字符
    sanitized = html.escape(text)
    
    # 移除潜在的危险字符序列
    # 移除JavaScript事件处理器
    sanitized = re.sub(r'\bon\w+\s*=', '', sanitized)
    
    # 移除script标签
    sanitized = re.sub(r'<\s*script[^>]*>', '', sanitized)
    sanitized = re.sub(r'<\s*/\s*script\s*>', '', sanitized)
    
    # 移除iframe标签
    sanitized = re.sub(r'<\s*iframe[^>]*>', '', sanitized)
    sanitized = re.sub(r'<\s*/\s*iframe\s*>', '', sanitized)
    
    # 移除object和embed标签（潜在的Flash等危险内容）
    sanitized = re.sub(r'<\s*object[^>]*>', '', sanitized)
    sanitized = re.sub(r'<\s*/\s*object\s*>', '', sanitized)
    sanitized = re.sub(r'<\s*embed[^>]*>', '', sanitized)
    
    return sanitized


def validate_email(email: str) -> bool:
    """
    验证电子邮件格式是否正确
    
    Args:
        email: 要验证的电子邮件地址
    
    Returns:
        电子邮件格式是否正确
    """
    if not email or not isinstance(email, str):
        return False
    
    # 使用更严格的电子邮件正则表达式
    email_pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    return bool(re.match(email_pattern, email))


def validate_phone_number(phone: str, country_code: str = 'CN') -> bool:
    """
    验证电话号码格式是否正确
    
    Args:
        phone: 要验证的电话号码
        country_code: 国家代码，默认为中国
    
    Returns:
        电话号码格式是否正确
    """
    if not phone or not isinstance(phone, str):
        return False
    
    # 移除所有非数字字符
    digits = re.sub(r'\D', '', phone)
    
    if country_code == 'CN':
        # 中国大陆电话号码验证
        # 手机号：1开头的11位数字
        # 固话：区号(3-4位)+号码(7-8位)，总共10-12位数字
        return bool(re.match(r'^1\d{10}$', digits)) or bool(re.match(r'^\d{10,12}$', digits))
    elif country_code == 'US':
        # 美国电话号码验证：10位数字
        return bool(re.match(r'^\d{10}$', digits))
    else:
        # 默认验证：8-15位数字
        return 8 <= len(digits) <= 15


def validate_input_format(input_data: Any, input_type: str, **kwargs) -> bool:
    """
    验证输入格式是否符合要求
    
    Args:
        input_data: 要验证的输入数据
        input_type: 输入类型
        **kwargs: 额外的验证参数
    
    Returns:
        输入格式是否符合要求
    """
    if input_type == 'email':
        return validate_email(str(input_data))
    elif input_type == 'phone':
        country_code = kwargs.get('country_code', 'CN')
        return validate_phone_number(str(input_data), country_code)
    elif input_type == 'numeric':
        # 验证是否为数字
        try:
            float(input_data)
            return True
        except (ValueError, TypeError):
            return False
    elif input_type == 'alphanumeric':
        # 验证是否为字母数字
        return bool(re.match(r'^[a-zA-Z0-9]+$', str(input_data)))
    elif input_type == 'alpha':
        # 验证是否仅包含字母
        return bool(re.match(r'^[a-zA-Z]+$', str(input_data)))
    elif input_type == 'length':
        # 验证长度是否在指定范围内
        min_length = kwargs.get('min_length', 0)
        max_length = kwargs.get('max_length', float('inf'))
        return min_length <= len(str(input_data)) <= max_length
    elif input_type == 'custom_regex':
        # 使用自定义正则表达式验证
        regex = kwargs.get('regex', '')
        return bool(re.match(regex, str(input_data)))
    
    return False


def sanitize_dict_values(data: dict) -> dict:
    """
    递归清理字典中的所有字符串值
    
    Args:
        data: 要清理的字典
    
    Returns:
        清理后的字典
    """
    if not isinstance(data, dict):
        return data
    
    sanitized = {}
    for key, value in data.items():
        if isinstance(value, str):
            sanitized[key] = sanitize_input(value)
        elif isinstance(value, dict):
            sanitized[key] = sanitize_dict_values(value)
        elif isinstance(value, list):
            sanitized[key] = [
                sanitize_input(item) if isinstance(item, str) else item
                for item in value
            ]
        else:
            sanitized[key] = value
    
    return sanitized


def validate_url(url: str) -> bool:
    """
    验证URL格式是否正确
    
    Args:
        url: 要验证的URL
    
    Returns:
        URL格式是否正确
    """
    if not url or not isinstance(url, str):
        return False
    
    # 使用正则表达式验证URL格式
    url_pattern = r'^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([/\w .-]*)*\/?$'
    return bool(re.match(url_pattern, url))