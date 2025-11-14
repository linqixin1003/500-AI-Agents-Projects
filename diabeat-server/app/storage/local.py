import os
from pathlib import Path
from .base import StorageProvider
from app.utils.url_builder import URLBuilder

class LocalStorageProvider(StorageProvider):
    """本地文件存储实现"""
    
    def __init__(self, base_dir: str = "static/uploads"):
        """
        Args:
            base_dir: 文件存储根目录
        """
        self.base_dir = Path(base_dir)
        # 确保目录存在
        os.makedirs(self.base_dir, exist_ok=True)
        
    async def save(self, file_data: bytes, path: str) -> str:
        """保存文件到本地
        
        Args:
            file_data: 文件二进制数据
            path: 相对存储路径
            
        Returns:
            str: 可访问的文件URL
        """
        # 确保目录存在
        full_path = self.base_dir / path
        os.makedirs(full_path.parent, exist_ok=True)
        
        # 写入文件
        with open(full_path, 'wb') as f:
            f.write(file_data)
            
        # 返回URL
        return URLBuilder.build_upload_url(path)
    
    async def delete(self, path: str) -> bool:
        """删除本地文件
        
        Args:
            path: 相对存储路径
            
        Returns:
            bool: 是否删除成功
        """
        try:
            full_path = self.base_dir / path
            if full_path.exists():
                os.remove(full_path)
                return True
            return False
        except Exception:
            return False

