import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Spin, Result, message } from 'antd';
import { getAccessToken } from '@/utils/cache';
import { getV1UserInfoOptions } from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';
import ResponseCode from '@/constants/ResponseCode';

/**
 * 中间跳转页面
 * 只接受 path 和 page 两个固定参数，其他所有参数传递给后端
 */
const NavigatePage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const [loading, setLoading] = useState(true);

  const { data: userInfoResult } = useQuery({
    ...getV1UserInfoOptions({}),
  });

  const userInfo = userInfoResult?.data;

  // 解析固定参数
  const path = searchParams.get('path');
  const page = searchParams.get('page');
  const params = searchParams.get('params');
  
  

  // 将参数传递给后端
  useEffect(() => {
    if (!userInfo || !params) return;

    const navigateToWorkSpacePage = async () => {
      try {
        const token = getAccessToken();
        
        // 调用后端 API 传递参数
        const response = await fetch(`/v1/agent/import/previewFromDownload?param=${params}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            Authorization: token ? `Bearer ${token}` : '',
          },
        });
  
        if (!response.ok) {
          console.warn('传递参数到后端失败:', response.status);
          message.error('传递参数到后端失败:' + response.status);
          window.location.href = `/dashboard`;
        } else {
          const result = await response.json();
          if (result.code === ResponseCode.S_OK) {
            window.location.href = `/workspaces/${result.data.workspaceId}/agents?action=import&param=${params}`;
          } else {
            message.error(result.message);
            window.location.href = `/dashboard`;
          }
        }
      } catch (err) {
        console.error('传递参数到后端时出错:', err);
        message.error('传递参数到后端时出错' + err);
        window.location.href = `/dashboard`;
        // 不阻止跳转，只记录错误
      } finally {
        setLoading(false);
      }
    };

    if (path === 'workspaces') {
      navigateToWorkSpacePage();
    }
  }, [path, page, params, userInfo]);

  return (
    <div className="min-h-screen flex justify-center items-center">
      <Result
        status="info"
        title="正在处理..."
        subTitle={
          <div>
            <div>目标路径: {path}</div>
          </div>
        }
        extra={<Spin size="large" />}
      />
    </div>
  );
};

export default NavigatePage;

