import React, { useState, useMemo } from 'react';
import { Input, Button, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import SearchHistory from './components/SearchHistory';
import SearchResults from './components/SearchResults';
import { useDatasetContext } from '@/contexts/datasetContext';
import { getV1DatasetByIdRetrieve, DocumentSegment, DatasetRetrieveHistory, getV1DatasetRetrieveHistoryById, SegmentVO } from '@/client';
import { getV1DatasetByIdRetrieveHistoryOptions } from '@/client/@tanstack/query.gen';
import { useQuery } from '@tanstack/react-query';
import ResponseCode from '@/constants/ResponseCode';

const { TextArea } = Input;
const noResult: DocumentSegment[] = [{ id: '000', content: '无没有找到相关搜索结果' }];
const RetrievalTest = () => {
  const { datasetInfo } = useDatasetContext();

  const [queryText, setQueryText] = useState('');
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState<SegmentVO[]>([]);
  const [selectedRecord, setSelectedRecord] = useState<DatasetRetrieveHistory | null>(null);
  const [pageNo, setPageNo] = useState(1);

  const { data, refetch } = useQuery({
    ...getV1DatasetByIdRetrieveHistoryOptions({
      query: {
        pageNo: pageNo,
        pageSize: 10,
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
      setResults(res.data.data?.length? res.data.data: noResult);
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
        setResults(noResult);
      }
    } else {
      message.error(res.data?.message || '获取检索记录失败');
    }

  };

  return (
    <div className="p-6 h-full">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 h-full">
        {/* Left Panel */}
        <div>
          <h3 className="text-lg font-medium mb-4">检索测试</h3>
          <div className="mb-6">
            <p className="text-gray-600 mb-2">根据给定的查询文本，测试知识库的命中效果。</p>
            <div className="mb-2">
              <TextArea
                value={queryText}
                onChange={(e) => setQueryText(e.target.value)}
                placeholder="输入查询文本..."
                maxLength={200}
                autoSize={{ minRows: 3, maxRows: 6 }}
                className="mb-1"
              />
              <div className="flex justify-between text-gray-400 text-sm">
                <span>{queryText.length}/200</span>
              </div>
            </div>
            <Button
              type="primary"
              icon={<SearchOutlined />}
              loading={loading}
              onClick={() => handleSearch(queryText)}
              disabled={!queryText.trim()}
              className="w-full"
            >
              开始测试
            </Button>
          </div>

          <SearchHistory
            pageNo={pageNo}
            setPageNo={setPageNo}
            total={data?.data?.total || 0}
            history={history}
            onSelect={selectRecord}
            selectedId={selectedRecord?.id!}
          />
        </div>

        {/* Right Panel */}
        <div>
          <h3 className="text-lg font-medium mb-4">测试结果</h3>
          <SearchResults results={results} />
        </div>
      </div>
    </div>
  );
};

export default RetrievalTest;
