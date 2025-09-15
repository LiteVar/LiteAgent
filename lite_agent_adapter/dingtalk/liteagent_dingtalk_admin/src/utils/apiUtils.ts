// API utility functions for authentication and configuration
import { client } from '../api/client.gen';

// Configure the API client with authentication token
export const setApiToken = (token: string) => {
  client.setConfig({
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
};

// Remove authentication token from API client and localStorage
export const removeApiToken = () => {
  client.setConfig({
    headers: {},
  });
  localStorage.removeItem('token');
  localStorage.removeItem('tokenName');
};

// Get current authentication token
export const getApiToken = (): string | null => {
  return localStorage.getItem('token');
};

// Get dynamic base URL based on environment
const getBaseUrl = () => {
  if (import.meta.env.MODE === 'development') {
    return ''; // 开发环境使用空字符串，让vite代理处理 /api 路径
  }
  return import.meta.env.VITE_API_BASE_URL || 'http://192.168.2.188:9080';
};

// Initialize API client with stored token and response interceptor
export const initializeApiClient = () => {
  const token = getApiToken();
  
  // 设置动态baseUrl和认证token
  client.setConfig({
    baseUrl: getBaseUrl(),
    headers: token ? {
      Authorization: `Bearer ${token}`,
    } : {},
  });
};

// Handle logout
export const logout = () => {
  removeApiToken();
  window.location.href = '/signin';
};

// Handle API errors uniformly
export const handleApiError = (error: unknown): string => {
  if (error && typeof error === 'object' && 'message' in error) {
    return (error as { message: string }).message;
  }
  return 'An unexpected error occurred';
};
