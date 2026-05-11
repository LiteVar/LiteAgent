import { useMemo, ReactNode, FC } from 'react';
import govIcon from '@/assets/login/gov.png';
import Bg from '@/assets/common/bg';

const LINK_CLASS = 'text-[#9AA7B3] no-underline hover:text-blue-500';

interface AuthLayoutProps {
  children: ReactNode;
}

const AuthPageLayout: FC<AuthLayoutProps> = ({ children }) => {
  
  const copyright = useMemo(() => {
    const currentYear = new Date().getFullYear();
    return `Copyright © 2021-${currentYear} 广州轻变量信息科技有限公司`;
  }, []);

  return (
    <div className="min-h-screen flex justify-center items-center relative overflow-hidden">
      <div className="absolute inset-0 -z-10">
        <Bg/>
      </div>
      <div className="w-[420px]">
        <div className="backdrop-blur-[2px] bg-white/60 border border-solid border-white py-10 px-6 rounded-[32px] shadow-sm">
          {children}
        </div>
      </div>

      <div className="mt-8 text-center text-sm text-[#9AA7B3] fixed bottom-6 w-full">
        <span>{copyright}</span>
        <a
          href="https://beian.miit.gov.cn/#/Integrated/index"
          target="_blank"
          className={`${LINK_CLASS} px-3`} rel="noreferrer"
        >
          粤ICP备2021065541号
        </a>
        <a
          href="https://www.beian.gov.cn/portal/registerSystemInfo?recordcode=44010602009917"
          target="_blank"
          className={LINK_CLASS} rel="noreferrer"
        >
          <img src={govIcon} className="align-middle" />
          <span>粤公网安备 44010602009917号</span>
        </a>
      </div>
    </div>
  );
};

export default AuthPageLayout;
