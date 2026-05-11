import React, { createContext, useContext, ReactNode } from 'react';
import { Account } from "@/client";

interface AdminProviderProps {
  value: {
    userInfo: Account | undefined;
  };
  children: ReactNode;
}

const AdminContext = createContext<{ userInfo: Account | undefined } | null>(null);

export const AdminProvider: React.FC<AdminProviderProps> = ({ value, children }) => {
  return (
    <AdminContext.Provider value={value}>
      {children}
    </AdminContext.Provider>
  );
};


export const useUserInfo = () => {
  const context = useContext(AdminContext);
  if (!context) {
    throw new Error("useUserInfo must be used within a AdminProvider");
  }
  return context.userInfo;
}
