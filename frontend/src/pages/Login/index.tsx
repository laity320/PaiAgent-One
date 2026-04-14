import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, message, Tabs } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { authApi } from '@/api/auth';
import { useAuthStore } from '@/stores/authStore';

export default function Login() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const handleSubmit = async (values: { username: string; password: string }, isRegister: boolean) => {
    setLoading(true);
    try {
      const res = isRegister
        ? await authApi.register(values)
        : await authApi.login(values);
      setAuth(res.token, { id: res.userId, username: res.username, role: res.role });
      message.success(isRegister ? '注册成功' : '登录成功');
      navigate('/workflows');
    } catch (err: unknown) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setLoading(false);
    }
  };

  const renderForm = (isRegister: boolean) => (
    <Form
      size="large"
      onFinish={(values) => handleSubmit(values, isRegister)}
      autoComplete="off"
    >
      <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
        <Input prefix={<UserOutlined />} placeholder="用户名" />
      </Form.Item>
      <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
        <Input.Password prefix={<LockOutlined />} placeholder="密码" />
      </Form.Item>
      <Form.Item>
        <Button type="primary" htmlType="submit" loading={loading} block>
          {isRegister ? '注册' : '登录'}
        </Button>
      </Form.Item>
    </Form>
  );

  return (
    <div
      style={{
        height: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      }}
    >
      <Card
        style={{ width: 400, borderRadius: 12, boxShadow: '0 8px 32px rgba(0,0,0,0.15)' }}
      >
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <h1 style={{ fontSize: 28, fontWeight: 700, color: '#6366f1', margin: 0 }}>
            PaiAgent
          </h1>
          <p style={{ color: '#999', marginTop: 4 }}>AI Agent Workflow Platform</p>
        </div>
        <Tabs
          centered
          items={[
            { key: 'login', label: '登录', children: renderForm(false) },
            { key: 'register', label: '注册', children: renderForm(true) },
          ]}
        />
      </Card>
    </div>
  );
}
