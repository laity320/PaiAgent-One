import { useCallback } from 'react';
import { useReactFlow } from '@xyflow/react';
import { useWorkflowStore } from '@/stores/workflowStore';
import { createNode } from '@/utils/nodeFactory';
import type { NodeRegistryItem } from '@/constants/nodeRegistry';

export function useDragToCanvas() {
  const { screenToFlowPosition } = useReactFlow();
  const addNode = useWorkflowStore((s) => s.addNode);

  const onDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  }, []);

  const onDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();

      const data = e.dataTransfer.getData('application/paiagent-node');
      if (!data) return;

      try {
        const registryItem: NodeRegistryItem = JSON.parse(data);
        const position = screenToFlowPosition({
          x: e.clientX,
          y: e.clientY,
        });
        const node = createNode(registryItem, position);
        addNode(node);
      } catch {
        // ignore invalid drop
      }
    },
    [screenToFlowPosition, addNode]
  );

  return { onDragOver, onDrop };
}
