from abc import ABC, abstractmethod
from typing import BinaryIO, Optional

class StorageProvider(ABC):
    """存储提供者的抽象基类"""
    
    @abstractmethod
    async def save(self, file_data: bytes, path: str) -> str:
        """保存文件
        
        Args:
            file_data: 文件二进制数据
            path: 文件保存路径
            
        Returns:
            str: 可访问的文件URL
        """
        pass
    
    @abstractmethod
    async def delete(self, path: str) -> bool:
        """删除文件
        
        Args:
            path: 文件路径
            
        Returns:
            bool: 是否删除成功
        """
        pass

