import { useState } from 'react';
import type { NodeRegistryItem } from '@/constants/nodeRegistry';
import DraggableNode from './DraggableNode';

interface NodeCategoryProps {
  label: string;
  icon: string;
  items: NodeRegistryItem[];
}

export default function NodeCategory({ label, icon, items }: NodeCategoryProps) {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div style={{ marginBottom: 8 }}>
      <div
        onClick={() => setCollapsed(!collapsed)}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          padding: '8px 12px',
          cursor: 'pointer',
          fontWeight: 600,
          fontSize: 13,
          color: '#374151',
          userSelect: 'none',
        }}
      >
        <span style={{ transform: collapsed ? 'rotate(-90deg)' : 'rotate(0)', transition: 'transform 0.2s' }}>
          ▾
        </span>
        <span>{icon}</span>
        <span>{label}</span>
      </div>
      {!collapsed && items.map((item) => (
        <DraggableNode key={item.label} item={item} />
      ))}
    </div>
  );
}
