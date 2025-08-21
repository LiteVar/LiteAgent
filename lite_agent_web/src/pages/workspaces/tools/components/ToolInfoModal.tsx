import React, { useCallback } from 'react';
import { Modal, Typography, Descriptions, Button, Popconfirm, Skeleton } from 'antd';
import { getV1ToolDetailByIdOptions } from "@/client/@tanstack/query.gen";
import { useQuery } from '@tanstack/react-query'
import { ToolSchemaType } from "@/types/Tool";
import { ToolDTO, ToolProvider } from "@/client";

const { Title, Paragraph } = Typography;

interface ToolInfoModalProps {
  visible: boolean;
  onClose: () => void;
  toolInfo: ToolDTO | undefined
  deleteTool: (toolId: string) => void;
  showEditingToolModal: (tool: any) => void;
}

const ToolInfoModal: React.FC<ToolInfoModalProps> = (props) => {
  const { visible, onClose, toolInfo, deleteTool, showEditingToolModal } = props;
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

  const handelEdit = useCallback(() => {
    showEditingToolModal({ ...toolDetail, ...toolInfo })
    onClose()
  }, [toolDetail, showEditingToolModal, toolInfo])

  const handelDelete = useCallback(async () => {
    await deleteTool(toolId!)
    onClose()
  }, [toolDetail, deleteTool])

  return (
    <Modal
      centered
      open={visible}
      onCancel={onClose}
      footer={null}
      width={600}
    >
      {isLoading &&
        <Skeleton />
      }
      {!!toolDetail && <>
        <Title level={4} className='center'>{toolDetail?.name}</Title>
        <Paragraph>{toolDetail?.description}</Paragraph>

        <Descriptions column={1} className="mb-4">
          <Descriptions.Item label="schema类型">
            {toolDetail?.schemaType === ToolSchemaType.OPEN_API3 ? 'OpenAPI3(YAML/JSON)' :
              toolDetail?.schemaType === ToolSchemaType.JSON_RPC ? 'OpenRPC(JSON)' :
                toolDetail?.schemaType === ToolSchemaType.OPEN_TOOL ? '第三方open tool' :
                  toolDetail?.schemaType === ToolSchemaType.MCP ? 'MCP(SSE)' :
                    toolDetail?.schemaType === ToolSchemaType.OPEN_MODBUS ? 'OpenModbus(JSON)' : ''}
          </Descriptions.Item>
          <Descriptions.Item label="schema文稿">
            <Paragraph ellipsis={{ rows: 3, expandable: true, symbol: '展开' }} style={{ maxHeight: 500, overflowY: 'auto' }}>
              {toolDetail?.schemaStr}
            </Paragraph>
          </Descriptions.Item>
          {toolDetail?.apiKeyType && (
            <Descriptions.Item label="API Key类型">{toolDetail?.apiKeyType}</Descriptions.Item>
          )}
          {toolDetail?.apiKey && (
            <Descriptions.Item label="API Key值">
              <Paragraph ellipsis={{ rows: 2, expandable: true, symbol: '展开' }}>
                {toolDetail?.apiKey}
              </Paragraph>
            </Descriptions.Item>
          )}
          <Descriptions.Item label="Auto MultiAgent">{toolDetail?.autoAgent ? '支持' : '不支持'}</Descriptions.Item>
        </Descriptions>

        {toolInfo?.canEdit &&
          <Button className="w-28 mr-4" type="primary" onClick={handelEdit}>编辑</Button>
        }
        {toolInfo?.canDelete &&
          <Popconfirm
            title="确认删除"
            description="即将删除工具，确认删除？"
            onConfirm={handelDelete}
            okText="确认"
            cancelText="取消"
          >
            <Button className="w-28 mb-4" type="primary" danger>删除</Button>
          </Popconfirm>
        }
      </>}
    </Modal>
  );
};

export default ToolInfoModal;
