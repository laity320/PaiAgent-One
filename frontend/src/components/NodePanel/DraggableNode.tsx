import type { NodeRegistryItem } from '@/constants/nodeRegistry';

interface DraggableNodeProps {
  item: NodeRegistryItem;
}

export default function DraggableNode({ item }: DraggableNodeProps) {
  const onDragStart = (e: React.DragEvent) => {
    e.dataTransfer.setData('application/paiagent-node', JSON.stringify(item));
    e.dataTransfer.effectAllowed = 'move';
  };

  return (
    <div
      draggable
      onDragStart={onDragStart}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        padding: '8px 12px',
        margin: '4px 8px',
        borderRadius: 6,
        border: '1px solid #f0f0f0',
        background: '#fafafa',
        cursor: 'grab',
        fontSize: 13,
        transition: 'all 0.2s',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.background = '#f0f0ff';
        e.currentTarget.style.borderColor = '#c7c7ff';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.background = '#fafafa';
        e.currentTarget.style.borderColor = '#f0f0f0';
      }}
    >
      <span style={{ fontSize: 16 }}>{item.icon}</span>
      <span>{item.label}</span>
    </div>
  );
}
