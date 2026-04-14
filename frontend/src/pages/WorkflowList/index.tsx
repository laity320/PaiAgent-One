import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, List, Empty, message, Popconfirm, Space } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, LogoutOutlined } from '@ant-design/icons';
import { workflowApi } from '@/api/workflow';
import { useAuthStore } from '@/stores/authStore';
import type { Workflow } from '@/types/workflow';

export default function WorkflowList() {
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const fetchList = async () => {
    setLoading(true);
    try {
      const list = await workflowApi.list();
      setWorkflows(list || []);
    } catch (err: unknown) {
      message.error(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchList();
  }, []);

  const handleDelete = async (id: number) => {
    try {
      await workflowApi.delete(id);
      message.success('删除成功');
      fetchList();
    } catch (err: unknown) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div style={{ minHeight: '100vh', background: '#f5f5f5' }}>
      <div
        style={{
          height: 56,
          background: '#fff',
          borderBottom: '1px solid #e5e7eb',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 24px',
        }}
      >
        <h1 style={{ fontSize: 20, fontWeight: 700, color: '#6366f1', margin: 0 }}>
          PaiAgent
        </h1>
        <Space>
          <span style={{ color: '#666' }}>{user?.username}</span>
          <Button icon={<LogoutOutlined />} type="text" onClick={handleLogout}>
            登出
          </Button>
        </Space>
      </div>

      <div style={{ maxWidth: 960, margin: '24px auto', padding: '0 24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
          <h2 style={{ margin: 0 }}>我的工作流</h2>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => navigate('/workflows/new')}
          >
            新建工作流
          </Button>
        </div>

        {workflows.length === 0 && !loading ? (
          <Card>
            <Empty description="暂无工作流，点击上方按钮新建" />
          </Card>
        ) : (
          <List
            loading={loading}
            grid={{ gutter: 16, column: 3 }}
            dataSource={workflows}
            renderItem={(item) => (
              <List.Item>
                <Card
                  hoverable
                  title={item.name}
                  extra={
                    <Space>
                      <Button
                        type="text"
                        size="small"
                        icon={<EditOutlined />}
                        onClick={() => navigate(`/workflows/${item.id}/edit`)}
                      />
                      <Popconfirm
                        title="确定删除此工作流？"
                        onConfirm={() => handleDelete(item.id)}
                      >
                        <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                      </Popconfirm>
                    </Space>
                  }
                  onClick={() => navigate(`/workflows/${item.id}/edit`)}
                >
                  <p style={{ color: '#999', fontSize: 13 }}>
                    {item.description || '无描述'}
                  </p>
                  <p style={{ color: '#bbb', fontSize: 12 }}>
                    更新于 {item.updatedAt}
                  </p>
                </Card>
              </List.Item>
            )}
          />
        )}
      </div>
    </div>
  );
}
