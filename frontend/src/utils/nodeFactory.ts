import type { Node } from '@xyflow/react';
import type { NodeCategory } from '@/types/node';
import type { NodeRegistryItem } from '@/constants/nodeRegistry';

let nodeCounter = 0;

export function createNode(
  registryItem: NodeRegistryItem,
  position: { x: number; y: number }
): Node {
  nodeCounter++;
  const id = `${registryItem.category}-${nodeCounter}-${Date.now()}`;

  return {
    id,
    type: registryItem.category,
    position,
    data: {
      label: registryItem.label,
      category: registryItem.category,
      icon: registryItem.icon,
      color: registryItem.color,
      config: { ...registryItem.defaultConfig },
    },
  };
}

export function createInputNode(position: { x: number; y: number }): Node {
  return {
    id: 'input-default',
    type: 'input' as NodeCategory,
    position,
    data: {
      label: '输入',
      category: 'input',
      icon: '📥',
      color: '#1677ff',
      config: {
        variableName: 'user_query',
        defaultValue: '',
      },
    },
  };
}

export function createOutputNode(position: { x: number; y: number }): Node {
  return {
    id: 'output-default',
    type: 'output' as NodeCategory,
    position,
    data: {
      label: '输出',
      category: 'output',
      icon: '📤',
      color: '#52c41a',
      config: {
        outputTemplate: '{{output}}',
        includeAudio: false,
        audioRef: '',
      },
    },
  };
}
