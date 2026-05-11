import React, { useState, useMemo } from 'react';
import { Input, Button, message } from 'antd';
import SearchHistory from './components/SearchHistory';
import SearchResults from './components/SearchResults';
import { useDatasetContext } from '@/contexts/datasetContext';
import { getV1DatasetByIdRetrieve, DatasetRetrieveHistory, getV1DatasetRetrieveHistoryById, SegmentVO } from '@/client';
import { getV1DatasetByIdRetrieveHistoryOptions } from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';
import ResponseCode from '@/constants/ResponseCode';
import noSearchImg from '@/assets/dataset/no-search.png';

const { TextArea } = Input;
const RetrievalTest = () => {
  const { datasetInfo } = useDatasetContext();

  const [queryText, setQueryText] = useState('');
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<SegmentVO[]>([]);
  const [selectedRecord, setSelectedRecord] = useState<DatasetRetrieveHistory | null>(null);
  const [pageNo, setPageNo] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const { data, refetch } = useQuery({
    ...getV1DatasetByIdRetrieveHistoryOptions({
      query: {
        pageNo: pageNo,
        pageSize: pageSize,
      },
      path: {
        id: datasetInfo?.id!,
      },
    }),
    enabled: !!datasetInfo?.id,
  });

  const history = useMemo(() => {
    return data?.data?.list || [];
  }, [data]);

  const handleSearch = async (query: string) => {
    setLoading(true);
    setSelectedRecord(null);
    console.log('id---', datasetInfo?.id!);
    console.log('query---', query);
    const res = await getV1DatasetByIdRetrieve({
      path: { id: datasetInfo?.id! },
      query: {query}
    });
    console.log('res', res);
    if (res.data?.code === ResponseCode.S_OK) {
      setResults(res.data.data?.length? res.data.data: []);
    } else {
      message.error(res.data?.message || '检索失败, 请检查配置的模型');
    }
    await refetch();
    setLoading(false);
  };

  const selectRecord = async(record: DatasetRetrieveHistory) => {
    message.info('正在加载检索记录...');
    const res = await getV1DatasetRetrieveHistoryById({
      path: {
        id: record.id!,
      },
    });

    message.destroy();
    if (res.data?.code === ResponseCode.S_OK) {
      setSelectedRecord(record);
      const result = res.data.data;
      if (result && result.length > 0) {
        setResults(result);
      } else {
        setResults([]);
      }
    } else {
      message.error(res.data?.message || '获取检索记录失败');
    }

  };

  return (
    <div className="flex flex-col h-full gap-6">
      <div className="flex flex-1 gap-6 overflow-hidden min-h-0">
        {/* Left Panel - Search & History */}
        <div className="flex-1 flex flex-col gap-5 min-w-0 bg-white/60 py-6 px-4 rounded-2xl">
          <div className="">
            <div className="text-xl m-0">检索测试</div>
            <div className="mt-2 text-sm text-gray-500">
              根据给定的查询文本，测试知识库的命中效果
            </div>           
          </div>
          <div>
            <div className="relative group mb-4">
              <TextArea
                value={queryText}
                onChange={(e) => setQueryText(e.target.value)}
                placeholder="请输入查询文本..."
                maxLength={200}
                autoSize={{ minRows: 4, maxRows: 6 }}
                className="!rounded-xl !border-[#E0E3E6] !bg-white/80 focus:!bg-white focus:!border-blue-400 focus:!shadow-none transition-all !p-4 !text-sm"
              />
              <span className="absolute bottom-3 right-4 text-[10px] text-gray-400 bg-white/50 px-1.5 py-0.5 rounded-md border border-black/5">
                {queryText.length}/200
              </span>
            </div>
            <Button
              type="primary"
              size="large"
              loading={loading}
              onClick={() => handleSearch(queryText)}
              disabled={!queryText.trim()}
              className="!rounded-xl text-white bg-[#40A5EE] hover:!bg-[#40A5EE]/90 border-none shadow-md shadow-blue-200/50"
            >
              开始测试
            </Button>
          </div>

          <div className="flex-1 overflow-hidden flex flex-col min-h-0">
            <SearchHistory
              pageNo={pageNo}
              setPageNo={setPageNo}
              pageSize={pageSize}
              setPageSize={setPageSize}
              total={data?.data?.total || 0}
              history={history}
              onSelect={selectRecord}
              selectedId={selectedRecord?.id!}
            />
          </div>
        </div>

        {/* Right Panel - Results */}
        <div className="flex-1 py-6 px-4 flex flex-col min-w-0 bg-white/40 backdrop-blur-sm rounded-2xl border border-white/60 shadow-sm overflow-hidden">
          <div className="border-b border-white/60 flex items-center justify-between">
            <div className="text-lg m-0">测试结果</div>
            {results.length > 0 && results[0].id !== '000' && (
              <span className="text-xs font-medium text-blue-500 bg-blue-50 px-2 py-1 rounded-md border border-blue-100/50">
                找到 {results.length} 条结果
              </span>
            )}
          </div>
          <div className="flex-1 overflow-hidden">
            {
              results.length > 0 ? (
                <SearchResults results={results} />
              ) : (
                <div className="flex flex-col items-center h-full py-20">
                <div className="mt-[16%]">
                  <img src={noSearchImg} className="w-[180px]" />
                </div>
                <div className="text-[#1D4A6B]">没有找到相关搜索结果</div>
              </div>
              )
            }
          </div>
        </div>
      </div>
    </div>
  );
};

export default RetrievalTest;
