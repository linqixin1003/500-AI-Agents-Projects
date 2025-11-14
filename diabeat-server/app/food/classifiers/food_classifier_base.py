from abc import ABC, abstractmethod
from typing import List, Tuple
from PIL import Image

class FoodClassifierBase(ABC):
    """食物分类器基类"""
    
    @abstractmethod
    async def identify(self, image_data: bytes) -> Tuple[List[str], float]:
        """识别食物
        
        Args:
            image_data: 图片二进制数据
            
        Returns:
            Tuple[List[str], float]: (食物名称列表, 置信度)
        """
        pass
    
    @abstractmethod
    async def estimate_weight(self, image_data: bytes, food_name: str) -> float:
        """估算食物分量
        
        Args:
            image_data: 图片二进制数据
            food_name: 食物名称
            
        Returns:
            float: 估算重量（克）
        """
        pass

