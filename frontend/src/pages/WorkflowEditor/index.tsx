import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { ReactFlowProvider } from '@xyflow/react';
import { message } from 'antd';
import EditorLayout from '@/layouts/EditorLayout';
import Header from '@/components/Header';
import NodePanel from '@/components/NodePanel';
import Canvas from '@/components/Canvas';
import ConfigPanel from '@/components/ConfigPanel';
import DebugDrawer from '@/components/DebugDrawer';
import { useWorkflowStore } from '@/stores/workflowStore';
import { useUIStore } from '@/stores/uiStore';
import { workflowApi } from '@/api/workflow';
import { createInputNode, createOutputNode } from '@/utils/nodeFactory';
import type { Node, Edge } from '@xyflow/react';

function WorkflowEditorInner() {
  const { id } = useParams<{ id: string }>();
  const { loadWorkflow, workflowId } = useWorkflowStore();
  const isConfigPanelVisible = useUIStore((s) => s.isConfigPanelVisible);

  useEffect(() => {
    if (id) {
      const workflowIdNum = parseInt(id, 10);
      if (workflowIdNum !== workflowId) {
        workflowApi
          .get(workflowIdNum)
          .then((wf) => {
            const graph = wf.graphJson || { nodes: [], edges: [] };
            loadWorkflow(
              wf.id,
              wf.name,
              graph.nodes as Node[],
              graph.edges as Edge[]
            );
          })
          .catch((err: unknown) => {
            message.error(err instanceof Error ? err.message : '加载工作流失败');
          });
      }
    } else {
      const inputNode = createInputNode({ x: 300, y: 50 });
      const outputNode = createOutputNode({ x: 300, y: 450 });
      loadWorkflow(null, '未命名工作流', [inputNode, outputNode], []);
    }
  }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <EditorLayout
      header={<Header />}
      nodePanel={<NodePanel />}
      canvas={<Canvas />}
      configPanel={<ConfigPanel />}
      debugDrawer={<DebugDrawer />}
      showConfigPanel={isConfigPanelVisible}
    />
  );
}

export default function WorkflowEditor() {
  return (
    <ReactFlowProvider>
      <WorkflowEditorInner />
    </ReactFlowProvider>
  );
}
