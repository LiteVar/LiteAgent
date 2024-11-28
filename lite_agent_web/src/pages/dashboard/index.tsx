import React, { useEffect, useMemo, useState } from 'react';
import {useQuery} from '@tanstack/react-query';
import {getV1WorkspaceListOptions,} from "@/client/@tanstack/query.gen";
import { useLocation, useNavigate } from 'react-router-dom';

const DashboardPage = () => {
  const location = useLocation();
  const pathname = location.pathname
  const urlWorkspaceId = pathname?.split('/')?.[2];
  const [workspaceId, setWorkspaceId] = useState<string>('');

  const navigate = useNavigate();
  const {data, isLoading, isError} = useQuery({
    ...getV1WorkspaceListOptions({})
  });
  const workspaces = useMemo(() => data?.data || [], [data]);

  useEffect(() => {
    if (urlWorkspaceId) {
      setWorkspaceId(urlWorkspaceId)
    } else if (workspaces.length > 0) {
      setWorkspaceId(workspaces[0].id || "");
    }
  }, [workspaces, urlWorkspaceId]);

  useEffect(() => {
    if (!workspaceId) return;
    navigate(`/dashboard/${workspaceId}/shop`, {replace: true})
  }, [workspaceId]);


  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (isError) {
    return <div>Error loading workspaces. Please try again later.</div>;
  }

  return <div>Redirecting to chat page...</div>;
};

export default DashboardPage;
