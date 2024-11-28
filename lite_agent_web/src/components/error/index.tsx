import React, {useMemo} from "react";
import {Button, Result} from "antd";
import {Helmet} from "react-helmet";
import {ExceptionStatusType} from "antd/lib/result";
import {useNavigate} from "react-router-dom";

interface IErrorPageProps {
  code: ExceptionStatusType
}

const ErrorPage: React.FC<IErrorPageProps> = ({code,}) => {
  const navigate = useNavigate();
  const message = useMemo<string>(() => {
    switch (code) {
      case 404:
        return '此页面未找到'
      case 403:
        return '此页面无权限访问'
      default:
        return '页面发生错误'
    }
  }, [code])

  return (
    <div className="w-full ">
      <Helmet>
        <title>{`错误 ${code}`}</title>
      </Helmet>
      <div className="mt-30">
        <Result
          status={code}
          title={code}
          subTitle={message}
          extra={<Button type="primary" onClick={() => navigate('/dashboard', {replace: true})}>返回首页</Button>}
        />
      </div>
    </div>
  )
}

export default ErrorPage;
