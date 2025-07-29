import React, { createContext, useContext, ReactNode } from 'react';
import { DatasetsVO, ModelDTO } from '@/client';
interface DatasetProviderProps {
  value: {
    workspaceId: string | undefined;
    datasetInfo: DatasetsVO | undefined;
    models: ModelDTO[] | undefined; 
  };
  children: ReactNode;
}

const DatasetContext = createContext<{ workspaceId: string | undefined; datasetInfo: DatasetsVO | undefined; models: ModelDTO[] | undefined } | null>(null);

export const DatasetProvider: React.FC<DatasetProviderProps> = ({ value, children }) => {
  return (
    <DatasetContext.Provider value={value}>
      {children}
    </DatasetContext.Provider>
  );
};

export const useDatasetContext = () => {
  const context = useContext(DatasetContext);
  if (!context) {
    throw new Error("useDatasetContext must be used within a DatasetProvider");
  }
  return context;
}; 