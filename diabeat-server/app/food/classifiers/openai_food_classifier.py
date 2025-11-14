import base64
import json
from typing import List, Tuple
from openai import OpenAI
from app.config import settings
from .food_classifier_base import FoodClassifierBase

class OpenAIFoodClassifier(FoodClassifierBase):
    """使用 OpenAI Vision API 进行食物识别"""
    
    def __init__(self):
        if not settings.OPENAI_API_KEY:
            raise ValueError("OPENAI_API_KEY not configured")
        self.client = OpenAI(api_key=settings.OPENAI_API_KEY)
    
    async def identify(self, image_data: bytes) -> Tuple[List[str], float]:
        """使用 OpenAI Vision 识别食物"""
        try:
            # 将图片转换为 base64
            base64_image = base64.b64encode(image_data).decode('utf-8')
            
            # 调用 OpenAI Vision API
            response = self.client.chat.completions.create(
                model="gpt-4o",
                messages=[
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": """请识别这张图片中的食物。返回 JSON 格式：
{
  "foods": [
    {"name": "食物名称", "weight": 估算重量(克), "confidence": 置信度(0-1), "cooking_method": "烹饪方式"}
  ],
  "total_confidence": 总体置信度(0-1)
}

请识别所有可见的食物，并估算每样食物的重量。如果无法确定，请给出合理估算。"""
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": f"data:image/jpeg;base64,{base64_image}"
                                }
                            }
                        ]
                    }
                ],
                max_tokens=1000
            )
            
            # 解析响应
            content = response.choices[0].message.content
            # 尝试提取 JSON
            if "```json" in content:
                content = content.split("```json")[1].split("```")[0].strip()
            elif "```" in content:
                content = content.split("```")[1].split("```")[0].strip()
            
            result = json.loads(content)
            foods = result.get("foods", [])
            total_confidence = result.get("total_confidence", 0.8)
            
            # 提取食物名称列表
            food_names = [food["name"] for food in foods]
            
            return food_names, total_confidence
            
        except Exception as e:
            # 如果识别失败，返回默认值
            return ["未知食物"], 0.5
    
    async def estimate_weight(self, image_data: bytes, food_name: str) -> float:
        """估算食物分量（在 identify 中已包含，这里返回默认值）"""
        # OpenAI Vision 在识别时已经返回了重量估算
        # 这里可以进一步优化，使用专门的模型
        return 200.0  # 默认 200 克

