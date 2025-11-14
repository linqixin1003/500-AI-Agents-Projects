# Diabeat Server Docker部署指南

本指南提供了使用Docker和Docker Compose部署Diabeat Server的完整步骤和最佳实践。

## 目录
- [前提条件](#前提条件)
- [快速部署](#快速部署)
- [配置说明](#配置说明)
- [环境变量](#环境变量)
- [数据库初始化](#数据库初始化)
- [生产环境部署](#生产环境部署)
- [扩展和性能优化](#扩展和性能优化)
- [故障排除](#故障排除)

## 前提条件

在开始部署之前，请确保您的系统已安装以下软件：

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)

## 快速部署

### 1. 克隆仓库

```bash
git clone https://your-repository-url/diabeat-server.git
cd diabeat-server
```

### 2. 创建环境配置文件

复制示例环境文件并根据您的需要进行配置：

```bash
cp .env.example .env
```

编辑`.env`文件，设置必要的环境变量。

### 3. 使用Docker Compose启动服务

```bash
docker-compose up -d
```

此命令将：
- 构建应用镜像
- 启动所有服务（PostgreSQL、MongoDB、Redis和应用服务器）
- 自动初始化数据库（如果sql目录中有初始化脚本）

### 4. 验证部署

服务启动后，您可以通过以下方式验证部署是否成功：

```bash
# 检查服务状态
docker-compose ps

# 查看应用日志
docker-compose logs -f app
```

访问 http://localhost:8000/docs 查看API文档。

## 配置说明

### Dockerfile 详解

我们使用多阶段构建来减小最终镜像大小：

1. **构建阶段**：安装构建依赖和Python包
2. **最终阶段**：仅复制必要的运行时文件和虚拟环境

### Docker Compose 服务说明

`docker-compose.yml`定义了四个主要服务：

1. **db**: PostgreSQL数据库
2. **mongodb**: MongoDB数据库
3. **redis**: Redis缓存服务
4. **app**: 主应用服务器

## 环境变量

### 数据库连接

- `DATABASE_URL`: PostgreSQL连接字符串
- `MONGODB_URL`: MongoDB连接字符串
- `REDIS_URL`: Redis连接字符串

### 应用配置

- `ENVIRONMENT`: 运行环境（dev/production）
- `SECRET_KEY`: 用于加密的密钥，生产环境中请使用强密钥

## 数据库初始化

数据库初始化脚本应放置在`sql`目录中，Docker Compose启动时会自动执行这些脚本。

## 生产环境部署

### 1. 优化Dockerfile

生产环境部署时，请确保：
- 使用最新的基础镜像
- 移除开发依赖
- 启用最小化权限

### 2. 配置修改

生产环境中，建议进行以下修改：

- 修改`docker-compose.yml`中的`ENVIRONMENT`为`production`
- 生成并使用强`SECRET_KEY`
- 移除`--reload`标志以提高性能
- 考虑添加反向代理（如Nginx）以处理SSL终止和负载均衡

### 3. 安全建议

- 不要在环境变量或配置文件中硬编码敏感信息
- 定期更新基础镜像和依赖包
- 限制容器的网络访问权限
- 配置适当的日志监控

## 扩展和性能优化

### 扩展应用实例

要扩展应用服务器以处理更多流量，可以使用Docker Compose的扩展功能：

```bash
docker-compose up -d --scale app=3
```

### 性能优化建议

1. **使用缓存**：充分利用Redis缓存频繁访问的数据
2. **数据库索引**：确保数据库查询使用适当的索引
3. **异步处理**：对于长时间运行的任务，考虑使用异步处理
4. **监控资源使用**：定期检查CPU、内存和磁盘使用情况

## 故障排除

### 常见问题

#### 数据库连接失败

检查环境变量是否正确设置，以及数据库服务是否正常运行：

```bash
docker-compose logs db
```

#### 应用启动失败

查看应用日志以获取详细错误信息：

```bash
docker-compose logs app
```

#### 端口冲突

如果端口8000已被占用，可以修改`docker-compose.yml`中的端口映射：

```yaml
ports:
  - "8080:8000"
```

### 其他命令

```bash
# 停止服务
docker-compose down

# 重新构建镜像
docker-compose build

# 查看服务资源使用情况
docker stats
```

---

如有任何问题或需要进一步的帮助，请参考项目文档或联系开发团队。