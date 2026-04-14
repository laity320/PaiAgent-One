export type NodeCategory = 'input' | 'llm' | 'tool' | 'output';

export type LLMProvider = 'deepseek' | 'tongyi' | 'aiping' | 'zhipu' | 'openai';

export interface PaiNodeData extends Record<string, unknown> {
  label: string;
  category: NodeCategory;
  icon?: string;
  color?: string;
  config: Record<string, unknown>;
}

export interface InputNodeData extends PaiNodeData {
  category: 'input';
  config: {
    variableName: string;
    defaultValue: string;
  };
}

export interface LLMNodeData extends PaiNodeData {
  category: 'llm';
  config: {
    provider: LLMProvider;
    model: string;
    temperature: number;
    maxTokens: number;
    systemPrompt: string;
    userPromptTemplate: string;
  };
}

export interface ToolNodeData extends PaiNodeData {
  category: 'tool';
  config: {
    toolType: string;
    toolConfig: Record<string, unknown>;
  };
}

export interface OutputNodeData extends PaiNodeData {
  category: 'output';
  config: {
    outputTemplate: string;
    includeAudio: boolean;
    audioRef: string;
  };
}
