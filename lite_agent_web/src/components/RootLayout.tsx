import React, {Suspense} from "react";
import {ApiClientInit} from "@/components/ApiClientInit";
import {Skeleton} from "antd";
import {Outlet} from "react-router-dom";

const RootLayout: React.FC = () => {
  return (
    <ApiClientInit>
      <Suspense fallback={<Skeleton/>}>
        <Outlet/>
      </Suspense>
    </ApiClientInit>
  );
};

export default RootLayout;
