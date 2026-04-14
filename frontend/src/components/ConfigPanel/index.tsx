import { useEffect, useState } from 'react';
import { Form, Input, Select, InputNumber, Button, Divider, message } from 'antd';
import { useWorkflowStore } from '@/stores/workflowStore';
import { useUIStore } from '@/stores/uiStore';
import type { PaiNodeData } from '@/types/node';

export default function ConfigPanel() {
  const selectedNodeId = useUIStore((s) => s.selectedNodeId);
  const { nodes, updateNodeData } = useWorkflowStore();
  const [form] = Form.useForm();

  const selectedNode = nodes.find((n) => n.id === selectedNodeId);
  const nodeData = selectedNode?.data as PaiNodeData | undefined;

  const [localConfig, setLocalConfig] = useState<Record<string, unknown>>({});

  useEffect(() => {
    if (nodeData) {
      setLocalConfig(nodeData.config || {});
      form.resetFields();
    }
  }, [selectedNodeId, nodeData, form]);

  if (!selectedNode || !nodeData) {
    return (
      <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>
        点击画布中的节点进行配置
      </div>
    );
  }

  const handleSave = () => {
    if (selectedNodeId) {
      updateNodeData(selectedNodeId, { config: localConfig });
      message.success('配置已保存');
    }
  };

  const updateConfig = (key: string, value: unknown) => {
    setLocalConfig((prev) => ({ ...prev, [key]: value }));
  };

  const renderConfigFields = () => {
    switch (nodeData.category) {
      case 'input':
        return (
          <>
            <Form.Item label="变量名">
              <Input
                value={localConfig.variableName as string}
                onChange={(e) => updateConfig('variableName', e.target.value)}
              />
            </Form.Item>
            <Form.Item label="默认值">
              <Input.TextArea
                value={localConfig.defaultValue as string}
                onChange={(e) => updateConfig('defaultValue', e.target.value)}
                rows={3}
              />
            </Form.Item>
          </>
        );

      case 'llm':
        return (
          <>
            <Form.Item label="模型提供商">
              <Select
                value={localConfig.provider as string}
                onChange={(v) => updateConfig('provider', v)}
                options={[
                  { value: 'deepseek', label: 'DeepSeek' },
                  { value: 'tongyi', label: '通义千问' },
                  { value: 'openai', label: 'OpenAI' },
                  { value: 'zhipu', label: '智谱' },
                ]}
              />
            </Form.Item>
            <Form.Item label="模型名称">
              <Input
                value={localConfig.model as string}
                onChange={(e) => updateConfig('model', e.target.value)}
              />
            </Form.Item>
            <Form.Item label="温度">
              <InputNumber
                min={0}
                max={2}
                step={0.1}
                value={localConfig.temperature as number}
                onChange={(v) => updateConfig('temperature', v)}
                style={{ width: '100%' }}
              />
            </Form.Item>
            <Form.Item label="最大 Tokens">
              <InputNumber
                min={1}
                max={32000}
                value={localConfig.maxTokens as number}
                onChange={(v) => updateConfig('maxTokens', v)}
                style={{ width: '100%' }}
              />
            </Form.Item>
            <Form.Item label="系统提示词">
              <Input.TextArea
                value={localConfig.systemPrompt as string}
                onChange={(e) => updateConfig('systemPrompt', e.target.value)}
                rows={3}
                placeholder="可选的系统角色提示"
              />
            </Form.Item>
            <Form.Item label="用户提示模板">
              <Input.TextArea
                value={localConfig.userPromptTemplate as string}
                onChange={(e) => updateConfig('userPromptTemplate', e.target.value)}
                rows={3}
                placeholder="使用 {{nodeId.output}} 引用上游节点输出"
              />
            </Form.Item>
          </>
        );

      case 'tool':
        return (
          <>
            <Form.Item label="工具类型">
              <Input value={localConfig.toolType as string} disabled />
            </Form.Item>
            {localConfig.toolType === 'tts' && (
              <>
                <Form.Item label="发音人">
                  <Select
                    value={(localConfig.toolConfig as Record<string, unknown>)?.voice as string}
                    onChange={(v) =>
                      updateConfig('toolConfig', {
                        ...(localConfig.toolConfig as Record<string, unknown>),
                        voice: v,
                      })
                    }
                    options={[
                      { value: 'xiaoyun', label: '小云 (女声)' },
                      { value: 'xiaogang', label: '小刚 (男声)' },
                      { value: 'xiaomeng', label: '小梦 (女声)' },
                    ]}
                  />
                </Form.Item>
                <Form.Item label="音频格式">
                  <Select
                    value={(localConfig.toolConfig as Record<string, unknown>)?.format as string}
                    onChange={(v) =>
                      updateConfig('toolConfig', {
                        ...(localConfig.toolConfig as Record<string, unknown>),
                        format: v,
                      })
                    }
                    options={[
                      { value: 'mp3', label: 'MP3' },
                      { value: 'wav', label: 'WAV' },
                    ]}
                  />
                </Form.Item>
              </>
            )}
          </>
        );

      case 'output':
        return (
          <>
            <Form.Item label="输出配置">
              <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                <Select defaultValue="output" style={{ width: 80 }} disabled />
                <Select defaultValue="ref" style={{ width: 80 }}>
                  <Select.Option value="ref">引用</Select.Option>
                </Select>
                <Select
                  style={{ flex: 1 }}
                  value={localConfig.audioRef as string}
                  onChange={(v) => updateConfig('audioRef', v)}
                  placeholder="选择引用节点"
                >
                  {nodes
                    .filter((n) => n.id !== selectedNodeId)
                    .map((n) => (
                      <Select.Option key={n.id} value={`${(n.data as PaiNodeData).label}.audioUrl`}>
                        {(n.data as PaiNodeData).label}.audioUrl
                      </Select.Option>
                    ))}
                </Select>
              </div>
            </Form.Item>
            <Form.Item label="回答内容配置">
              <Input.TextArea
                value={localConfig.outputTemplate as string}
                onChange={(e) => updateConfig('outputTemplate', e.target.value)}
                rows={4}
                placeholder="{{output}}"
              />
            </Form.Item>
            <div style={{ fontSize: 12, color: '#faad14', marginBottom: 12 }}>
              提示: 使用 {'{{参数名}}'} 引用上面定义的参数
            </div>
          </>
        );

      default:
        return null;
    }
  };

  return (
    <div style={{ padding: 16 }}>
      <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 16 }}>节点配置</div>

      <Form layout="vertical" form={form} size="small">
        <Form.Item label="节点 ID">
          <Input value={selectedNode.id} disabled />
        </Form.Item>
        <Form.Item label="节点类型">
          <Input value={nodeData.category} disabled />
        </Form.Item>

        <Divider style={{ margin: '12px 0' }} />

        {renderConfigFields()}

        <Form.Item>
          <Button type="primary" block onClick={handleSave}>
            保存配置
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
}
