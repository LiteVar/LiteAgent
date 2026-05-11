import React, { useCallback, useEffect, useState } from 'react';
import { Modal, Descriptions, Button, Popconfirm, Skeleton } from 'antd';
import { getV1ToolDetailByIdOptions } from "@/client/@tanstack/query.gen";
import { useQuery } from '@tanstack/react-query'
import { ToolSchemaType } from "@/types/Tool";
import { ToolDTO, ToolProvider } from "@/client";

interface ToolInfoModalProps {
  visible: boolean;
  onClose: () => void;
  toolInfo: ToolDTO | undefined
  deleteTool: (toolId: string) => void;
  showEditingToolModal: (tool: any) => void;
  showExportModal?: (event: React.MouseEvent, record: any) => void;
}

const ToolInfoModal: React.FC<ToolInfoModalProps> = (props) => {
  const { visible, onClose, toolInfo, deleteTool, showEditingToolModal, showExportModal } = props;
  const toolId = toolInfo?.id;
  const { data, isLoading } = useQuery({
    ...getV1ToolDetailByIdOptions({
      path: {
        id: toolId!
      }
    }),
    enabled: visible && !!toolId
  })

  const toolDetail: ToolProvider | undefined = data?.data;

  const [isSchemaExpanded, setIsSchemaExpanded] = useState(false);
  const [isApiKeyExpanded, setIsApiKeyExpanded] = useState(false);

  useEffect(() => {
    if (!visible) {
      setIsSchemaExpanded(false);
      setIsApiKeyExpanded(false);
    }
  }, [visible]);

  const handelEdit = useCallback(() => {
    if (!toolInfo) return;
    const nextEditingTool = { ...toolDetail, ...toolInfo };
    onClose();
    window.setTimeout(() => {
      showEditingToolModal(nextEditingTool);
    }, 0);
  }, [toolDetail, showEditingToolModal, toolInfo, onClose])

  const handelDelete = useCallback(async () => {
    await deleteTool(toolId!)
    onClose()
  }, [toolId, deleteTool, onClose])

  const handelExport = useCallback(async (event: React.MouseEvent) => {
    event.stopPropagation();
    if (showExportModal && toolDetail) {
      showExportModal(event, toolDetail);
    }
  }, [showExportModal, toolDetail])

  return (
    <Modal
      centered
      zIndex={99}
      open={visible}
      onCancel={onClose}
      footer={null}
      destroyOnClose
      width={600}
      title={<span className="text-[18px] font-medium text-[#1D4A6B]">{toolDetail?.name || '工具详情'}</span>}
      styles={{
        header: { padding: '16px 24px', marginBottom: 0, borderBottom: 'none' },
        body: { padding: '16px 24px' },
      }}
    >
      {isLoading &&
        <Skeleton active />
      }
      {!!toolDetail && (
        <div className="flex flex-col gap-4">
          <p className="text-[14px] text-[#58636C] m-0">{toolDetail?.description}</p>

          <Descriptions 
            column={1} 
            className="mt-2"
            labelStyle={{ color: '#7C8B98', width: '120px' }}
            contentStyle={{ color: '#383F44' }}
          >
            <Descriptions.Item label="schema类型">
              {toolDetail?.schemaType === ToolSchemaType.OPEN_API3 ? 'OpenAPI3(YAML/JSON)' :
                toolDetail?.schemaType === ToolSchemaType.JSON_RPC ? 'OpenRPC(JSON)' :
                  toolDetail?.schemaType === ToolSchemaType.OPEN_TOOL ? '第三方open tool' :
                    toolDetail?.schemaType === ToolSchemaType.MCP ? 'MCP(HTTP)' :
                      toolDetail?.schemaType === ToolSchemaType.OPEN_MODBUS ? 'OpenModbus(JSON)' : 
                        toolDetail?.schemaType === ToolSchemaType.OPEN_TOOL_SPEC ? 'OpenTool Spec' : ''}
            </Descriptions.Item>
            <Descriptions.Item label="schema文稿">
              <div className="schema-paragraph-wrapper bg-[#F2F3F5] rounded-lg relative p-3">
                <div
                  className={`m-0 text-[12px] font-mono whitespace-pre-wrap break-all ${
                    isSchemaExpanded ? 'max-h-[500px] overflow-y-auto' : 'max-h-[72px] overflow-hidden'
                  }`}
                >
                  {toolDetail?.schemaStr}
                </div>
                <div className="flex justify-end mt-1">
                  <Button type="link" size="small" className="p-0 h-auto text-[#40A5EE]" onClick={() => setIsSchemaExpanded((v) => !v)}>
                    {isSchemaExpanded ? '收起' : '展开'}
                  </Button>
                </div>
              </div>
            </Descriptions.Item>
            {toolDetail?.apiKeyType && (
              <Descriptions.Item label="API Key类型">{toolDetail?.apiKeyType}</Descriptions.Item>
            )}
            {toolDetail?.apiKey && (
              <Descriptions.Item label="API Key值">
                <div>
                  <div
                    className={`m-0 break-all ${isApiKeyExpanded ? '' : 'line-clamp-2'}`}
                  >
                    {toolDetail?.apiKey}
                  </div>
                  <div className="flex justify-end mt-1">
                    <Button type="link" size="small" className="p-0 h-auto text-[#40A5EE]" onClick={() => setIsApiKeyExpanded((v) => !v)}>
                      {isApiKeyExpanded ? '收起' : '展开'}
                    </Button>
                  </div>
                </div>
              </Descriptions.Item>
            )}
            <Descriptions.Item label="Auto MultiAgent">{toolDetail?.autoAgent ? '支持' : '不支持'}</Descriptions.Item>
          </Descriptions>

          <div className="flex gap-3 mt-4">
            {toolInfo?.canEdit &&
              <Button className="flex-1 bg-[#40A5EE] rounded-xl h-10 border-[#40A5EE]" type="primary" onClick={handelEdit}>编辑</Button>
            }
            {toolInfo?.canEdit && showExportModal &&
              <Button className="flex-1 rounded-xl h-10 border-[#E0E3E6] text-[#383F44]" onClick={handelExport}>导出</Button>
            }
            {toolInfo?.canDelete &&
              <Popconfirm
                title="确认删除"
                description="即将删除工具，确认删除？"
                onConfirm={handelDelete}
                okText="确认"
                cancelText="取消"
                okButtonProps={{ danger: true, className: 'bg-[#CC2D3A] border-[#CC2D3A]' }}
              >
                <Button className="flex-1 rounded-xl h-10" danger>删除</Button>
              </Popconfirm>
            }
          </div>
        </div>
      )}
    </Modal>
  );
};

export default ToolInfoModal;
