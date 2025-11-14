from app.config import settings
from urllib.parse import urljoin

class URLBuilder:
    """URL 构建工具类"""
    
    @staticmethod
    def get_base_url() -> str:
        """获取基础 URL"""
        if settings.STATIC_ASSETS_URL:
            return settings.STATIC_ASSETS_URL.rstrip('/')
        
        # 从 HOST 构建
        host = settings.HOST
        if not host.startswith('http'):
            host = f"http://{host}"
        return host.rstrip('/')
    
    @staticmethod
    def build_upload_url(path: str) -> str:
        """构建上传文件的访问 URL
        
        Args:
            path: 文件相对路径
            
        Returns:
            str: 完整的文件访问 URL
        """
        base_url = URLBuilder.get_base_url()
        # 确保 path 以 / 开头
        if not path.startswith('/'):
            path = '/' + path
        return urljoin(base_url, f"/static{path}")

