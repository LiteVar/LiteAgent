import React, {useEffect, useRef} from 'react'
import {client} from "@/client";
import {getAccessToken, removeAccessToken} from "@/utils/cache";
import { useNavigate, useLocation } from 'react-router-dom';
import {ROUTES} from "@/constants/routes";
import { Modal } from 'antd';

const whiteList = ['/init', '/login', '/activate', '/reset'];

export const ApiClientInit = ({ children }: { children: React.ReactNode }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const interceptorsRef = useRef<{ request?: number; response?: number }>({});

  useEffect(() => {
    const currentPath = location.pathname;
    if (whiteList.includes(currentPath)) return;

    // 移除之前注册的拦截器
    if (interceptorsRef.current.request !== undefined) {
      client.interceptors.request.eject(interceptorsRef.current.request);
    }
    if (interceptorsRef.current.response !== undefined) {
      client.interceptors.response.eject(interceptorsRef.current.response);
    }

    const reqId = client.interceptors.request.use((request) => {
      const token = getAccessToken()
      if (token) {
        request.headers.set('Authorization', 'Bearer ' + token);
      } else {
        const fullPath = location.pathname + location.search;
        navigate(`${ROUTES.LOGIN}?redirect=${encodeURIComponent(fullPath)}`);
      }
      return request;
    });

    const resId = client.interceptors.response.use(
      async (response) => {
        if (response.status === 401) {
          removeAccessToken();
          const fullPath = location.pathname + location.search;
          window.location.href = ROUTES.LOGIN + `?redirect=${encodeURIComponent(fullPath)}`
        }
        
        if (response.status === 400) {
          try {
            const clonedResponse = response.clone();
            const responseData = await clonedResponse.json();
            if (responseData?.code === 10004) {
              Modal.confirm({
                title: '无权限访问',
                content: '您没有权限访问此资源',
                okText: '确认',
                cancelButtonProps: { style: { display: 'none' } },
                onOk: () => {
                  navigate(ROUTES.DASHBOARD);
                },
              });
              throw new Error('Permission denied: code 10004');
            }
          } catch (error) {
            if (error instanceof Error && error.message === 'Permission denied: code 10004') {
              throw error;
            }
          }
        }
        
        return response;
      }
    );

    interceptorsRef.current = { request: reqId, response: resId };

    return () => {
      if (reqId !== undefined) client.interceptors.request.eject(reqId);
      if (resId !== undefined) client.interceptors.response.eject(resId);
    };
  }, [location.pathname, location.search, navigate])

  return <>{children}</>
}
