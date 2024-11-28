import React, {useEffect} from 'react'
import {client} from "@/client";
import {getAccessToken, removeAccessToken} from "@/utils/cache";
import { useNavigate, useLocation } from 'react-router-dom';
import {ROUTES} from "@/config/constants";

export const ApiClientInit = ({ children }: { children: React.ReactNode }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const whiteList = ['/init', '/login', '/activate'];

  useEffect(() => {
    const currentPath = location.pathname;
    if (whiteList.includes(currentPath)) return;

    client.interceptors.request.use((request) => {
      const token = getAccessToken()
      if (token) {
        request.headers.set('Authorization', 'Bearer ' + token);
      } else {
        navigate(ROUTES.LOGIN);
      }
      return request;
    });
    client.interceptors.response.use(
      (response) => {
        // 在这里处理响应
        if (response.status === 401) {
          removeAccessToken();
          window.location.href = ROUTES.LOGIN + `?redirect=${currentPath}`
        }
        return response;
      }
    );

  }, [])

  return <>{children}</>
}
