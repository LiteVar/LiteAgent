import { useSearchParams, useLocation } from 'react-router-dom';

const REDIRECT_KEY = 'redirect';

const useLoginRedirect = () => {
  const [searchParams] = useSearchParams(); // 解构以获取 URLSearchParams 实例
  const location = useLocation(); // 使用 useLocation 来获取当前路径
  const redirect = searchParams.get(REDIRECT_KEY);
  const pathname = location.pathname;

  if (!redirect || redirect === pathname) {
    return '/dashboard';
  }

  return redirect;
};

export { useLoginRedirect };
