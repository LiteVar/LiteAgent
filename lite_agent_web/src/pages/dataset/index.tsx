import React, { useMemo } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import DocumentList from './documentList/index';
import FragmentList from './fragmentList/index';
import CreateDocument from './createDocument/index';
import RetrievalTest from './retrievalTest/index';
import Settings from './settings/index';
import Apis from './apis/index';
import Header from './components/Header';
import SideMenu from './components/SideMenu';
import { useQuery } from '@tanstack/react-query';
import { getV1DatasetByIdOptions, getV1ModelListOptions } from '@/client/@tanstack/query.gen';
import { DatasetProvider } from '@/contexts/datasetContext';

const DatasetDetails = () => {
  const navigate = useNavigate();
  const [selectedTab, setSelectedTab] = useState('documents');
  const workspaceId = useMemo(() => {
    const path = window.location.pathname;
    const id = path.split('/')[2];
    return id;
  }, []);
  const datasetId = useMemo(() => {
    const path = window.location.pathname;
    const id = path.split('/')[3];
    return id;
  }, []);

  useEffect(() => {
    const path = window.location.pathname;
    if (path.includes('/test')) setSelectedTab('test');
    if (path.includes('/settings')) setSelectedTab('settings');
    if (path.includes('/apis')) setSelectedTab('apis');
  }, []);

  const handleTabChange = (key: string) => {
    setSelectedTab(key);
    navigate(`/dataset/${workspaceId}/${datasetId}/${key === 'documents' ? '' : key}`);
  };

  const { data, refetch } = useQuery({
    ...getV1DatasetByIdOptions({
      path: {
        id: datasetId,
      },
    }),
    enabled: !!datasetId && !!workspaceId,
  });

  const { data: modelsData } = useQuery({
    ...getV1ModelListOptions({
      headers: {
        'Workspace-id': workspaceId!,
      },
      query: {
        pageNo: 0,
        pageSize: 100000000,
      },
    }),
    enabled: !!workspaceId,
  });

  const datasetInfo = useMemo(() => {
    return data?.data;
  }, [data]);

  const models = useMemo(() => {
    return modelsData?.data?.list || [];
  }, [modelsData]);

  const contextValue = {
    workspaceId,
    datasetInfo,
    models,
  };

  return (
    <DatasetProvider value={contextValue}>
      <div className="flex flex-col h-[100vh] overflow-hidden">
        <Header datasetInfo={datasetInfo} />
        <main className="flex-grow flex flex-1 overflow-hidden">
          <SideMenu
            canEdit={datasetInfo?.canEdit!}
            canDelete={datasetInfo?.canDelete!}
            selectedTab={selectedTab}
            onTabChange={handleTabChange}
          />
          <div className="flex-1 bg-white rounded p-4 px-8 overflow-y-auto">
            <Routes>
              <Route path="/" element={<DocumentList />} />
              <Route path="/fragments" element={<FragmentList />} />
              <Route path="/createDocument" element={<CreateDocument />} />
              <Route path="/test" element={<RetrievalTest />} />
              {datasetInfo?.canEdit && datasetInfo?.canDelete && (
                <>
                  <Route path="/settings" element={<Settings refetch={refetch} />} />
                </>
              )}

              <Route path="/apis" element={<Apis refetch={refetch} />} />
            </Routes>
          </div>
        </main>
      </div>
    </DatasetProvider>
  );
};

export default DatasetDetails;
