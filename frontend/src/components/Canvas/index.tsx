import { useCallback } from 'react';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  type NodeMouseHandler,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { nodeTypes } from '@/components/CustomNodes';
import { useWorkflowStore } from '@/stores/workflowStore';
import { useUIStore } from '@/stores/uiStore';
import { useDragToCanvas } from '@/hooks/useDragToCanvas';

export default function Canvas() {
  const { nodes, edges, onNodesChange, onEdgesChange, onConnect } = useWorkflowStore();
  const selectNode = useUIStore((s) => s.selectNode);
  const { onDragOver, onDrop } = useDragToCanvas();

  const onNodeClick: NodeMouseHandler = useCallback(
    (_event, node) => {
      selectNode(node.id);
    },
    [selectNode]
  );

  const onPaneClick = useCallback(() => {
    selectNode(null);
  }, [selectNode]);

  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onConnect={onConnect}
      onNodeClick={onNodeClick}
      onPaneClick={onPaneClick}
      onDragOver={onDragOver}
      onDrop={onDrop}
      nodeTypes={nodeTypes}
      fitView
      snapToGrid
      snapGrid={[15, 15]}
      deleteKeyCode="Delete"
      style={{ background: '#f8f9fa' }}
    >
      <Background color="#e5e7eb" gap={20} />
      <Controls
        position="bottom-left"
        style={{ marginLeft: 8, marginBottom: 8 }}
      />
      <MiniMap
        position="bottom-right"
        style={{ marginRight: 8, marginBottom: 8 }}
        nodeColor={(node) => {
          const data = node.data as Record<string, unknown>;
          return (data.color as string) || '#999';
        }}
      />
    </ReactFlow>
  );
}
