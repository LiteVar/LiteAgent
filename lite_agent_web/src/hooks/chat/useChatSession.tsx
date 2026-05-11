import { useRef, useCallback, useState } from 'react';
import { message, Modal } from 'antd';
import { 
  postV1ChatInitSession,
  postV1ChatInitSessionByAgentId,
  postV1ChatClearSession,
  postV1ChatClearDebugRecord,
  AgentDetailVO
} from '@/client';
import ResponseCode from '@/constants/ResponseCode';
import { CloseCircleOutlined } from '@ant-design/icons';

interface UseChatSessionProps {
  agentId: string;
  mode: 'prod' | 'dev';
  agentInfo?: AgentDetailVO;
}

export const useChatSession = ({ agentId, mode, agentInfo }: UseChatSessionProps) => {
  const sessionRef = useRef<string>('');
  const [sessionLoading, setSessionLoading] = useState(false);

  const initializeSession = useCallback(async () => {
    setSessionLoading(true);
    if (mode === 'prod') {
      const res = await postV1ChatInitSessionByAgentId({
        path: {
          agentId: agentId,
        },
      });

      if (res?.data?.code === ResponseCode.S_OK) {
        sessionRef.current = res?.data?.data || '';
        setSessionLoading(false);
        return true;
      } else if (res?.data?.code === ResponseCode.AGENT_NOT_FOUND) {
        message.error(res.data.message);
        setSessionLoading(false);
        return false;
      } else if (res?.data?.code === ResponseCode.MODEL_CLOSED) {
        Modal.error({
          className: '[&_.ant-modal-body]:px-8 [&_.ant-modal-body]:py-6',
          title: <div className="font-medium text-base leading-6">当前模型已停用，请在设置中切换至其他可用模型后重试。</div>,
          centered: true,
          icon: <CloseCircleOutlined className="text-red-500 text-2xl" />,
          okText: '确定',
          okButtonProps: {
            className: 'bg-blue-400 text-white text-sm border-none shadow-none outline-none',
          },
        });
      } else {
        message.error(res?.data?.message || 'ai模型初始化失败，请正确配置模型');
        setSessionLoading(false);
        return false;
      }
    } else {
      const res = await postV1ChatInitSession({
        body: {
          agentId: agentInfo?.agent?.id!,
          modelId: agentInfo?.agent?.llmModelId!,
          prompt: agentInfo?.agent?.prompt,
          temperature: agentInfo?.agent?.temperature,
          topP: agentInfo?.agent?.topP,
          maxTokens: agentInfo?.agent?.maxTokens,
          subAgentIds: agentInfo?.agent?.subAgentIds || [],
          mode: agentInfo?.agent?.mode,
          type: agentInfo?.agent?.type,
          functionList: agentInfo?.agent?.functionList || [],
          datasetIds: agentInfo?.datasetList?.map((d) => d.id!) || [],
          sequence: agentInfo?.agent?.sequence || [],
          turns: agentInfo?.agent?.turns,
        },
      });
      if (res?.data?.code === ResponseCode.S_OK) {
        sessionRef.current = res?.data?.data || '';
        setSessionLoading(false);
        return true;
      } else if (res?.data?.code === ResponseCode.AGENT_NOT_FOUND) {
        message.error(res.data.message);
        setSessionLoading(false);
        return false;
      } else if (res?.data?.code === ResponseCode.MODEL_CLOSED) {
        Modal.error({
          className: '[&_.ant-modal-body]:px-8 [&_.ant-modal-body]:py-6',
          title: <div className="font-medium text-base leading-6">当前模型已停用，请在设置中切换至其他可用模型后重试。</div>,
          centered: true,
          icon: <CloseCircleOutlined className="text-red-500 text-2xl" />,
          okText: '确定',
          okButtonProps: {
            className: 'bg-blue-400 text-white text-sm border-none shadow-none outline-none',
          },
        });
      } else {
        console.error('AI 模型初始化失败：', res?.data?.message);
        message.error(res?.data?.message || 'ai模型初始化失败，请正确配置模型');
        setSessionLoading(false);
        return false;
      }
    }
  }, [mode, agentId, agentInfo]);

  const clearSession = useCallback(async () => {
    if (sessionRef.current) {
      await postV1ChatClearSession({
        query: {
          sessionId: sessionRef.current,
        },
      });
    }
    if (mode === 'dev') {
      await postV1ChatClearDebugRecord({
        query: {
          agentId: agentId,
          debugFlag: 1,
        },
      });
    }
    sessionRef.current = '';
    message.success(mode === 'prod' ? '上下文联系已清除' : '记录已清空');
  }, [mode, agentId]);

  const resetSession = useCallback(() => {
    sessionRef.current = '';
  }, []);

  const getCurrentSession = useCallback(() => {
    return sessionRef.current;
  }, []);

  return {
    sessionRef,
    sessionLoading,
    initializeSession,
    clearSession,
    resetSession,
    getCurrentSession,
  };
};