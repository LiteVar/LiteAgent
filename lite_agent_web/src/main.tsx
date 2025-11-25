import React from 'react';
import ReactDOM from 'react-dom/client';
import './globals.css';
import './antd.global.css'
import '@/assets/styles/markdown.css';
import reportWebVitals from './reportWebVitals';
import {createBrowserRouter, RouterProvider} from "react-router-dom";
import {QueryClientProvider, QueryClient } from '@tanstack/react-query'
import {ConfigProvider} from 'antd';
import zhCN from 'antd/locale/zh_CN';
import 'dayjs/locale/zh-cn';
import themeConfig from './antdTheme';
import { routerConfig } from './router.tsx';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      refetchIntervalInBackground: false,
    }
  },
});

const router = createBrowserRouter(routerConfig, {
  future: {
    v7_relativeSplatPath: true,
  },
});

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);
root.render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={zhCN} theme={themeConfig}>
        <RouterProvider router={router} />
      </ConfigProvider>
    </QueryClientProvider>
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
