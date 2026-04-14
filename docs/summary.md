# PaiAgent-One 项目总结文档

## 1. 项目概述

PaiAgent-One 是一个全栈 AI Agent 工作流平台，提供可视化的工作流编辑器，支持用户通过拖拽方式组合大模型节点和工具节点，构建自定义 AI 工作流。系统核心场景为：用户输入一段文字，经过大模型处理后，通过 TTS 工具节点生成音频，实现 AI 播客播放功能。

### 1.1 核心功能

- **可视化工作流编辑器**：基于 React Flow 的拖拽式画布，支持节点连线组合
- **多厂商大模型集成**：支持 DeepSeek、通义千问（Qwen）、AI Ping、智谱（Zhipu）等多家 LLM 厂商 API
- **工具节点扩展**：内置超拟人音频合成（TTS）工具节点，支持扩展更多工具
- **实时调试面板**：SSE（Server-Sent Events）推送工作流执行进度，支持逐节点查看执行结果
- **工作流管理**：工作流的创建、保存、加载、归档等完整 CRUD 操作
- **用户认证**：基于 JWT 的用户注册/登录认证体系

---

## 2. 技术架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────┐
│                   Frontend (React)                   │
│  React 18 + TypeScript + Vite 5 + Ant Design 5      │
│  @xyflow/react (React Flow v12) + Zustand            │
├─────────────────────────────────────────────────────┤
│                REST API / SSE (HTTP)                  │
├─────────────────────────────────────────────────────┤
│                Backend (Spring Boot)                  │
│  Spring Boot 3.2.5 + JDK 21 + MyBatis-Plus 3.5.6   │
├─────────────────────────────────────────────────────┤
│              MySQL 8.0+ (数据持久化)                  │
└─────────────────────────────────────────────────────┘
         │                          │
    ┌────┴────┐              ┌──────┴──────┐
    │ LLM API │              │  TTS 服务    │
    │ 多厂商   │              │  音频合成     │
    └─────────┘              └─────────────┘
