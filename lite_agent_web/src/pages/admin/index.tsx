import React, { useEffect, useMemo, useCallback } from 'react';
import {useNavigate, Link} from "react-router-dom";
import { getV1WorkspaceListOptions } from "@/client/@tanstack/query.gen";
import { useQuery } from "@tanstack/react-query";

const WorkspacesPage = () => {
  const navigate = useNavigate();
  const { data, isLoading, isError } = useQuery({
    ...getV1WorkspaceListOptions({})
  });
  const workspaces = useMemo(() => data?.data || [], [data]);

  const redirectToModelList = useCallback(async () => {
    navigate(`/admin/models`, { replace: true })
  }, [workspaces.length, navigate]);

  useEffect(() => {
    if (!isLoading && !isError) {
      redirectToModelList();
    }
  }, [isLoading, isError, redirectToModelList]);

  if (isLoading) {
    return <div>Loading workspaces...</div>;
  }

  if (isError) {
    return <div>Error loading workspaces. Please try again later.</div>;
  }

  if (workspaces.length === 0) {
    return (
      <div>
        <p>No workspaces found.</p>
        <Link to="/create-workspace">Create a new workspace</Link>
      </div>
    );
  }

  return <div>Redirecting to workspace...</div>;
}

export default WorkspacesPage
