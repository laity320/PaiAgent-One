import type { NodeProps } from '@xyflow/react';
import BaseNode from './BaseNode';
import type { PaiNodeData } from '@/types/node';

export default function OutputNode({ data, selected }: NodeProps) {
  return (
    <BaseNode
      data={data as unknown as PaiNodeData}
      selected={selected}
      showTargetHandle={true}
      showSourceHandle={false}
    />
  );
}
