import { NODE_CATEGORIES } from '@/constants/nodeRegistry';
import NodeCategory from './NodeCategory';

export default function NodePanel() {
  return (
    <div style={{ padding: '12px 0' }}>
      <div
        style={{
          padding: '0 12px 8px',
          fontWeight: 700,
          fontSize: 14,
          color: '#1f2937',
          borderBottom: '1px solid #f0f0f0',
          marginBottom: 8,
        }}
      >
        节点库
      </div>
      {NODE_CATEGORIES.map((cat) => (
        <NodeCategory
          key={cat.key}
          label={cat.label}
          icon={cat.icon}
          items={cat.items}
        />
      ))}
      <div
        style={{
          margin: '16px 12px',
          padding: '10px',
          borderRadius: 8,
          background: 'linear-gradient(135deg, #f0f0ff, #e8f4ff)',
          textAlign: 'center',
          fontSize: 12,
          color: '#888',
        }}
      >
        拖拽节点到画布中使用
      </div>
    </div>
  );
}
