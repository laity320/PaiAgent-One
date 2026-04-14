import type { NodeProps } from '@xyflow/react';
import BaseNode from './BaseNode';
import type { PaiNodeData } from '@/types/node';

export default function InputNode({ data, selected }: NodeProps) {
  return (
    <BaseNode
      data={data as unknown as PaiNodeData}
      selected={selected}
      showTargetHandle={false}
      showSourceHandle={true}
    />
  );
}
