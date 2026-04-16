# PaiAgent - 企业级 AI 工作流可视化编排平台

基于 LangGraph4j + Spring AI 的企业级 AI 工作流平台，支持通过可视化拖拽界面编排多种大模型和工具节点，使用状态图引擎执行复杂 AI 任务。

## 核心特性

- **可视化工作流编排** - 基于 React Flow 的拖拽式节点编辑器，支持 LLM、工具、输入输出等多种节点类型
- **LangGraph4j 状态图引擎** - 参考 LangGraph 架构，实现 GraphBuilder 节点注册、NodeAdapter 适配器桥接、WorkflowState 状态管理
- **多厂商 LLM 无缝切换** - ChatClientFactory 动态工厂，运行时根据节点配置动态创建 OpenAI 兼容的 ChatClient，支持 OpenAI、DeepSeek、通义千问、智谱 AI 等
- **模板方法模式重构** - AbstractLLMNodeExecutor 基类统一 800+ 行公共逻辑，5 个 LLM 节点子类仅需 10-20 行代码
- **Skill 预置知识包** - SkillRegistry 自动加载、Reference 缓存、支持 FULL（全量）/ PROGRESSIVE（渐进式）两种注入模式
- **TTS 语音合成** - UTF-8 字节级文本分段、智能标点断句、并行合成、WAV 格式合并
- **实时调试** - SSE 实时推送执行进度和节点结果
- **用户认证** - JWT 认证机制，支持用户注册登录

## 技术栈

### 后端
- **Java 21** + **Spring Boot 3.4.1**
- **Spring AI 1.0** - AI 应用开发框架
- **LangGraph4j 风格架构** - 状态图引擎设计
- **MyBatis-Plus** - ORM 框架
- **MySQL 8.0** - 数据持久化
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
│   ├── src/main/java/com/paiagent/
│   │   ├── auth/            # 认证模块 (JWT)
│   │   ├── workflow/        # 工作流管理
│   │   ├── engine/          # 工作流执行引擎
│   │   │   ├── graph/       # LangGraph4j 风格状态图
│   │   │   │   ├── GraphBuilder.java      # 图构建器
│   │   │   │   ├── StateGraph.java        # 编译后的状态图
│   │   │   │   ├── NodeAdapter.java       # 节点适配器
│   │   │   │   └── WorkflowState.java     # 状态管理器
│   │   │   ├── executor/    # 执行器
│   │   │   ├── node/        # 节点处理器
│   │   │   │   ├── llm/     # LLM 节点执行器
│   │   │   │   │   ├── AbstractLLMNodeExecutor.java  # 模板基类
│   │   │   │   │   ├── DefaultLLMNodeExecutor.java   # 默认 LLM
│   │   │   │   │   ├── SummarizeLLMNodeExecutor.java # 摘要
│   │   │   │   │   ├── TranslateLLMNodeExecutor.java # 翻译
│   │   │   │   │   ├── QALLMNodeExecutor.java        # 问答
│   │   │   │   │   └── CodeGenLLMNodeExecutor.java   # 代码生成
│   │   │   │   ├── InputNodeHandler.java
│   │   │   │   ├── OutputNodeHandler.java
│   │   │   │   └── ToolNodeHandler.java
│   │   │   └── parser/      # 变量解析器
│   │   ├── llm/             # LLM 适配器层
│   │   │   ├── adapter/     # 厂商适配器
│   │   │   │   ├── OpenAiAdapter.java
│   │   │   │   ├── DeepSeekAdapter.java
│   │   │   │   ├── QwenAdapter.java
│   │   │   │   └── ZhipuAdapter.java
│   │   │   ├── client/      # 动态客户端
│   │   │   │   └── DynamicChatClient.java
│   │   │   ├── factory/     # 动态工厂
│   │   │   │   └── ChatClientFactory.java
│   │   │   └── dto/         # 数据传输对象
│   │   ├── skill/           # Skill 预置知识包
│   │   │   ├── model/       # 实体模型
│   │   │   ├── registry/    # 注册中心
│   │   │   └── injector/    # 注入器
│   │   └── tool/            # 工具处理器
│   │       └── handler/
│   │           └── TtsToolHandler.java    # TTS 语音合成
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

# 方式一：使用 Maven（需要 Java 21）
export JAVA_HOME="/path/to/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
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

前端开发服务器默认运行在 `http://localhost:3000`

### 4. 访问应用

打开浏览器访问：`http://localhost:3000`

默认账号：
- 用户名：`admin`
- 密码：`admin123`

## 配置说明

### 后端配置 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/paiagent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: ${DB_PASSWORD:123456}

