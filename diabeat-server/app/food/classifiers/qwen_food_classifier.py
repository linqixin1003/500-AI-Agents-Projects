import base64
import json
import re
import logging
from typing import List, Tuple
from openai import OpenAI
from app.config import settings
from .food_classifier_base import FoodClassifierBase

logger = logging.getLogger(__name__)

class QwenFoodClassifier(FoodClassifierBase):
    """使用通义千问进行食物识别"""
    
    def __init__(self):
        if not settings.DASHSCOPE_API_KEY:
            raise ValueError("DASHSCOPE_API_KEY not configured")
        self.client = OpenAI(
            api_key=settings.DASHSCOPE_API_KEY,
            base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
        )
        logger.info("✅ 通义千问分类器初始化成功，使用模型: qwen3-vl-plus")
    
    async def identify(self, image_data: bytes) -> Tuple[List[str], float]:
        """使用通义千问识别食物"""
        try:
            # 将图片转换为 base64
            base64_image = base64.b64encode(image_data).decode('utf-8')
            image_size_kb = len(image_data) / 1024
            logger.info(f"📸 开始识别食物，图片大小: {image_size_kb:.2f} KB")
            
            # 优化的提示词
            prompt = """你是一个专业的食物识别专家。请仔细分析这张图片中的所有食物。

要求：
1. 识别图片中所有可见的食物（包括主食、配菜、调料等）
2. 使用中文食物名称（例如：汉堡、米饭、鸡腿、番茄、生菜等）
3. 根据食物在图片中的大小和常见分量，估算每样食物的重量（单位：克）
4. 给出识别置信度（0-1之间的小数）
5. 如果食物有明确的烹饪方式，请标注（例如：煎、炸、蒸、煮、烤等）

请严格按照以下 JSON 格式返回结果，不要添加任何其他文字说明：

{
  "foods": [
    {"name": "食物名称", "weight": 重量数值, "confidence": 置信度数值, "cooking_method": "烹饪方式或null"}
  ],
  "total_confidence": 总体置信度数值
}

示例：
{
  "foods": [
    {"name": "汉堡", "weight": 200, "confidence": 0.95, "cooking_method": "烤"},
    {"name": "生菜", "weight": 30, "confidence": 0.90, "cooking_method": null},
    {"name": "番茄", "weight": 25, "confidence": 0.88, "cooking_method": null}
  ],
  "total_confidence": 0.91
}

现在请识别图片中的食物："""
            
            # 调用通义千问 API (使用 OpenAI 兼容模式)
            messages = [
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "text",
                            "text": prompt
                        },
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/jpeg;base64,{base64_image}"
                            }
                        }
                    ]
                }
            ]
            
            logger.info("🤖 调用通义千问 API (qwen3-vl-plus)...")
            response = self.client.chat.completions.create(
                model="qwen3-vl-plus",
                messages=messages,
                max_tokens=2000,
                temperature=0.1
            )
            
            logger.info(f"📥 API 调用成功")
            
            if response.choices and response.choices[0].message:
                try:
                    # 获取响应内容 (OpenAI 兼容模式)
                    message = response.choices[0].message
                    content = message.content
                    
                    if not content:
                        logger.error("❌ API 响应格式异常: 缺少 content 字段")
                        return ["未知食物"], 0.5
                    
                    logger.info(f"📝 模型原始响应 (前200字符): {content[:200]}...")
                    
                    # 尝试提取 JSON（支持多种格式）
                    json_content = None
                    
                    # 方法1: 查找 ```json ... ```
                    json_match = re.search(r'```json\s*\n?(.*?)\n?```', content, re.DOTALL)
                    if json_match:
                        json_content = json_match.group(1).strip()
                    
                    # 方法2: 查找 ``` ... ```
                    if not json_content:
                        json_match = re.search(r'```\s*\n?(.*?)\n?```', content, re.DOTALL)
                        if json_match:
                            json_content = json_match.group(1).strip()
                    
                    # 方法3: 查找 { ... } 直接提取
                    if not json_content:
                        json_match = re.search(r'\{[\s\S]*\}', content)
                        if json_match:
                            json_content = json_match.group(0).strip()
                    
                    # 如果还是找不到，使用整个内容
                    if not json_content:
                        json_content = content.strip()
                    
                    logger.info(f"🔍 提取的 JSON 内容: {json_content[:300]}...")
                    
                    # 解析 JSON
                    result = json.loads(json_content)
                    foods = result.get("foods", [])
                    total_confidence = result.get("total_confidence", 0.8)
                    
                    if not foods:
                        logger.warning("⚠️ 识别结果为空，未找到任何食物")
                        return [{"name": "未知食物", "weight": 200.0, "confidence": 0.5, "cooking_method": None}], 0.5
                    
                    # 返回完整的食物信息（包含名称、重量、置信度、烹饪方式）
                    food_info_list = []
                    for food in foods:
                        food_info = {
                            "name": food.get("name", "未知食物"),
                            "weight": food.get("weight", 200.0),  # 使用模型返回的重量
                            "confidence": food.get("confidence", total_confidence),  # 使用每个食物的置信度
                            "cooking_method": food.get("cooking_method")
                        }
                        food_info_list.append(food_info)
                    
                    food_names = [info["name"] for info in food_info_list]
                    logger.info(f"✅ 识别成功: {food_names}, 置信度: {total_confidence}")
                    logger.info(f"📊 食物详情: {food_info_list}")
                    
                    # 返回食物信息列表和总体置信度
                    return food_info_list, total_confidence
                    
                except json.JSONDecodeError as je:
                    logger.error(f"❌ JSON 解析失败: {str(je)}")
                    logger.error(f"   原始内容: {content[:500] if 'content' in locals() else 'N/A'}")
                    return [{"name": "未知食物", "weight": 200.0, "confidence": 0.5, "cooking_method": None}], 0.5
                except Exception as pe:
                    logger.error(f"❌ 响应解析异常: {str(pe)}")
                    logger.error(f"   响应对象: {response}")
                    return [{"name": "未知食物", "weight": 200.0, "confidence": 0.5, "cooking_method": None}], 0.5
            else:
                logger.error("❌ API 调用失败: 响应中没有 choices 或 message")
                return [{"name": "未知食物", "weight": 200.0, "confidence": 0.5, "cooking_method": None}], 0.5
                
        except Exception as e:
            logger.error(f"❌ 食物识别异常: {type(e).__name__}: {str(e)}", exc_info=True)
            return [{"name": "未知食物", "weight": 200.0, "confidence": 0.5, "cooking_method": None}], 0.5
    
    async def estimate_weight(self, image_data: bytes, food_name: str) -> float:
        """估算食物分量"""
        return 200.0  # 默认 200 克