```

### 2.2 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18.3.1 | UI 框架 |
| TypeScript | 5.4.5 | 类型安全 |
| Vite | 5.3.0 | 构建工具 |
| @xyflow/react | 12.3.0 | 流程图画布 |
| Ant Design | 5.17.0 | UI 组件库 |
| Zustand | 4.5.2 | 状态管理 |
| immer | 10.1.1 | 不可变状态更新 |
| Axios | 1.7.2 | HTTP 请求 |
| React Router | 6.23.1 | 路由管理 |

### 2.3 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.5 | 应用框架 |
| JDK | 21 | 运行环境 |
| MyBatis-Plus | 3.5.6 | ORM 框架 |
| MySQL | 8.0+ | 关系数据库 |
| jjwt | 0.12.5 | JWT 令牌 |
| Spring Security Crypto | - | BCrypt 密码加密 |
| Spring WebFlux | - | WebClient (LLM HTTP 调用) |
| Lombok | - | 代码简化 |

---

## 3. 项目结构

### 3.1 目录总览

```
PaiAgent-One/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml                       # Maven 项目配置
│   └── src/main/
│       ├── java/com/paiagent/
│       │   ├── PaiAgentApplication.java
│       │   ├── common/               # 通用模块
│       │   ├── auth/                 # 认证模块
│       │   ├── workflow/             # 工作流 CRUD 模块
│       │   ├── engine/               # 工作流引擎模块
│       │   ├── llm/                  # 大模型适配器模块
│       │   └── tool/                 # 工具处理器模块
│       └── resources/
│           ├── application.yml       # 应用配置
│           └── db/migration/         # 数据库迁移脚本
├── frontend/                         # React 前端
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── main.tsx                  # 入口文件
│       ├── App.tsx                   # 根组件
│       ├── types/                    # TypeScript 类型定义
│       ├── stores/                   # Zustand 状态管理
│       ├── api/                      # API 请求层
│       ├── router/                   # 路由配置
│       ├── constants/                # 常量（节点注册表）
│       ├── utils/                    # 工具函数
│       ├── hooks/                    # 自定义 Hooks
│       ├── layouts/                  # 布局组件
│       ├── pages/                    # 页面组件
│       └── components/               # 可复用组件
└── docs/                             # 项目文档
```

### 3.2 后端模块详解（62 个 Java 文件）

#### 3.2.1 通用模块 (common/)

| 文件 | 说明 |
|------|------|
| `result/R.java` | 统一 API 响应包装类 |
| `exception/BizException.java` | 业务异常 |
| `exception/AuthException.java` | 认证异常 |
| `exception/WorkflowExecutionException.java` | 工作流执行异常 |
| `exception/GlobalExceptionHandler.java` | 全局异常处理器 (@RestControllerAdvice) |
| `enums/NodeType.java` | 节点类型枚举（INPUT/LLM/TOOL/OUTPUT） |
| `enums/LlmProvider.java` | LLM 厂商枚举（OPENAI/DEEPSEEK/QWEN/ZHIPU） |
| `enums/ExecutionStatus.java` | 执行状态枚举 |
| `util/JwtUtil.java` | JWT 工具类（生成/验证/解析令牌） |
| `util/SecurityUtil.java` | 安全上下文工具类（ThreadLocal 用户信息） |
| `config/WebMvcConfig.java` | MVC 配置（CORS、拦截器注册、静态资源） |
| `config/MyBatisPlusMetaHandler.java` | 自动填充创建/更新时间 |

#### 3.2.2 认证模块 (auth/)

| 文件 | 说明 |
|------|------|
| `entity/User.java` | 用户实体 |
| `dto/LoginRequest.java` | 登录请求 DTO |
| `dto/RegisterRequest.java` | 注册请求 DTO |
| `dto/LoginResponse.java` | 登录响应 DTO（含 token） |
| `mapper/UserMapper.java` | MyBatis-Plus Mapper |
| `service/AuthService.java` | 认证服务接口 |
| `service/impl/AuthServiceImpl.java` | 认证服务实现（BCrypt + JWT） |
| `interceptor/JwtInterceptor.java` | JWT 拦截器（HandlerInterceptor） |
| `controller/AuthController.java` | 认证控制器（/api/auth/*） |

#### 3.2.3 工作流模块 (workflow/)

| 文件 | 说明 |
|------|------|
| `entity/Workflow.java` | 工作流实体（含 JSON 类型的 graphJson 字段） |
| `mapper/WorkflowMapper.java` | MyBatis-Plus Mapper |
| `dto/WorkflowCreateRequest.java` | 创建请求 DTO |
| `dto/WorkflowUpdateRequest.java` | 更新请求 DTO |
| `dto/WorkflowVO.java` | 工作流视图对象 |
| `service/WorkflowService.java` | 工作流服务接口 |
| `service/impl/WorkflowServiceImpl.java` | 工作流服务实现（CRUD + 用户归属校验） |
| `controller/WorkflowController.java` | 工作流控制器（/api/workflow/*） |

#### 3.2.4 工作流引擎模块 (engine/)

| 文件 | 说明 |
|------|------|
| `model/NodeDefinition.java` | 节点定义模型 |
| `model/EdgeDefinition.java` | 边定义模型 |
| `model/WorkflowGraph.java` | 工作流图结构 |
| `parser/WorkflowParser.java` | 解析 React Flow JSON 为 WorkflowGraph |
| `parser/VariableResolver.java` | 变量解析器（`{{nodeId.output}}` 模板语法） |
| `context/ExecutionContext.java` | 执行上下文（存放节点结果、SSE 发射器等） |
| `dto/NodeResult.java` | 节点执行结果 |
| `dto/DebugRequest.java` | 调试请求 DTO |
| `dto/ExecutionEvent.java` | SSE 执行事件 |
| `executor/TopologicalSorter.java` | 拓扑排序器（Kahn 算法，环检测） |
| `executor/WorkflowExecutor.java` | 工作流执行器核心（同步/SSE 两种模式） |
| `node/NodeHandler.java` | 节点处理器接口（策略模式） |
| `node/AbstractNodeHandler.java` | 抽象基类（模板方法：计时、日志、异常包装） |
| `node/InputNodeHandler.java` | 输入节点处理器 |
| `node/LlmNodeHandler.java` | LLM 节点处理器（调用 LLM 适配器） |
| `node/ToolNodeHandler.java` | 工具节点处理器（调用工具处理器注册表） |
| `node/OutputNodeHandler.java` | 输出节点处理器（模板渲染 + 音频检测） |
| `node/NodeHandlerRegistry.java` | 节点处理器注册表（Spring DI 自动注册） |
| `controller/ExecutionController.java` | 执行控制器（/api/execution/*） |

#### 3.2.5 大模型适配器模块 (llm/)

| 文件 | 说明 |
|------|------|
| `dto/ChatMessage.java` | 聊天消息 DTO |
| `dto/ChatRequest.java` | 聊天请求 DTO |
| `dto/ChatResponse.java` | 聊天响应 DTO |
| `adapter/LlmAdapter.java` | 适配器接口 |
| `adapter/OpenAiAdapter.java` | OpenAI 适配器（基础实现，RestTemplate） |
| `adapter/DeepSeekAdapter.java` | DeepSeek 适配器（继承 OpenAI） |
| `adapter/QwenAdapter.java` | 通义千问适配器（DashScope 兼容模式） |
| `adapter/ZhipuAdapter.java` | 智谱适配器（继承 OpenAI） |
| `adapter/LlmAdapterFactory.java` | 适配器工厂（Strategy + Factory 模式） |

#### 3.2.6 工具处理器模块 (tool/)

| 文件 | 说明 |
|------|------|
| `handler/ToolHandler.java` | 工具处理器接口 |
| `handler/ToolHandlerRegistry.java` | 工具处理器注册表 |
| `handler/TtsToolHandler.java` | TTS 音频合成工具处理器 |
| `service/FileStorageService.java` | 文件存储服务 |

### 3.3 前端模块详解（38 个 TS/TSX 文件）

#### 3.3.1 类型定义 (types/)

| 文件 | 说明 |
|------|------|
| `node.ts` | 节点数据类型（PaiNodeData 及子类型定义） |
| `workflow.ts` | 工作流类型定义 |
| `api.ts` | API 请求/响应类型定义 |

#### 3.3.2 状态管理 (stores/)

| 文件 | 说明 |
|------|------|
| `authStore.ts` | 认证状态（token、用户信息、localStorage 持久化） |
| `uiStore.ts` | UI 状态（选中节点、面板可见性） |
| `workflowStore.ts` | 工作流核心状态（nodes/edges、CRUD 操作） |
| `debugStore.ts` | 调试状态（运行状态、节点结果、最终输出） |

#### 3.3.3 API 请求层 (api/)

| 文件 | 说明 |
|------|------|
| `client.ts` | Axios 实例（请求/响应拦截器、JWT 注入） |
| `auth.ts` | 认证 API（登录、注册） |
| `workflow.ts` | 工作流 API（CRUD） |
| `debug.ts` | 调试 API（执行、SSE 流式） |

#### 3.3.4 自定义节点组件 (components/CustomNodes/)

| 文件 | 说明 |
|------|------|
| `BaseNode.tsx` | 节点基础壳（颜色条、图标标题、连接点） |
| `InputNode.tsx` | 输入节点（蓝色，仅有输出连接点） |
| `LLMNode.tsx` | 大模型节点（紫色，显示模型信息） |
| `ToolNode.tsx` | 工具节点（橙色，显示工具类型） |
| `OutputNode.tsx` | 输出节点（绿色，仅有输入连接点） |

#### 3.3.5 核心面板组件

| 文件 | 说明 |
|------|------|
| `NodePanel/index.tsx` | 左侧节点库面板（拖拽源） |
| `Canvas/index.tsx` | 中央画布（React Flow + 背景 + 小地图） |
| `ConfigPanel/index.tsx` | 右侧节点配置面板（动态表单） |
| `DebugDrawer/index.tsx` | 调试抽屉（SSE 实时执行 + 步骤展示） |
| `Header/index.tsx` | 顶部工具栏（保存/加载/调试按钮） |
| `common/AudioPlayer/index.tsx` | 音频播放器组件 |

#### 3.3.6 页面

| 文件 | 说明 |
|------|------|
| `pages/Login/index.tsx` | 登录/注册页面 |
| `pages/WorkflowList/index.tsx` | 工作流列表页面 |
| `pages/WorkflowEditor/index.tsx` | 工作流编辑器页面（核心页面） |

---

## 4. 数据库设计

数据库使用 MySQL 8.0+，包含 4 张表，统一使用 `pai_` 前缀：

### 4.1 表结构

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `pai_user` | 用户表 | id, username, password_hash, role, status |
| `pai_workflow` | 工作流表 | id, user_id, name, description, graph_json(JSON), status |
| `pai_execution_record` | 执行记录表 | id, workflow_id, user_id, status, input_params(JSON), node_results(JSON), duration_ms |
| `pai_llm_config` | 大模型配置表 | id, user_id, provider, name, base_url, api_key, model_name, is_default |

### 4.2 关键设计决策

- `graph_json` 使用 MySQL JSON 类型，完整存储 React Flow 的 nodes 和 edges 定义
- `node_results` 使用 JSON 存储每个节点的执行结果快照
- `api_key` 计划采用 AES 加密存储
- 所有表均包含 `created_at` 和 `updated_at` 自动时间戳

---

## 5. API 接口设计

### 5.1 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录，返回 JWT |
| POST | `/api/auth/register` | 用户注册 |

### 5.2 工作流接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/workflow/list` | 获取当前用户的工作流列表 |
| GET | `/api/workflow/{id}` | 获取工作流详情 |
| POST | `/api/workflow` | 创建工作流 |
| PUT | `/api/workflow/{id}` | 更新工作流 |
| DELETE | `/api/workflow/{id}` | 删除（归档）工作流 |

