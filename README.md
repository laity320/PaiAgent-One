# PaiAgent One

一个可视化的 AI Agent 工作流编排平台，通过拖拽方式构建智能工作流，支持 LLM 调用、工具集成和实时调试。

## 功能特性

- **可视化工作流编排** - 基于 React Flow 的拖拽式节点编辑器
- **多 LLM 支持** - 集成 DeepSeek、通义千问、智谱 AI 等大语言模型
- **工具节点** - 支持 TTS 语音合成等工具节点
- **实时调试** - 支持 SSE 实时推送执行进度和结果
- **用户认证** - JWT 认证机制，支持用户注册登录
- **工作流管理** - 工作流的创建、保存、编辑、删除

## 技术栈

### 后端
- **Java 21** + **Spring Boot 3.2.5**
- **MyBatis-Plus** - ORM 框架
- **MySQL** - 数据持久化
- **JWT** - 身份认证
- **SSE** - 服务器推送事件

### 前端
- **React 18** + **TypeScript**
- **Vite** - 构建工具
- **Ant Design** - UI 组件库
- **React Flow** - 工作流可视化
- **Zustand** - 状态管理

## 项目结构

```
PaiAgent-One/
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/paiagent/
│   │       ├── auth/        # 认证模块
│   │       ├── workflow/    # 工作流管理
│   │       ├── engine/      # 工作流执行引擎
│   │       ├── llm/         # LLM 适配器
│   │       └── tool/        # 工具处理器
│   └── pom.xml
├── frontend/                # React 前端
│   ├── src/
│   │   ├── components/      # 组件
│   │   ├── pages/           # 页面
│   │   ├── stores/          # 状态管理
│   │   └── api/             # API 接口
│   └── package.json
└── uploads/                 # 文件上传目录
```

## 快速开始

### 环境要求
- JDK 21+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 1. 数据库配置

创建 MySQL 数据库：
```sql
CREATE DATABASE paiagent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 后端启动

```bash
cd backend

# 方式一：使用 Maven
mvn spring-boot:run

# 方式二：使用 run.bat (Windows)
run.bat
```

后端服务默认运行在 `http://localhost:8080`

### 3. 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器默认运行在 `http://localhost:5173`

### 4. 访问应用

打开浏览器访问：`http://localhost:5173`

默认账号：
- 用户名：`admin`
- 密码：`admin123`

## 配置说明

### 后端配置 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/paiagent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:123456}

paiagent:
  jwt:
    secret: ${JWT_SECRET:paiagent-one-secret-key-change-in-production-2024}
    expiration-hours: 24
  file-storage:
    base-path: ./uploads
    url-prefix: /files
```

## 节点类型

| 节点类型 | 说明 | 配置项 |
|---------|------|--------|
| INPUT | 输入节点 | 输入文本 |
| LLM | 大语言模型 | 模型选择、API Key、系统提示词、温度参数 |
| TOOL | 工具节点 | TTS 语音合成等 |
| OUTPUT | 输出节点 | 输出格式配置 |

## 支持的 LLM 提供商

- DeepSeek
- 通义千问 (Qwen)
- 智谱 AI (Zhipu)
- OpenAI 兼容接口

## 开发计划

- [x] 基础工作流编排
- [x] 多 LLM 支持
- [x] TTS 语音合成
- [x] 实时调试 (SSE)
- [ ] 更多工具节点
- [ ] 条件分支节点
- [ ] 循环节点
- [ ] 工作流导入导出

## 许可证

MIT License
