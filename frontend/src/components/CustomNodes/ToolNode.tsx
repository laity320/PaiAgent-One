import type { NodeProps } from '@xyflow/react';
import BaseNode from './BaseNode';
import type { PaiNodeData } from '@/types/node';

export default function ToolNode({ data, selected }: NodeProps) {
  const nodeData = data as unknown as PaiNodeData;
  return (
    <BaseNode data={nodeData} selected={selected}>
      <div style={{ fontSize: 11, color: '#999' }}>
        {(nodeData.config as Record<string, unknown>).toolType as string}
      </div>
    </BaseNode>
  );
}
