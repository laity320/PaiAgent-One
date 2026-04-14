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
      model: 'deepseek-chat',
      temperature: 0.7,
      maxTokens: 2048,
      systemPrompt: '',
      userPromptTemplate: '{{input.output}}',
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
      model: 'qwen-turbo',
      temperature: 0.7,
      maxTokens: 2048,
      systemPrompt: '',
      userPromptTemplate: '{{input.output}}',
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
      toolConfig: {
        voice: 'xiaoyun',
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
