import { useEffect, useState } from 'react';
import { Form, Input, Select, InputNumber, Button, Divider, message } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { useWorkflowStore } from '@/stores/workflowStore';
import { useUIStore } from '@/stores/uiStore';
import { workflowApi } from '@/api/workflow';
import type { PaiNodeData } from '@/types/node';

interface InputParam {
  name: string;
  type: 'input' | 'ref';
  value: string;
}

export default function ConfigPanel() {
  const selectedNodeId = useUIStore((s) => s.selectedNodeId);
  const { nodes, edges, workflowId, workflowName, updateNodeData, loadWorkflow } = useWorkflowStore();
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

  const handleSave = async () => {
    if (selectedNodeId) {
      // 更新本地状态
      const updatedNodes = nodes.map((n) =>
        n.id === selectedNodeId ? { ...n, data: { ...n.data, config: localConfig } } : n
      );
      updateNodeData(selectedNodeId, { config: localConfig });

      // 持久化到后端
      try {
        const graphJson = { nodes: updatedNodes, edges };
        if (workflowId) {
          await workflowApi.update(workflowId, { name: workflowName, graphJson });
        } else {
          const id = await workflowApi.create({ name: workflowName, graphJson });
          loadWorkflow(id, workflowName, updatedNodes, edges);
        }
        message.success('配置已保存');
      } catch (err: unknown) {
        message.error(err instanceof Error ? err.message : '保存失败');
      }
    }
  };

  const updateConfig = (key: string, value: unknown) => {
    setLocalConfig((prev) => ({ ...prev, [key]: value }));
  };

  const renderConfigFields = () => {
    // 检查是否为 DeepSeek 或通义千问节点
    const isDeepSeek = nodeData.label === 'DeepSeek';
    const isTongyi = nodeData.label === '通义千问';
    const hasCustomConfig = isDeepSeek || isTongyi;

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
            {hasCustomConfig ? (
              <>
                <Form.Item label="模型接口地址">
                  <Input
                    value={localConfig.apiUrl as string}
                    onChange={(e) => updateConfig('apiUrl', e.target.value)}
                    placeholder={isDeepSeek ? 'https://api.deepseek.com/v1/chat/completions' : 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation'}
                  />
                </Form.Item>
                <Form.Item label="API 密钥">
                  <Input.Password
                    value={localConfig.apiKey as string}
                    onChange={(e) => updateConfig('apiKey', e.target.value)}
                    placeholder={`请输入 ${nodeData.label} API 密钥`}
                  />
                </Form.Item>
                <Form.Item label="模型名称">
                  <Input
                    value={localConfig.model as string}
                    onChange={(e) => updateConfig('model', e.target.value)}
                    placeholder={isDeepSeek ? 'deepseek-chat' : 'qwen-turbo'}
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
                <div style={{ fontSize: 12, color: '#999', marginTop: -8, marginBottom: 12 }}>
                  范围 0-2，值越高输出越随机创意，值越低输出越确定一致
                </div>
                <Divider style={{ margin: '12px 0' }} />
                <div style={{ fontWeight: 600, marginBottom: 8 }}>输入参数</div>
                {(localConfig.inputParams as InputParam[] | undefined)?.map((param, index) => (
                  <div key={index} style={{ marginBottom: 8, padding: 8, background: '#fafafa', borderRadius: 4 }}>
                    <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                      <Input
                        placeholder="参数名"
                        value={param.name}
                        onChange={(e) => {
                          const newParams = [...((localConfig.inputParams as InputParam[]) || [])];
                          newParams[index] = { ...param, name: e.target.value };
                          updateConfig('inputParams', newParams);
                        }}
                        style={{ flex: 1 }}
                      />
                      <Select
                        value={param.type}
                        onChange={(v) => {
                          const newParams = [...((localConfig.inputParams as InputParam[]) || [])];
                          newParams[index] = { ...param, type: v, value: '' };
                          updateConfig('inputParams', newParams);
                        }}
                        style={{ width: 80 }}
                        options={[
                          { value: 'input', label: '输入' },
                          { value: 'ref', label: '引用' },
                        ]}
                      />
                      <Button
                        type="text"
                        danger
                        icon={<MinusCircleOutlined />}
                        onClick={() => {
                          const newParams = ((localConfig.inputParams as InputParam[]) || []).filter((_, i) => i !== index);
                          updateConfig('inputParams', newParams);
                        }}
                      />
                    </div>
                    {param.type === 'input' ? (
                      <Input.TextArea
                        placeholder="输入值"
                        value={param.value}
                        onChange={(e) => {
                          const newParams = [...((localConfig.inputParams as InputParam[]) || [])];
                          newParams[index] = { ...param, value: e.target.value };
                          updateConfig('inputParams', newParams);
                        }}
                        rows={2}
                      />
                    ) : (
                      <Select
                        placeholder="选择引用节点"
                        value={param.value || undefined}
                        onChange={(v) => {
                          const newParams = [...((localConfig.inputParams as InputParam[]) || [])];
                          newParams[index] = { ...param, value: v };
                          updateConfig('inputParams', newParams);
                        }}
                        allowClear
                        style={{ width: '100%' }}
                      >
                        {nodes
                          .filter((n) => n.id !== selectedNodeId)
                          .map((n) => (
                            <Select.Option key={n.id} value={`${n.id}.output`}>
                              {(n.data as PaiNodeData).label} ({n.id})
                            </Select.Option>
                          ))}
                      </Select>
                    )}
                  </div>
                ))}
                <Button
                  type="dashed"
                  block
                  icon={<PlusOutlined />}
                  onClick={() => {
                    const currentParams = (localConfig.inputParams as InputParam[]) || [];
                    updateConfig('inputParams', [...currentParams, { name: '', type: 'input', value: '' }]);
                  }}
                  style={{ marginBottom: 12 }}
                >
                  添加参数
                </Button>
                <Divider style={{ margin: '12px 0' }} />
                <div style={{ fontWeight: 600, marginBottom: 8 }}>输出参数</div>
                <div style={{ marginBottom: 8, padding: 8, background: '#fafafa', borderRadius: 4 }}>
                  <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                    <Input
                      placeholder="变量名"
                      value={(localConfig.outputParam as { name: string; type: string; description: string })?.name || ''}
                      onChange={(e) => {
                        updateConfig('outputParam', {
                          ...((localConfig.outputParam as { name: string; type: string; description: string }) || {}),
                          name: e.target.value,
                          type: ((localConfig.outputParam as { name: string; type: string; description: string })?.type) || 'string',
                          description: ((localConfig.outputParam as { name: string; type: string; description: string })?.description) || '',
                        });
                      }}
                      style={{ flex: 1 }}
                    />
                    <Select
                      value={(localConfig.outputParam as { name: string; type: string; description: string })?.type || 'string'}
                      onChange={(v) => {
                        updateConfig('outputParam', {
                          ...((localConfig.outputParam as { name: string; type: string; description: string }) || {}),
                          type: v,
                        });
                      }}
                      style={{ width: 100 }}
                      options={[
                        { value: 'string', label: 'string' },
                      ]}
                    />
                  </div>
                  <Input
                    placeholder="描述（可选）"
                    value={(localConfig.outputParam as { name: string; type: string; description: string })?.description || ''}
                    onChange={(e) => {
                      updateConfig('outputParam', {
                        ...((localConfig.outputParam as { name: string; type: string; description: string }) || {}),
                        description: e.target.value,
                      });
                    }}
                  />
                </div>
                <Divider style={{ margin: '12px 0' }} />
                <Form.Item label="用户提示词">
                  <Input.TextArea
                    value={localConfig.userPrompt as string}
                    onChange={(e) => updateConfig('userPrompt', e.target.value)}
                    rows={8}
                    placeholder="请输入用户提示词，可使用 {{参数名}} 引用上面定义的参数值"
                  />
                </Form.Item>
                <div style={{ fontSize: 12, color: '#999', marginTop: -8, marginBottom: 12 }}>
                  使用 {`{{参数名}}`} 引用上面定义的参数值
                </div>
              </>
            ) : (
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
                <div style={{ fontSize: 12, color: '#999', marginTop: -8, marginBottom: 12 }}>
                  范围 0-2，值越高输出越随机创意，值越低输出越确定一致
                </div>
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
            )}
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
                <Form.Item label="API 密钥">
                  <Input.Password
                    value={localConfig.apiKey as string}
                    onChange={(e) => updateConfig('apiKey', e.target.value)}
                    placeholder="请输入 API 密钥"
                  />
                </Form.Item>
                <Form.Item label="模型名称">
                  <Input
                    value={localConfig.model as string}
                    onChange={(e) => updateConfig('model', e.target.value)}
                    placeholder="qwen3-tts-flash"
                  />
                </Form.Item>
                <Divider style={{ margin: '12px 0' }} />
                <div style={{ fontWeight: 600, marginBottom: 8 }}>输入参数</div>
                {/* text 参数 */}
                <div style={{ marginBottom: 8, padding: 8, background: '#fafafa', borderRadius: 4 }}>
                  <div style={{ fontWeight: 500, marginBottom: 8 }}>text（文本内容）</div>
                  <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                    <Select
                      value={((localConfig.text as Record<string, unknown>)?.type as string) || 'input'}
                      onChange={(v) => {
                        updateConfig('text', {
                          ...((localConfig.text as Record<string, unknown>) || {}),
                          type: v,
                          value: '',
                        });
                      }}
                      style={{ width: 80 }}
                      options={[
                        { value: 'input', label: '输入' },
                        { value: 'ref', label: '引用' },
                      ]}
                    />
                  </div>
                  {((localConfig.text as Record<string, unknown>)?.type as string) === 'input' ? (
                    <Input.TextArea
                      placeholder="请输入要合成的文本"
                      value={((localConfig.text as Record<string, unknown>)?.value as string) || ''}
                      onChange={(e) => {
                        updateConfig('text', {
                          ...((localConfig.text as Record<string, unknown>) || {}),
                          value: e.target.value,
                        });
                      }}
                      rows={3}
                    />
                  ) : (
                    <Select
                      placeholder="选择引用节点"
                      value={((localConfig.text as Record<string, unknown>)?.value as string) || undefined}
                      onChange={(v) => {
                        updateConfig('text', {
                          ...((localConfig.text as Record<string, unknown>) || {}),
                          value: v,
                        });
                      }}
                      allowClear
                      style={{ width: '100%' }}
                    >
                      {nodes
                        .filter((n) => n.id !== selectedNodeId)
                        .map((n) => (
                          <Select.Option key={n.id} value={`${n.id}.output`}>
                            {(n.data as PaiNodeData).label} ({n.id})
                          </Select.Option>
                        ))}
                    </Select>
                  )}
                </div>
                {/* voice 参数 */}
                <Form.Item label="voice（音色）">
                  <Select
                    value={localConfig.voice as string}
                    onChange={(v) => updateConfig('voice', v)}
                    options={[
                      { value: 'Cherry', label: 'Cherry' },
                      { value: 'Serena', label: 'Serena' },
                      { value: 'Ethan', label: 'Ethan' },
                    ]}
                  />
                </Form.Item>
                {/* language_type 参数 */}
                <Form.Item label="language_type（语言类型）">
                  <Select
                    value={localConfig.languageType as string}
                    onChange={(v) => updateConfig('languageType', v)}
                    options={[
                      { value: 'Auto', label: 'Auto' },
                    ]}
                  />
                </Form.Item>
                <Divider style={{ margin: '12px 0' }} />
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