paiagent:
  jwt:
    secret: ${JWT_SECRET:paiagent-one-secret-key-change-in-production-2024}
    expiration-hours: 24
  file-storage:
    base-path: ./uploads
    url-prefix: /files
  llm:
    providers:
      openai:
        base-url: ${OPENAI_BASE_URL:https://api.openai.com}
        api-key: ${OPENAI_API_KEY:}
      deepseek:
        base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
        api-key: ${DEEPSEEK_API_KEY:}
      tongyi:
        base-url: ${QWEN_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode}
        api-key: ${QWEN_API_KEY:}
      zhipu:
        base-url: ${ZHIPU_BASE_URL:https://open.bigmodel.cn/api/paas}
        api-key: ${ZHIPU_API_KEY:}
```

## 节点类型

| 节点类型 | 说明 | 配置项 |
|---------|------|--------|
| INPUT | 输入节点 | 输入文本 |
| LLM | 大语言模型（通用） | 模型选择、API Key、系统提示词、温度参数、Skill 知识包 |
| SUMMARIZE | 文本摘要 | 模型选择、摘要长度、Skill 知识包 |
| TRANSLATE | 文本翻译 | 模型选择、目标语言、Skill 知识包 |
| QA | 问答助手 | 模型选择、知识领域、Skill 知识包 |
| CODEGEN | 代码生成 | 模型选择、编程语言、Skill 知识包 |
| TOOL | 工具节点 | TTS 语音合成等 |
| OUTPUT | 输出节点 | 输出格式配置 |

## 支持的 LLM 提供商

- **OpenAI** - GPT-4、GPT-3.5 等
- **DeepSeek** - DeepSeek-V3、DeepSeek-R1 等
- **通义千问 (Qwen)** - qwen-turbo、qwen-plus、qwen-max 等
- **智谱 AI (Zhipu)** - GLM-4、GLM-3-Turbo 等

## 核心架构设计

### 1. LangGraph4j 风格状态图引擎

```
WorkflowGraph (原始图)
    ↓
GraphBuilder.register(node) + topologicalSorter.sort()
    ↓
StateGraph (编译后的执行计划)
    ↓
stateGraph.execute(state, context) → 按顺序驱动 NodeAdapter
```

### 2. ChatClientFactory 动态工厂

```java
// 运行时根据节点配置动态创建
DynamicChatClient client = chatClientFactory.create(
    provider, model, apiUrl, apiKey, temperature, maxTokens
);
ChatResponse resp = client.chat(systemPrompt, userPrompt);
```

### 3. 模板方法模式 - LLM 节点执行器

```java
public abstract class AbstractLLMNodeExecutor extends AbstractNodeHandler {
    // 固定流程
    protected final NodeResult doExecute(NodeDefinition node, ExecutionContext context) {
        // 1. 解析配置
        // 2. 处理 inputParams
        // 3. Skill 知识包注入
        // 4. 子类 customizeSystemPrompt()
        // 5. 创建 ChatClient 调用 LLM
        // 6. 子类 postProcessResponse()
        // 7. 构建 NodeResult
    }
    
    // 子类仅需实现
    public abstract String getType();
}
```

### 4. Skill 预置知识包注入

```java
// 全量注入
skillInjector.inject(systemPrompt, "legal-assistant");

// 渐进式注入（按权重选择 Top-K）
Skill skill = skillRegistry.getSkill("code-reviewer");
String knowledge = skillInjector.getKnowledgeContent(skill.getId());
```

### 5. TTS 语音合成 - UTF-8 字节级分段

```java
// 三级分段策略
1. 按句末标点（。！？!?）分段
2. 超长句子按次级标点（，、;：）分段  
3. 仍超长则按 UTF-8 字节边界截断

// 并行合成 + WAV 合并
CompletableFuture.supplyAsync(() -> synthesize(segment), executor)
    .thenApply(audioParts -> mergeWavFiles(audioParts));
```

## 开发计划

- [x] 基础工作流编排
- [x] LangGraph4j 风格状态图引擎
- [x] ChatClientFactory 动态工厂
- [x] 模板方法模式重构 LLM 节点执行器
- [x] Skill 预置知识包机制
- [x] 多 LLM 支持（OpenAI、DeepSeek、通义千问、智谱）
- [x] TTS 语音合成（UTF-8 字节级分段、并行合成）
- [x] 实时调试 (SSE)
- [ ] 条件分支节点
- [ ] 循环节点
- [ ] 工作流导入导出
- [ ] 更多工具节点（图片生成、搜索引擎等）

## 许可证

MIT License
