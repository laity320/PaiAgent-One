import type { NodeTypes } from '@xyflow/react';
import InputNode from './InputNode';
import LLMNode from './LLMNode';
import ToolNode from './ToolNode';
import OutputNode from './OutputNode';

export const nodeTypes: NodeTypes = {
  input: InputNode,
  llm: LLMNode,
  tool: ToolNode,
  output: OutputNode,
};