### 5.3 执行接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/execution/debug` | 同步执行工作流调试 |
| POST | `/api/execution/debug/stream` | SSE 流式执行工作流调试 |

---

## 6. 核心设计模式

### 6.1 策略模式 (Strategy Pattern)

**节点处理器**：`NodeHandler` 接口定义了统一的节点执行契约，每种节点类型（INPUT/LLM/TOOL/OUTPUT）有独立的实现类。`NodeHandlerRegistry` 通过 Spring 依赖注入自动收集所有实现。

```
NodeHandler (接口)
  ├── AbstractNodeHandler (抽象基类 - 模板方法)
  │   ├── InputNodeHandler
  │   ├── LlmNodeHandler
  │   ├── ToolNodeHandler
  │   └── OutputNodeHandler
  └── NodeHandlerRegistry (注册表)
```

**LLM 适配器**：`LlmAdapter` 接口统一多厂商 API 调用。所有适配器继承自 `OpenAiAdapter`（因为大多数厂商都兼容 OpenAI API 格式），只需覆写 baseUrl、apiKey、provider 等配置。

```
LlmAdapter (接口)
  └── OpenAiAdapter (基础实现)
      ├── DeepSeekAdapter
      ├── QwenAdapter
      └── ZhipuAdapter
```

### 6.2 工厂模式 (Factory Pattern)

