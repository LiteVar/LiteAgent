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
import Bg from '@/assets/common/bg';

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
    else if (path.includes('/settings')) setSelectedTab('settings');
    else if (path.includes('/apis')) setSelectedTab('apis');
    else if (path.includes('/fragments')) setSelectedTab('fragments');
    else setSelectedTab('documents');
  }, [window.location.pathname]);

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

  const currentLLMModel = useMemo(() => {
    return models.find((m) => m.id === datasetInfo?.llmModelId);
  }, [models, datasetInfo]);


  const contextValue = {
    workspaceId,
    datasetInfo,
    models,
  };

  return (
    <DatasetProvider value={contextValue}>
      <div className="flex flex-col h-[100vh] overflow-hidden bg-[#F6FBFF]">
        <div className="absolute inset-0 -z-1">
          <Bg/>
        </div>
        <Header datasetInfo={datasetInfo} />
        <main className="flex flex-1 overflow-hidden p-4 gap-4">
          <SideMenu
            canEdit={datasetInfo?.canEdit!}
            canDelete={datasetInfo?.canDelete!}
            selectedTab={selectedTab}
            onTabChange={handleTabChange}
          />
          <div className="flex-1 flex flex-col backdrop-blur-md border border-white/80 rounded-2xl relative shadow-sm overflow-hidden">
            <Routes>
              <Route path="/" element={<DocumentList />} />
              <Route path="/fragments" element={<FragmentList />} />
              <Route path="/createDocument" element={<CreateDocument />} />
              <Route path="/test" element={<RetrievalTest />} />
              {datasetInfo?.canEdit && datasetInfo?.canDelete && (
                <Route path="/settings" element={<Settings currentLLMModel={currentLLMModel} refetch={refetch} />} />
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
