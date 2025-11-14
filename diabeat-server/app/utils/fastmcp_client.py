import httpx
import asyncio
from typing import Optional, Dict, Any, List
import logging
from app.config import settings

logger = logging.getLogger(__name__)

class FastMCPClient:
    """FastMCP服务器客户端，用于与MCP服务器进行通信"""
    
    def __init__(self):
        self.base_url = settings.FASTMCP_URL
        self.api_key = settings.FASTMCP_API_KEY
        self.timeout = httpx.Timeout(settings.MCP_TIMEOUT)
        self.retry_count = settings.MCP_RETRY_COUNT
        self.enabled = settings.MCP_ENABLED
        self.headers = {
            "Content-Type": "application/json"
        }
        
        if self.api_key:
            self.headers["Authorization"] = f"Bearer {self.api_key}"
        
    async def _make_request(self, endpoint: str, method: str = "GET", data: Optional[Dict] = None) -> Optional[Dict]:
        """基础请求方法，带重试机制"""
        if not self.enabled:
            logger.info("MCP集成已禁用，跳过请求")
            return None
        
        url = f"{self.base_url}{endpoint}"
        
        async with httpx.AsyncClient(timeout=self.timeout) as client:
            for attempt in range(self.retry_count):
                try:
                    if method == "GET":
                        response = await client.get(url, headers=self.headers, params=data)
                    elif method == "POST":
                        response = await client.post(url, headers=self.headers, json=data)
                    elif method == "PUT":
                        response = await client.put(url, headers=self.headers, json=data)
                    elif method == "DELETE":
                        response = await client.delete(url, headers=self.headers)
                    else:
                        logger.error(f"不支持的HTTP方法: {method}")
                        return None
                    
                    if response.status_code == 200:
                        return response.json()
                    elif response.status_code == 401:
                        logger.error("MCP服务认证失败: 无效的API密钥")
                        return None
                    elif response.status_code == 404:
                        logger.error(f"MCP服务端点不存在: {endpoint}")
                        return None
                    else:
                        logger.warning(
                            f"MCP服务请求失败 (状态码: {response.status_code})，尝试 {attempt + 1}/{self.retry_count}"
                        )
                        # 重试前等待
                        await asyncio.sleep(1 * (attempt + 1))
                        
                except httpx.RequestError as e:
                    logger.warning(
                        f"MCP服务请求错误: {str(e)}，尝试 {attempt + 1}/{self.retry_count}"
                    )
                    # 重试前等待
                    await asyncio.sleep(1 * (attempt + 1))
                except Exception as e:
                    logger.error(f"MCP服务请求发生未知错误: {str(e)}")
                    return None
            
            logger.error(f"MCP服务请求在 {self.retry_count} 次尝试后失败")
            return None
    
    async def get_data(self, endpoint: str, params: Optional[Dict] = None) -> Dict[str, Any]:
        """获取数据"""
        return await self._make_request(endpoint, method="GET", data=params)
    
    async def post_data(self, endpoint: str, data: Dict) -> Dict[str, Any]:
        """发送数据"""
        return await self._make_request(endpoint, method="POST", data=data)
    
    async def put_data(self, endpoint: str, data: Dict) -> Dict[str, Any]:
        """更新数据"""
        return await self._make_request(endpoint, method="PUT", data=data)
    
    async def delete_data(self, endpoint: str) -> Dict[str, Any]:
        """删除数据"""
        return await self._make_request(endpoint, method="DELETE")
    
    async def query_agent(self, agent_name: str, query: Dict) -> Dict[str, Any]:
        """查询特定代理"""
        endpoint = f"/api/agents/{agent_name}/query"
        return await self.post_data(endpoint, query)
    
    async def list_agents(self) -> List[Dict[str, Any]]:
        """列出所有可用代理"""
        result = await self.get_data("/api/agents")
        return result.get("agents", [])
    
    async def health_check(self) -> bool:
        """检查MCP服务器健康状态"""
        try:
            result = await self.get_data("/health")
            return result is not None and result.get("status") == "healthy"
        except Exception as e:
            logger.error(f"健康检查失败: {str(e)}")
            return False
    
    async def ask_health_question(
        self, question: str, context: Optional[str] = None
    ) -> Dict[str, Any]:
        """向健康顾问代理发送咨询问题"""
        try:
            data = {
                "question": question,
                "context": context or ""
            }
            
            # 尝试两种可能的端点路径
            for endpoint in [
                "/api/agents/health-advisor/ask",
                "/agents/health-advisor/ask"
            ]:
                response = await self.post_data(endpoint, data)
                if response:
                    return response
            
            # 如果都失败，使用回退方案
            logger.warning("所有健康咨询端点都失败，使用回退方案")
            return self._fallback_health_response(question)
            
        except Exception as e:
            logger.error(f"健康咨询请求失败: {str(e)}")
            return self._fallback_health_response(question)
    
    def _fallback_health_response(self, question: str) -> Dict[str, Any]:
        """当MCP服务不可用时的回退响应"""
        return {
            "answer": "感谢您的咨询。为了给您提供更准确的建议，我们建议您咨询专业医疗人员。",
            "confidence": 0.5,
            "source": "fallback",
            "note": "此回答为系统默认回复，仅供参考。MCP服务暂时不可用。"
        }
    
    async def enhance_glucose_prediction(
        self, prediction_data: Dict[str, Any]
    ) -> Dict[str, Any]:
        """增强血糖预测结果"""
        try:
            # 尝试两种可能的端点路径
            for endpoint in [
                "/api/agents/diabetes-analyst/enhance",
                "/agents/diabetes-analyst/enhance"
            ]:
                response = await self.post_data(endpoint, prediction_data)
                if response:
                    return response
            
            # 如果都失败，使用回退方案
            logger.warning("所有预测增强端点都失败，使用回退方案")
            return self._fallback_enhanced_prediction()
            
        except Exception as e:
            logger.error(f"增强血糖预测失败: {str(e)}")
            return self._fallback_enhanced_prediction()
    
    def _fallback_enhanced_prediction(self) -> Dict[str, Any]:
        """预测增强的回退方案"""
        return {
            "confidence_score": 0.7,
            "risk_assessment": {
                "severity": "medium",
                "factors": ["无法获取详细信息"],
                "recommendations": ["继续监测血糖"]
            },
            "note": "使用回退模式生成的增强预测"
        }

# 创建全局客户端实例
fastmcp_client = FastMCPClient()


# 工具函数
def is_mcp_available() -> bool:
    """检查MCP服务是否可用"""
    return settings.MCP_ENABLED


async def ask_health_question(
    question: str, context: Optional[str] = None
) -> Dict[str, Any]:
    """快捷函数：发送健康咨询问题"""
    return await fastmcp_client.ask_health_question(question, context)


async def enhance_glucose_prediction(
    prediction_data: Dict[str, Any]
) -> Dict[str, Any]:
    """快捷函数：增强血糖预测"""
    return await fastmcp_client.enhance_glucose_prediction(prediction_data)
