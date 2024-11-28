import React, { createContext, useContext, ReactNode } from 'react';
import {Account, WorkSpaceVO} from "@/client";

interface WorkspaceProviderProps {
  value: {
    workspace: WorkSpaceVO | undefined;
    userInfo: Account | undefined;
  };
  children: ReactNode;
}

const WorkspaceContext = createContext<{ workspace: WorkSpaceVO | undefined, userInfo: Account | undefined } | null>(null);

export const WorkspaceProvider: React.FC<WorkspaceProviderProps> = ({ value, children }) => {
  return (
    <WorkspaceContext.Provider value={value}>
      {children}
    </WorkspaceContext.Provider>
  );
};

export const useWorkspace = () => {
  const context = useContext(WorkspaceContext);
  if (!context) {
    throw new Error("useWorkspace must be used within a WorkspaceProvider");
  }
  return context.workspace;
};

export const useUserInfo = () => {
  const context = useContext(WorkspaceContext);
  if (!context) {
    throw new Error("useUserInfo must be used within a WorkspaceProvider");
  }
  return context.userInfo;
}
