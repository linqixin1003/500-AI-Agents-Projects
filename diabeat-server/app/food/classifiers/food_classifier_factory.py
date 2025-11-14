from typing import Optional
from app.config import settings
from .food_classifier_base import FoodClassifierBase
from .openai_food_classifier import OpenAIFoodClassifier
from .qwen_food_classifier import QwenFoodClassifier

class FoodClassifierFactory:
    """食物分类器工厂"""
    
    @staticmethod
    def create(classifier_type: Optional[str] = None) -> FoodClassifierBase:
        """创建分类器实例
        
        Args:
            classifier_type: 分类器类型 ('openai', 'qwen')
            
        Returns:
            FoodClassifierBase: 分类器实例
        """
        provider = (classifier_type or settings.FOOD_CLASSIFIER_PROVIDER).lower()
        if provider == "openai":
            return OpenAIFoodClassifier()
        if provider == "qwen":
            return QwenFoodClassifier()
        raise ValueError(f"Unknown classifier type: {provider}")

