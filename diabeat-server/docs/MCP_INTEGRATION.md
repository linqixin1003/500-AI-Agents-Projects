# MCP（微服务通信平台）集成指南

## 概述

本项目集成了MCP（微服务通信平台）功能，用于增强应用的智能决策和服务间通信能力。MCP集成主要用于：

- 健康咨询和建议生成
- 血糖预测增强
- 营养分析优化
- 服务间的安全通信

## 配置要求

### 1. 环境变量配置

请将以下配置添加到您的 `.env` 文件中：

```
# FastMCP 服务配置
FASTMCP_URL=http://localhost:8001
FASTMCP_API_KEY=your-api-key-here
```

您可以参考 [`.env.example.mcp`](../config/.env.example.mcp) 文件获取配置模板。

### 2. 系统要求

- FastMCP服务器（可选，如果不配置，系统会使用回退方案）
- Python 3.9+
- 已安装的依赖包（通过 `pip install -r requirements.txt` 安装）

## 功能说明

### 1. 健康咨询

应用可以通过MCP向健康顾问代理发送咨询请求，获取针对特定健康问题的专业建议。

### 2. 血糖预测增强

MCP集成可提供更准确的血糖预测结果，包括置信度分数和详细的风险评估。

### 3. 服务健康检查

系统启动时会自动检查MCP服务的健康状态，并在日志中显示连接结果。

## 使用示例

### 健康咨询示例

```python
from app.utils.fastmcp_client import ask_health_question

async def get_health_advice():
    # 发送健康咨询问题
    question = "我应该如何调整饮食来控制血糖波动？"
    context = "我是一位2型糖尿病患者，餐后血糖经常高于10mmol/L"
    
    response = await ask_health_question(question, context)
    print(response)
```

### 血糖预测增强示例

```python
from app.prediction.service import predict_blood_glucose
from app.prediction.schemas import BloodGlucosePredictionRequest

async def enhanced_glucose_prediction():
    # 创建预测请求
    request_data = BloodGlucosePredictionRequest(
        current_blood_glucose=7.2,
        recent_meals=[...],  # 最近的餐食数据
        insulin_history=[...],  # 胰岛素注射历史
        activity_level="moderate"
    )
    
    # 获取增强的预测结果
    result = await predict_blood_glucose(request_data, use_mcp=True)
    print(f"预测置信度: {result.confidence_score}")
    print(f"详细风险评估: {result.risk_assessment}")
```

## 故障排除

### 常见问题

1. **MCP服务连接失败**
   - 检查FASTMCP_URL是否正确
   - 确认FastMCP服务器是否正在运行
   - 验证API密钥是否有效

2. **功能不可用**
   - 查看应用日志中的错误信息
   - 确保所有依赖都已正确安装

3. **回退机制**
   - 如果MCP服务不可用，系统会自动切换到基础功能模式
   - 某些高级功能（如详细风险评估）可能不可用

## 注意事项

- MCP集成是可选的，系统可以在没有MCP服务的情况下正常运行基本功能
- 生产环境中请确保使用有效的API密钥和安全的连接
- 定期检查MCP服务的健康状态以确保最佳性能

---

*最后更新: 2024年*
