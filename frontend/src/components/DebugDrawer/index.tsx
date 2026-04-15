import { Drawer, Input, Button, Steps, Tag, Spin, Empty, Progress } from 'antd';
import { PlayCircleOutlined } from '@ant-design/icons';
import { useUIStore } from '@/stores/uiStore';
import { useDebugStore } from '@/stores/debugStore';
import { useWorkflowStore } from '@/stores/workflowStore';
import { useAuthStore } from '@/stores/authStore';
import AudioPlayer from '@/components/common/AudioPlayer';
import type { Node, Edge } from '@xyflow/react';

/** 按边的连接关系拓扑排序，返回排序后的节点列表 */
function topoSort(nodes: Node[], edges: Edge[]): Node[] {
  const inDegree = new Map<string, number>();
  const adjList = new Map<string, string[]>();

  nodes.forEach((n) => {
    inDegree.set(n.id, 0);
    adjList.set(n.id, []);
  });

  edges.forEach((e) => {
    if (e.source && e.target) {
      adjList.get(e.source)?.push(e.target);
      inDegree.set(e.target, (inDegree.get(e.target) ?? 0) + 1);
    }
  });

  const queue = nodes.filter((n) => (inDegree.get(n.id) ?? 0) === 0);
  const result: Node[] = [];

  while (queue.length > 0) {
    const node = queue.shift()!;
    result.push(node);
    for (const nextId of adjList.get(node.id) ?? []) {
      const deg = (inDegree.get(nextId) ?? 1) - 1;
      inDegree.set(nextId, deg);
      if (deg === 0) {
        const nextNode = nodes.find((n) => n.id === nextId);
        if (nextNode) queue.push(nextNode);
      }
    }
  }

  // Append any nodes not reached (disconnected)
  nodes.forEach((n) => {
    if (!result.find((r) => r.id === n.id)) result.push(n);
  });

  return result;
}

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

    // Initialize all nodes as pending, sorted by execution order
    const sortedNodes = topoSort(nodes, edges);
    sortedNodes.forEach((n) => {
      addNodeResult({
        nodeId: n.id,
        nodeName: (n.data as Record<string, unknown>).label as string,
        status: 'pending',
        inputs: null,
        output: null,
        outputType: null,
        duration: 0,
        progress: null,
        progressText: null,
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
              case 'node_progress':
                updateNodeResult(data.nodeId, {
                  status: 'running',
                  progress: data.progress,
                  progressText: data.progressText,
                });
                break;
              case 'node_complete':
                updateNodeResult(data.nodeId, {
                  status: 'success',
                  inputs: data.inputs,
                  output: data.output,
                  outputType: data.outputType,
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
                  {r.duration > 0 && (
                    <span style={{ marginLeft: 8, color: '#999', fontSize: 11 }}>{r.duration}ms</span>
                  )}
                </span>
              ),
              description: r.status === 'success' ? (
                <div style={{ marginTop: 4 }}>
                  {/* 输入参数 */}
                  {r.inputs && Object.keys(r.inputs).length > 0 && (
                    <div style={{ marginBottom: 6 }}>
                      <div style={{ fontSize: 11, color: '#999', marginBottom: 2 }}>输入:</div>
                      <div
                        style={{
                          fontSize: 12,
                          color: '#666',
                          maxHeight: 60,
                          overflow: 'auto',
                          background: '#f0f5ff',
                          padding: 6,
                          borderRadius: 4,
                        }}
                      >
                        {Object.entries(r.inputs).map(([key, value]) => (
                          <div key={key} style={{ marginBottom: 2 }}>
                            <span style={{ color: '#1890ff' }}>{key}:</span>{' '}
                            <span style={{ wordBreak: 'break-all' }}>
                              {typeof value === 'string' && value.length > 100
                                ? value.substring(0, 100) + '...'
                                : String(value)}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                  {/* 输出参数 - TTS节点不显示输出（由输出节点统一展示） */}
                  {r.output && r.outputType !== 'audio' && (
                    <div>
                      <div style={{ fontSize: 11, color: '#999', marginBottom: 2 }}>输出:</div>
                      <div
                        style={{
                          fontSize: 12,
                          color: '#666',
                          maxHeight: 80,
                          overflow: 'auto',
                          background: '#f6ffed',
                          padding: 6,
                          borderRadius: 4,
                          wordBreak: 'break-all',
                        }}
                      >
                        {r.output.length > 300 ? r.output.substring(0, 300) + '...' : r.output}
                      </div>
                    </div>
                  )}
                </div>
              ) : r.status === 'running' ? (
                <div style={{ marginTop: 4 }}>
                  {/* 进度条 */}
                  {r.progress != null && (
                    <div style={{ marginBottom: 4 }}>
                      <Progress
                        percent={r.progress}
                        size="small"
                        status="active"
                        format={() => r.progressText || `${r.progress}%`}
                      />
                    </div>
                  )}
                  {/* 执行中文案 */}
                  {r.progress == null && (
                    <div style={{ fontSize: 12, color: '#1890ff' }}>执行中...</div>
                  )}
                </div>
              ) : r.status === 'error' ? (
                <div
                  style={{
                    fontSize: 12,
                    color: '#ff4d4f',
                    background: '#fff2f0',
                    padding: 6,
                    borderRadius: 4,
                    marginTop: 4,
                  }}
                >
                  {r.output}
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
