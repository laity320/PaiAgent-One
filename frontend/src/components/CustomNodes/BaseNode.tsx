import type { ReactNode } from 'react';
import { Handle, Position } from '@xyflow/react';
import type { PaiNodeData, NodeCategory } from '@/types/node';

interface BaseNodeProps {
  data: PaiNodeData;
  selected?: boolean;
  children?: ReactNode;
  showSourceHandle?: boolean;
  showTargetHandle?: boolean;
}

const categoryColors: Record<NodeCategory, string> = {
  input: '#1677ff',
  llm: '#722ed1',
  tool: '#fa8c16',
  output: '#52c41a',
};

export default function BaseNode({
  data,
  selected,
  children,
  showSourceHandle = true,
  showTargetHandle = true,
}: BaseNodeProps) {
  const color = data.color || categoryColors[data.category];

  return (
    <div
      style={{
        background: '#fff',
        borderRadius: 8,
        border: selected ? `2px solid ${color}` : '1px solid #e5e7eb',
        boxShadow: selected
          ? `0 0 0 2px ${color}33`
          : '0 1px 4px rgba(0,0,0,0.08)',
        minWidth: 150,
        transition: 'all 0.2s',
      }}
    >
      {/* Color bar */}
      <div
        style={{
          height: 4,
          background: color,
          borderRadius: '8px 8px 0 0',
        }}
      />

      {/* Content */}
      <div style={{ padding: '10px 14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: children ? 6 : 0 }}>
          <span style={{ fontSize: 16 }}>{data.icon}</span>
          <span style={{ fontWeight: 600, fontSize: 13, color: '#1f2937' }}>
            {data.label}
          </span>
        </div>
        {children}
      </div>

      {/* Handles */}
      {showTargetHandle && (
        <Handle
          type="target"
          position={Position.Top}
          style={{
            width: 10,
            height: 10,
            background: color,
            border: '2px solid #fff',
          }}
        />
      )}
      {showSourceHandle && (
        <Handle
          type="source"
          position={Position.Bottom}
          style={{
            width: 10,
            height: 10,
            background: color,
            border: '2px solid #fff',
          }}
        />
      )}
    </div>
  );
}
