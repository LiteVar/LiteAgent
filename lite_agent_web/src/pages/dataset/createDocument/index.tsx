import React, { useState, useMemo } from 'react';
import SelectSource from './components/SelectSource';
import DataProcess from './components/DataProcess';
import UploadData from './components/UploadData';
import Header from './components/Header';
import { DatasetDocument, postV1DatasetByDatasetIdDocuments } from '@/client';
import { useDatasetContext } from '@/contexts/datasetContext';
import { DocumentSourceType } from '@/types/dataset';
const CreateDocument = () => {
  const { workspaceId } = useDatasetContext();
  const datasetId = useMemo(() => {
    const path = window.location.pathname;
    return path.split('/')[3];
  }, []);

  const [currentStep, setCurrentStep] = useState(0);
  const [documentData, setDocumentData] = useState<DatasetDocument | null>({
    workspaceId,
    datasetId,
    dataSourceType: DocumentSourceType.INPUT,
  });

  const steps = [
    {
      title: '选择文档/文本',
      content: (
        <SelectSource
          documentData={documentData}
          setDocumentData={setDocumentData}
          onNext={() => setCurrentStep(1)}
        />
      ),
    },
    {
      title: '数据处理',
      content: (
        <DataProcess
          documentData={documentData}
          setDocumentData={setDocumentData}
          onPrev={() => setCurrentStep(0)}
          onNext={() => setCurrentStep(2)}
        />
      ),
    },
    {
      title: '上传数据',
      content: <UploadData documentData={documentData} onPrev={() => setCurrentStep(1)} />,
    },
  ];

  return (
    <div className="flex flex-col h-full p-2 pt-4">
      <Header currentStep={currentStep} steps={steps} />
      <div className="flex-1 px-8 border-0 border-t border-t-gray-200 border-solid">
        <div className="steps-content h-full">{steps[currentStep].content}</div>
      </div>
    </div>
  );
};

export default CreateDocument;