`LlmAdapterFactory` 根据 provider 字符串选择对应的适配器实例，对上层透明。

### 6.3 模板方法模式 (Template Method)

`AbstractNodeHandler` 定义了节点执行的标准流程：前置日志 → 计时开始 → 执行（由子类实现） → 计时结束 → 异常包装。

### 6.4 拓扑排序

`TopologicalSorter` 使用 Kahn 算法（BFS）对 DAG 进行拓扑排序，确定节点执行顺序，并检测循环依赖。

### 6.5 变量解析

`VariableResolver` 通过正则 `\{\{(\w+)\.output\}\}` 解析模板中的变量引用，支持通过 nodeId 和 label 两种方式引用上游节点的输出。

---

## 7. 工作流引擎执行流程

```
1. 接收调试请求（graphJson + inputParams）
       │
2. WorkflowParser 解析 JSON → WorkflowGraph
       │
3. TopologicalSorter 拓扑排序 → 有序节点列表
       │
4. 创建 ExecutionContext（存放输入、结果、SSE emitter）
       │
5. 遍历有序节点列表：
   ├── NodeHandlerRegistry.getHandler(nodeType)
   ├── handler.execute(nodeDefinition, context)
   ├── context.putNodeResult(nodeId, result)
   └── SSE 推送节点状态事件（node_start/node_complete/node_error）
       │
6. 返回最终输出 / SSE 推送 execution_complete 事件
```

