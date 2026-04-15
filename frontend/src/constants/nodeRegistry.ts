import type { NodeCategory, LLMProvider } from '@/types/node';

export interface NodeRegistryItem {
  type: string;
  category: NodeCategory;
  label: string;
  icon: string;
  color: string;
  description: string;
  defaultConfig: Record<string, unknown>;
}

export const NODE_REGISTRY: NodeRegistryItem[] = [
  // --- LLM Nodes ---
  {
    type: 'llm',
    category: 'llm',
    label: 'DeepSeek',
    icon: '🔮',
    color: '#722ed1',
    description: 'DeepSeek 大模型',
    defaultConfig: {
      provider: 'deepseek' as LLMProvider,
      apiUrl: 'https://api.deepseek.com/v1/chat/completions',
      apiKey: '',
      model: 'deepseek-chat',
      temperature: 0.7,
      inputParams: [] as { name: string; type: 'input' | 'ref'; value: string }[],
      outputParam: { name: 'output', type: 'string', description: '' },
      userPrompt: `角色
你是一位专业的广播节目编辑，负责制作一档名为"AI电台"的节目。你的任务是将用户提供的原始内容改编为适合单口相声播客节目的逐字稿。

# 任务
将原始内容分解为若干主题或问题，确保每段对话涵盖关键点，并自然过渡。

# 注意点
确保对话语言口语化、易懂。
对于专业术语或复杂概念，使用简单明了的语言进行解释，使听众更易理解。
保持对话节奏轻松、有趣，并加入适当的幽默和互动，以提高听众的参与感。
注意：我会直接将你生成的内容朗读出来，不要输出口播稿以外的东西，不要带格式，

# 示例
欢迎收听AI电台，今天咱们的节目一定让你们大开眼界！
没错！今天的主题绝对精彩，快搬小板凳听好哦！
那么，今天我们要讨论的内容是……

# 原始内容：{{input}}`,
    },
  },
  {
    type: 'llm',
    category: 'llm',
    label: '通义千问',
    icon: '🌟',
    color: '#722ed1',
    description: '阿里通义千问',
    defaultConfig: {
      provider: 'tongyi' as LLMProvider,
      apiUrl: 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation',
      apiKey: '',
      model: 'qwen-turbo',
      temperature: 0.7,
      inputParams: [] as { name: string; type: 'input' | 'ref'; value: string }[],
      outputParam: { name: 'output', type: 'string', description: '' },
      userPrompt: '',
    },
  },
  {
    type: 'llm',
    category: 'llm',
    label: 'AI Ping',
    icon: '🤖',
    color: '#722ed1',
    description: 'AI Ping 模型',
    defaultConfig: {
      provider: 'openai' as LLMProvider,
      model: 'gpt-4o-mini',
      temperature: 0.7,
      maxTokens: 2048,
      systemPrompt: '',
      userPromptTemplate: '{{input.output}}',
    },
  },
  {
    type: 'llm',
    category: 'llm',
    label: '智谱',
    icon: '🧠',
    color: '#722ed1',
    description: '智谱 GLM 模型',
    defaultConfig: {
      provider: 'zhipu' as LLMProvider,
      model: 'glm-4-flash',
      temperature: 0.7,
      maxTokens: 2048,
      systemPrompt: '',
      userPromptTemplate: '{{input.output}}',
    },
  },
  // --- Tool Nodes ---
  {
    type: 'tool',
    category: 'tool',
    label: '超拟人音频合成',
    icon: '🎙️',
    color: '#fa8c16',
    description: '超拟人语音合成 TTS',
    defaultConfig: {
      toolType: 'tts',
      apiKey: '',
      model: 'qwen3-tts-flash',
      inputParams: {
        text: { type: 'input', value: '' },
        voice: 'Cherry',
        languageType: 'Auto',
      },
      outputParam: { name: 'voice_url', type: 'string', description: '' },
      toolConfig: {
        format: 'mp3',
      },
    },
  },
];

export const NODE_CATEGORIES = [
  {
    key: 'llm',
    label: '大模型节点',
    icon: '🤖',
    items: NODE_REGISTRY.filter((n) => n.category === 'llm'),
  },
  {
    key: 'tool',
    label: '工具节点',
    icon: '🔧',
    items: NODE_REGISTRY.filter((n) => n.category === 'tool'),
  },
];
