import { createBrowserRouter, Navigate } from 'react-router-dom';
import AuthGuard from './AuthGuard';
import Login from '@/pages/Login';
import WorkflowList from '@/pages/WorkflowList';
import WorkflowEditor from '@/pages/WorkflowEditor';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <Login />,
  },
  {
    path: '/',
    element: (
      <AuthGuard>
        <Navigate to="/workflows" replace />
      </AuthGuard>
    ),
  },
  {
    path: '/workflows',
    element: (
      <AuthGuard>
        <WorkflowList />
      </AuthGuard>
    ),
  },
  {
    path: '/workflows/new',
    element: (
      <AuthGuard>
        <WorkflowEditor />
      </AuthGuard>
    ),
  },
  {
    path: '/workflows/:id/edit',
    element: (
      <AuthGuard>
        <WorkflowEditor />
      </AuthGuard>
    ),
  },
  {
    path: '*',
    element: <Navigate to="/workflows" replace />,
  },
]);
