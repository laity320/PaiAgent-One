import type { Node, Edge } from '@xyflow/react';

export interface Workflow {
  id: number;
  name: string;
  description?: string;
  graphJson: {
    nodes: Node[];
    edges: Edge[];
  };
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowListItem {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}