---

## 8. 前端架构设计

### 8.1 状态管理架构

使用 Zustand + immer 实现状态管理，按关注点拆分为 4 个 Store：

- **authStore**：认证状态，自动从 localStorage 恢复会话
- **workflowStore**：工作流画布核心状态（nodes/edges/连线），集成 React Flow 的 applyNodeChanges/applyEdgeChanges
- **uiStore**：UI 交互状态（选中节点、面板显隐）
- **debugStore**：调试运行状态（执行进度、节点结果、最终输出）

### 8.2 拖拽交互

1. `NodePanel` 中的 `DraggableNode` 设置 `draggable`，`onDragStart` 写入节点 JSON 到 `dataTransfer`
2. `Canvas` 通过 `useDragToCanvas` Hook 监听 `onDrop` 事件
3. 解析 JSON → `screenToFlowPosition` 坐标转换 → `createNode` → `addNode` 到 Store

### 8.3 SSE 调试流

1. 点击"执行"按钮，`DebugDrawer` 创建 `EventSource` 连接到 `/api/execution/debug/stream`
2. 监听事件类型：`node_start` → `node_complete` → `node_error` → `execution_complete`
3. 实时更新 `debugStore` 中的节点结果列表
4. 最终输出为音频 URL 时，渲染 `AudioPlayer` 组件播放

---

## 9. 配置说明

### 9.1 关键配置项 (application.yml)

| 配置路径 | 说明 | 默认值 |
|----------|------|--------|
| `server.port` | 服务端口 | 8080 |
| `spring.datasource.url` | MySQL 连接地址 | localhost:3306/paiagent |
| `spring.datasource.password` | 数据库密码 | `${DB_PASSWORD:root}` |
| `paiagent.jwt.secret` | JWT 签名密钥 | 内置默认值（生产需替换） |
| `paiagent.jwt.expiration-hours` | JWT 过期时间 | 24 小时 |
| `paiagent.file-storage.base-path` | 文件存储路径 | ./uploads |
| `paiagent.engine.default-timeout-seconds` | 工作流执行超时 | 120 秒 |
| `paiagent.engine.node-timeout-seconds` | 单节点执行超时 | 60 秒 |
| `paiagent.llm.http-connect-timeout-ms` | LLM HTTP 连接超时 | 5000 ms |
| `paiagent.llm.http-read-timeout-ms` | LLM HTTP 读取超时 | 120000 ms |

### 9.2 前端代理配置 (vite.config.ts)

- `/api` → `http://localhost:8080`（后端 API）
- `/files` → `http://localhost:8080`（文件资源）

---

## 10. 构建与验证状态

| 项目 | 命令 | 状态 |
|------|------|------|
| 前端 TypeScript 检查 | `npx tsc --noEmit` | 通过 |
| 前端生产构建 | `vite build` | 通过（JS 产物约 1.1 MB） |
| 后端 Maven 编译 | `mvn compile -q` | 通过 |

---

## 11. 内置节点注册表

### 11.1 大模型节点 (LLM)

| 节点名称 | 厂商标识 | 默认模型 | 说明 |
|----------|----------|----------|------|
| DeepSeek | deepseek | deepseek-chat | DeepSeek 大模型 |
| 通义千问 | qwen | qwen-turbo | 阿里云通义千问 |
| AI Ping | openai | gpt-3.5-turbo | 通用 OpenAI 兼容 |
| 智谱 | zhipu | glm-4-flash | 智谱 AI 大模型 |

### 11.2 工具节点 (Tool)

| 节点名称 | 工具类型 | 说明 |
|----------|----------|------|
| 超拟人音频合成 | tts | 文本转语音，支持多种音色 |

---

## 12. 文件统计

| 类别 | 文件数 | 说明 |
|------|--------|------|
| 后端 Java 文件 | 62 | 含实体、DTO、控制器、服务、引擎等 |
| 前端 TS/TSX 文件 | 38 | 含页面、组件、Store、API、类型等 |
| 配置文件 | 6 | pom.xml, application.yml, package.json, vite.config.ts, tsconfig.json, index.html |
| SQL 迁移脚本 | 1 | V1__init_schema.sql |
| **总计** | **107** | |
