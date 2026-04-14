import { Drawer, Input, Button, Steps, Tag, Spin, Empty } from 'antd';
import { PlayCircleOutlined } from '@ant-design/icons';
import { useUIStore } from '@/stores/uiStore';
import { useDebugStore } from '@/stores/debugStore';
import { useWorkflowStore } from '@/stores/workflowStore';
import { useAuthStore } from '@/stores/authStore';
import AudioPlayer from '@/components/common/AudioPlayer';

export default function DebugDrawer() {
  const { isDebugDrawerOpen, setDebugDrawerOpen } = useUIStore();
  const {
    isRunning,
    inputText,
    nodeResults,
    finalOutput,
    error,
    setInputText,
    setRunning,
    addNodeResult,
    updateNodeResult,
    setFinalOutput,
    setError,
    reset,
  } = useDebugStore();
  const { workflowId, nodes, edges } = useWorkflowStore();
  const token = useAuthStore((s) => s.token);

  const handleRun = async () => {
    if (!inputText.trim()) return;
    reset();
    setRunning(true);

    // Initialize all nodes as pending
    nodes.forEach((n) => {
      addNodeResult({
        nodeId: n.id,
        nodeName: (n.data as Record<string, unknown>).label as string,
        status: 'pending',
        output: null,
        duration: 0,
      });
    });

    try {
      const response = await fetch('/api/execution/debug/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({
          workflowId: workflowId || undefined,
          graphJson: { nodes, edges },
          input: inputText,
        }),
      });

      if (!response.ok) {
        throw new Error(`请求失败: ${response.status}`);
      }

      const reader = response.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // SSE events are separated by double newlines
        const eventBlocks = buffer.split('\n\n');
        buffer = eventBlocks.pop() || '';

        for (const block of eventBlocks) {
          if (!block.trim()) continue;

          let eventName = '';
          let eventData = '';

          for (const line of block.split('\n')) {
            if (line.startsWith('event:')) {
              eventName = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
              eventData += line.slice(5).trim();
            }
          }

          if (!eventName || !eventData) continue;

          try {
            const data = JSON.parse(eventData);

            switch (eventName) {
              case 'node_start':
                updateNodeResult(data.nodeId, { status: 'running' });
                break;
              case 'node_complete':
                updateNodeResult(data.nodeId, {
                  status: 'success',
                  output: data.output,
                  duration: data.durationMs,
                });
                break;
              case 'node_error':
                updateNodeResult(data.nodeId, { status: 'error', output: data.error });
                break;
              case 'execution_complete':
                if (data.finalOutput) {
                  setFinalOutput(data.finalOutput);
                }
                setRunning(false);
                return;
            }
          } catch {
            // ignore JSON parse errors
          }
        }
      }

      setRunning(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : '调试连接中断');
      setRunning(false);
    }
  };

  const statusColor = (status: string) => {
    switch (status) {
      case 'running': return 'processing';
      case 'success': return 'success';
      case 'error': return 'error';
      default: return 'default';
    }
  };

  return (
    <Drawer
      title="工作流调试"
      open={isDebugDrawerOpen}
      onClose={() => setDebugDrawerOpen(false)}
      width={420}
      mask={false}
    >
      <div style={{ marginBottom: 16 }}>
        <div style={{ fontWeight: 600, marginBottom: 8 }}>输入文本</div>
        <Input.TextArea
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder="请输入测试文本..."
          rows={4}
          disabled={isRunning}
        />
        <Button
          type="primary"
          icon={<PlayCircleOutlined />}
          onClick={handleRun}
          loading={isRunning}
          style={{ marginTop: 8, width: '100%' }}
          disabled={!inputText.trim()}
        >
          {isRunning ? '执行中...' : '开始执行'}
        </Button>
      </div>

      {error && (
        <div style={{ padding: 8, background: '#fff2f0', borderRadius: 6, marginBottom: 16, color: '#ff4d4f' }}>
          {error}
        </div>
      )}

      {nodeResults.length > 0 && (
        <div>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>执行进度</div>
          <Steps
            direction="vertical"
            size="small"
            current={nodeResults.findIndex((r) => r.status === 'running')}
            items={nodeResults.map((r) => ({
              title: (
                <span>
                  {r.nodeName}
                  <Tag color={statusColor(r.status)} style={{ marginLeft: 8 }}>
                    {r.status === 'running' ? <Spin size="small" /> : r.status}
                  </Tag>
                </span>
              ),
              description: r.output ? (
                <div
                  style={{
                    fontSize: 12,
                    color: '#666',
                    maxHeight: 80,
                    overflow: 'auto',
                    background: '#fafafa',
                    padding: 6,
                    borderRadius: 4,
                    marginTop: 4,
                  }}
                >
                  {r.output}
                  {r.duration > 0 && (
                    <span style={{ float: 'right', color: '#bbb' }}>{r.duration}ms</span>
                  )}
                </div>
              ) : null,
              status:
                r.status === 'success'
                  ? 'finish'
                  : r.status === 'error'
                    ? 'error'
                    : r.status === 'running'
                      ? 'process'
                      : 'wait',
            }))}
          />
        </div>
      )}

      {finalOutput && (
        <div style={{ marginTop: 16 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>最终输出</div>
          {finalOutput.type === 'audio' ? (
            <AudioPlayer src={finalOutput.content} />
          ) : (
            <div
              style={{
                padding: 12,
                background: '#f6ffed',
                borderRadius: 8,
                fontSize: 13,
                whiteSpace: 'pre-wrap',
              }}
            >
              {finalOutput.content}
            </div>
          )}
        </div>
      )}

      {nodeResults.length === 0 && !isRunning && (
        <Empty description="输入文本后点击执行开始调试" />
      )}
    </Drawer>
  );
}
