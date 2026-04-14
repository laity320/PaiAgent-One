import { useNavigate } from 'react-router-dom';
import { Button, Space, Input, message, Tooltip } from 'antd';
import {
  PlusOutlined,
  FolderOpenOutlined,
  SaveOutlined,
  BugOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '@/stores/authStore';
import { useWorkflowStore } from '@/stores/workflowStore';
import { useUIStore } from '@/stores/uiStore';
import { workflowApi } from '@/api/workflow';

export default function Header() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { workflowId, workflowName, nodes, edges, isDirty, setWorkflowName, loadWorkflow } =
    useWorkflowStore();
  const { toggleDebugDrawer } = useUIStore();

  const handleNew = () => {
    if (isDirty && !confirm('有未保存的更改，确定新建？')) return;
    loadWorkflow(null, '未命名工作流', [], []);
    navigate('/workflows/new');
  };

  const handleLoad = () => {
    navigate('/workflows');
  };

  const handleSave = async () => {
    const graphJson = { nodes, edges };
    try {
      if (workflowId) {
        await workflowApi.update(workflowId, { name: workflowName, graphJson });
        message.success('保存成功');
      } else {
        const id = await workflowApi.create({ name: workflowName, graphJson });
        loadWorkflow(id, workflowName, nodes, edges);
        message.success('创建成功');
        navigate(`/workflows/${id}/edit`, { replace: true });
      }
    } catch (err: unknown) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div
      style={{
        height: 56,
        background: '#fff',
        borderBottom: '1px solid #e5e7eb',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 16px',
        flexShrink: 0,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <h1
          style={{
            fontSize: 20,
            fontWeight: 700,
            color: '#6366f1',
            margin: 0,
            cursor: 'pointer',
          }}
          onClick={() => navigate('/workflows')}
        >
          PaiAgent
        </h1>
        <Input
          value={workflowName}
          onChange={(e) => setWorkflowName(e.target.value)}
          variant="borderless"
          style={{ width: 160, fontSize: 14, color: '#666' }}
        />
      </div>

      <Space>
        <Tooltip title="新建">
          <Button icon={<PlusOutlined />} onClick={handleNew}>
            新建
          </Button>
        </Tooltip>
        <Tooltip title="加载">
          <Button icon={<FolderOpenOutlined />} onClick={handleLoad}>
            加载
          </Button>
        </Tooltip>
        <Button type="primary" icon={<SaveOutlined />} onClick={handleSave}>
          保存
        </Button>
        <Button
          type="primary"
          icon={<BugOutlined />}
          onClick={toggleDebugDrawer}
          style={{ background: '#10b981' }}
        >
          调试
        </Button>
      </Space>

      <Space>
        <UserOutlined />
        <span>{user?.username}</span>
        <Button type="text" icon={<LogoutOutlined />} onClick={handleLogout}>
          登出
        </Button>
      </Space>
    </div>
  );
}
