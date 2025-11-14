"""密码处理模块，提供密码哈希、验证和强度检查功能"""

import re
import bcrypt
from typing import Tuple, Optional


class PasswordStrengthError(Exception):
    """密码强度不足异常"""
    def __init__(self, message: str, issues: Optional[list] = None):
        self.message = message
        self.issues = issues or []
        super().__init__(message)


def get_password_hash(password: str) -> str:
    """
    对密码进行哈希处理
    
    Args:
        password: 原始密码
    
    Returns:
        哈希后的密码字符串
    """
    # 生成盐并哈希密码
    salt = bcrypt.gensalt(rounds=12)  # 增加工作因子以提高安全性
    hashed = bcrypt.hashpw(password.encode('utf-8'), salt)
    return hashed.decode('utf-8')


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """
    验证密码是否匹配
    
    Args:
        plain_password: 明文密码
        hashed_password: 哈希后的密码
    
    Returns:
        密码是否匹配
    """
    return bcrypt.checkpw(
        plain_password.encode('utf-8'), 
        hashed_password.encode('utf-8')
    )


def check_password_strength(password: str) -> Tuple[bool, list]:
    """
    检查密码强度
    
    密码强度要求：
    1. 长度至少8个字符
    2. 包含至少一个大写字母
    3. 包含至少一个小写字母
    4. 包含至少一个数字
    5. 包含至少一个特殊字符
    6. 不包含连续三个相同字符
    7. 不包含常见密码模式
    
    Args:
        password: 要检查的密码
    
    Returns:
        (密码是否足够强, 问题列表)
    """
    issues = []
    
    # 检查长度
    if len(password) < 8:
        issues.append("密码长度至少需要8个字符")
    
    # 检查是否包含大写字母
    if not re.search(r'[A-Z]', password):
        issues.append("密码必须包含至少一个大写字母")
    
    # 检查是否包含小写字母
    if not re.search(r'[a-z]', password):
        issues.append("密码必须包含至少一个小写字母")
    
    # 检查是否包含数字
    if not re.search(r'[0-9]', password):
        issues.append("密码必须包含至少一个数字")
    
    # 检查是否包含特殊字符
    if not re.search(r'[!@#$%^&*(),.?":{}|<>]', password):
        issues.append("密码必须包含至少一个特殊字符")
    
    # 检查是否包含连续三个相同字符
    if re.search(r'(.)\1\1', password):
        issues.append("密码不能包含三个连续相同的字符")
    
    # 检查常见密码模式
    common_patterns = [
        r'123456', r'password', r'qwerty', r'abc123', r'letmein',
        r'monkey', r'sunshine', r'iloveyou', r'trustno1', r'dragon',
        r'football', r'baseball', r'welcome', r'admin', r'qwertyuiop'
    ]
    
    for pattern in common_patterns:
        if pattern in password.lower():
            issues.append("密码包含常见的密码模式")
            break
    
    # 检查是否包含键盘序列
    keyboard_sequences = [
        'qwertyuiop', 'asdfghjkl', 'zxcvbnm', '1234567890',
        '0987654321', 'poiuytrewq', 'lkjhgfds', 'mnbvcxz'
    ]
    
    lower_password = password.lower()
    for sequence in keyboard_sequences:
        # 检查是否包含至少4个连续的键盘序列字符
        for i in range(len(sequence) - 3):
            if sequence[i:i+4] in lower_password:
                issues.append("密码包含键盘序列")
                break
        if issues and issues[-1] == "密码包含键盘序列":
            break
    
    is_strong = len(issues) == 0
    return is_strong, issues


def validate_password_strength(password: str) -> None:
    """
    验证密码强度，如果强度不足则抛出异常
    
    Args:
        password: 要验证的密码
    
    Raises:
        PasswordStrengthError: 如果密码强度不足
    """
    is_strong, issues = check_password_strength(password)
    if not is_strong:
        raise PasswordStrengthError(
            f"密码强度不足，需要满足以下要求: {'; '.join(issues)}",
            issues
        )