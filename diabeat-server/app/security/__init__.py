"""安全模块，提供密码验证、输入验证和其他安全相关功能"""

from app.security.password import (
    verify_password,
    get_password_hash,
    check_password_strength,
    PasswordStrengthError
)
from app.security.input_validation import (
    sanitize_input,
    validate_email,
    validate_phone_number,
    validate_input_format
)
from app.security.cors import configure_cors
from app.security.rate_limiting import RateLimiter, rate_limit

__all__ = [
    # 密码相关
    "verify_password",
    "get_password_hash",
    "check_password_strength",
    "PasswordStrengthError",
    # 输入验证相关
    "sanitize_input",
    "validate_email",
    "validate_phone_number",
    "validate_input_format",
    # CORS相关
    "configure_cors",
    # 速率限制相关
    "RateLimiter",
    "rate_limit"
]