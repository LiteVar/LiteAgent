import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { Button, Spin, message, DatePicker, Select } from 'antd';
import * as echarts from 'echarts';
import dayjs, { Dayjs } from 'dayjs';
import { postV1PluginConnectorAnalyze } from '@/client';
import ResponseCode from '@/constants/ResponseCode';
import Papa from 'papaparse';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';
import userImg from '@/assets/plugin/user.png';
import conversationImg from '@/assets/plugin/conversation.png';
import sessionImg from '@/assets/plugin/session.png';
import tokenImg from '@/assets/plugin/token.png';

const { RangePicker } = DatePicker;

interface StatisticsCardData {
  title: string;
  value: string | number;
  description: string;
}

type TimeRange = 'today' | 'last7days' | 'thisMonth' | 'lastMonth' | 'custom';

const TIME_RANGE_OPTIONS: { value: TimeRange; label: string }[] = [
  { value: 'today', label: '今日' },
  { value: 'last7days', label: '近7天' },
  { value: 'thisMonth', label: '本月' },
  { value: 'lastMonth', label: '上个月' },
  { value: 'custom', label: '自定义' },
];

interface AnalyticsData {
  statistics: {
    activeUserCount: number;
    averageConversationRounds: number;
    averageSessionsPerActiveUser: number;
    totalTokenConsumption: number;
  };
  activeUserTrend: Array<{
    time: string;
    activeUserCount: number;
  }>;
  conversationRoundsDistribution: Array<{
    roundsRange: string;
    count: number;
  }>;
  sessionDistribution: Array<{
    sessionRange: string;
    userCount: number;
  }>;
}

const PluginConnectAnalytics: React.FC = () => {
  const { connectorId } = useParams<{ connectorId: string }>();
  const [searchParams] = useSearchParams();
  const connectorName = searchParams.get('name') || '插件智连';

  const [loading, setLoading] = useState<boolean>(false);
  const [analyticsData, setAnalyticsData] = useState<AnalyticsData | null>(null);
  const [timeRange, setTimeRange] = useState<TimeRange>('last7days');
  const [customDateRange, setCustomDateRange] = useState<[Dayjs, Dayjs] | null>(null);

  const trendChartRef = useRef<HTMLDivElement>(null);
  const roundsChartRef = useRef<HTMLDivElement>(null);
  const sessionChartRef = useRef<HTMLDivElement>(null);

  const trendChartInstance = useRef<echarts.ECharts | null>(null);
  const roundsChartInstance = useRef<echarts.ECharts | null>(null);
  const sessionChartInstance = useRef<echarts.ECharts | null>(null);

  const getTimeRangeValues = useCallback((range: TimeRange): [string, string] => {
    const now = dayjs();
    let startTime: Dayjs;
    let endTime: Dayjs = now;

    switch (range) {
      case 'today':
        startTime = now.startOf('day');
        break;
      case 'last7days':
        startTime = now.subtract(6, 'day').startOf('day');
        break;
      case 'thisMonth':
        startTime = now.startOf('month');
        break;
      case 'lastMonth':
        startTime = now.subtract(1, 'month').startOf('month');
        endTime = now.subtract(1, 'month').endOf('month');
        break;
      case 'custom':
        if (customDateRange) {
          return [
            customDateRange[0].format('YYYY-MM-DD HH:mm:ss'),
            customDateRange[1].format('YYYY-MM-DD HH:mm:ss'),
          ];
        }
        startTime = now.subtract(6, 'day').startOf('day');
        break;
      default:
        startTime = now.subtract(6, 'day').startOf('day');
    }

    return [startTime.format('YYYY-MM-DD HH:mm:ss'), endTime.format('YYYY-MM-DD HH:mm:ss')];
  }, [customDateRange]);

  const getTimeRangeLabel = useCallback((): string => {
    switch (timeRange) {
      case 'today': return '今日';
      case 'last7days': return '近7天';
      case 'thisMonth': return '本月';
      case 'lastMonth': return '上个月';
      case 'custom':
        if (customDateRange) {
          return `${customDateRange[0].format('YYYY-MM-DD')} - ${customDateRange[1].format('YYYY-MM-DD')}`;
        }
        return '自定义';
      default: return '近7天';
    }
  }, [timeRange, customDateRange]);

  const fetchAnalyticsData = useCallback(async () => {
    if (!connectorId) return;

    setLoading(true);
    try {
      const [startTime, endTime] = getTimeRangeValues(timeRange);
      const response = await postV1PluginConnectorAnalyze({
        body: { connectorId, startTime, endTime },
      });

      if (response.data?.code === ResponseCode.S_OK) {
        setAnalyticsData(response.data.data as unknown as AnalyticsData);
      } else {
        setAnalyticsData(null);
      }
    } catch (error) {
      console.error('获取统计数据失败:', error);
      setAnalyticsData(null);
    } finally {
      setLoading(false);
    }
  }, [connectorId, timeRange, getTimeRangeValues]);

  useEffect(() => {
    fetchAnalyticsData();
  }, [fetchAnalyticsData]);

  useEffect(() => {
    if (!analyticsData || !trendChartRef.current) return;

    if (!trendChartInstance.current) {
      trendChartInstance.current = echarts.init(trendChartRef.current);
    }

    const chart = trendChartInstance.current;
    const data = analyticsData.activeUserTrend;

    const option: echarts.EChartsOption = {
      grid: { left: '50px', right: '20px', top: '20px', bottom: '40px' },
      xAxis: {
        type: 'category',
        data: data.map((item) => dayjs(item.time).format('MM-DD')),
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#6b7280' },
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f3f4f6' } },
        axisLabel: { color: '#6b7280' },
      },
      series: [
        {
          data: data.map((item) => item.activeUserCount),
          type: 'line',
          smooth: true,
          lineStyle: { color: '#40A5EE', width: 2 },
          itemStyle: { color: '#40A5EE' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64, 165, 238, 0.3)' },
              { offset: 1, color: 'rgba(64, 165, 238, 0)' },
            ]),
          },
        },
      ],
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        borderColor: 'transparent',
        textStyle: { color: '#fff' },
      },
    };

    chart.setOption(option);
    setTimeout(() => chart.resize(), 100);

    const handleResize = () => chart.resize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [analyticsData]);

  useEffect(() => {
    if (!analyticsData || !roundsChartRef.current) return;

    if (!roundsChartInstance.current) {
      roundsChartInstance.current = echarts.init(roundsChartRef.current);
    }

    const chart = roundsChartInstance.current;
    const data = analyticsData.conversationRoundsDistribution;

    const option: echarts.EChartsOption = {
      grid: { left: '50px', right: '20px', top: '20px', bottom: '40px' },
      xAxis: {
        type: 'category',
        data: data.map((item) => item.roundsRange),
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#6b7280' },
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f3f4f6' } },
        axisLabel: { color: '#6b7280' },
      },
      series: [
        {
          data: data.map((item) => item.count),
          type: 'bar',
          barWidth: '40%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(56, 135, 254, 1)' },
              { offset: 1, color: 'rgba(184, 233, 255, 1)' },
            ]),
            borderRadius: [6, 6, 6, 6],
          },
        },
      ],
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        borderColor: 'transparent',
        textStyle: { color: '#fff' },
      },
    };

    chart.setOption(option);
    setTimeout(() => chart.resize(), 100);

    const handleResize = () => chart.resize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [analyticsData]);

  useEffect(() => {
    if (!analyticsData || !sessionChartRef.current) return;

    if (!sessionChartInstance.current) {
      sessionChartInstance.current = echarts.init(sessionChartRef.current);
    }

    const chart = sessionChartInstance.current;
    const data = analyticsData.sessionDistribution;

    const option: echarts.EChartsOption = {
      grid: { left: '50px', right: '20px', top: '20px', bottom: '40px' },
      xAxis: {
        type: 'category',
        data: data.map((item) => item.sessionRange),
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#6b7280' },
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f3f4f6' } },
        axisLabel: { color: '#6b7280' },
      },
      series: [
        {
          data: data.map((item) => item.userCount),
          type: 'bar',
          barWidth: '40%',
          itemStyle: {
            color: '#64b6f1',
            borderRadius: [6, 6, 6, 6],
          },
        },
      ],
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        borderColor: 'transparent',
        textStyle: { color: '#fff' },
      },
    };

    chart.setOption(option);
    setTimeout(() => chart.resize(), 100);

    const handleResize = () => chart.resize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [analyticsData]);

  useEffect(() => {
    return () => {
      trendChartInstance.current?.dispose();
      roundsChartInstance.current?.dispose();
      sessionChartInstance.current?.dispose();
    };
  }, []);

  const handleExportCSV = () => {
    if (!analyticsData) {
      message.warning('暂无数据可导出');
      return;
    }

    try {
      const csvData: (string | number)[][] = [];
      csvData.push(['插件智连数据分析报告']);
      csvData.push(['插件名称', connectorName]);
      csvData.push(['时间范围', getTimeRangeLabel()]);
      csvData.push(['导出时间', dayjs().format('YYYY-MM-DD HH:mm:ss')]);
      csvData.push([]);

      csvData.push(['核心统计指标']);
      csvData.push(['指标名称', '数值', '说明']);
      csvData.push(['活跃用户量', analyticsData.statistics.activeUserCount, getTimeRangeLabel()]);
      csvData.push(['平均对话轮数', analyticsData.statistics.averageConversationRounds.toFixed(1), '每次对话平均轮数']);
      csvData.push(['人均对话会话数', analyticsData.statistics.averageSessionsPerActiveUser.toFixed(1), '每位活跃用户平均会话数']);
      csvData.push(['Token总消耗', analyticsData.statistics.totalTokenConsumption, '所选时间段总消耗']);
      csvData.push([]);

      csvData.push(['活跃用户量时间趋势']);
      csvData.push(['日期', '活跃用户数']);
      analyticsData.activeUserTrend.forEach(item => {
        csvData.push([dayjs(item.time).format('YYYY-MM-DD'), item.activeUserCount]);
      });
      csvData.push([]);

      csvData.push(['对话轮数分布']);
      csvData.push(['轮数范围', '用户数量']);
      analyticsData.conversationRoundsDistribution.forEach(item => {
        csvData.push([item.roundsRange, item.count]);
      });
      csvData.push([]);

      csvData.push(['用户对话会话分布']);
      csvData.push(['会话数范围', '用户数量']);
      analyticsData.sessionDistribution.forEach(item => {
        csvData.push([item.sessionRange, item.userCount]);
      });

      const csv = Papa.unparse(csvData, { quotes: true, delimiter: ',' });
      const BOM = '\uFEFF';
      const blob = new Blob([BOM + csv], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', `${connectorName}_数据分析_${dayjs().format('YYYYMMDD_HHmmss')}.csv`);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      message.success('CSV 导出成功！');
    } catch (error) {
      console.error('CSV 导出失败:', error);
      message.error('CSV 导出失败，请重试');
    }
  };

  const handleExportPDF = async () => {
    if (!analyticsData) {
      message.warning('暂无数据可导出');
      return;
    }

    const hide = message.loading('正在生成 PDF，请稍候...', 0);
    try {
      const element = document.querySelector('.analytics-container') as HTMLElement;
      if (!element) throw new Error('找不到要导出的内容');

      const canvas = await html2canvas(element, {
        scale: 2,
        useCORS: true,
        logging: false,
        backgroundColor: '#f6f8fa',
      });

      const imgData = canvas.toDataURL('image/png');
      const imgWidth = 190;
      const pageHeight = 277;
      const imgHeight = (canvas.height * imgWidth) / canvas.width;

      const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
      let heightLeft = imgHeight;
      let position = 10;

      pdf.addImage(imgData, 'PNG', 10, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;

      while (heightLeft > 0) {
        position = heightLeft - imgHeight + 10;
        pdf.addPage();
        pdf.addImage(imgData, 'PNG', 10, position, imgWidth, imgHeight);
        heightLeft -= pageHeight;
      }

      pdf.save(`${connectorName}_数据分析_${dayjs().format('YYYYMMDD_HHmmss')}.pdf`);
      hide();
      message.success('PDF 导出成功！');
    } catch (error) {
      hide();
      console.error('PDF 导出失败:', error);
      message.error('PDF 导出失败，请重试');
    }
  };

  const statisticsCards: StatisticsCardData[] = analyticsData
    ? [
        {
          title: '活跃用户量',
          value: analyticsData.statistics.activeUserCount.toLocaleString(),
          description: getTimeRangeLabel(),
        },
        {
          title: '平均对话轮数',
          value: analyticsData.statistics.averageConversationRounds.toFixed(1),
          description: '每次对话平均轮数',
        },
        {
          title: '人均对话会话数',
          value: analyticsData.statistics.averageSessionsPerActiveUser.toFixed(1),
          description: '每位活跃用户平均会话数',
        },
        {
          title: 'Token总消耗',
          value: analyticsData.statistics.totalTokenConsumption.toLocaleString(),
          description: '所选时间段总消耗',
        },
      ]
    : [
        { title: '活跃用户量', value: '0', description: getTimeRangeLabel() },
        { title: '平均对话轮数', value: '0', description: '每次对话平均轮数' },
        { title: '人均对话会话数', value: '0', description: '每位活跃用户平均会话数' },
        { title: 'Token总消耗', value: '0', description: '所选时间段总消耗' },
      ];

  const cardImages = [userImg, conversationImg, sessionImg, tokenImg];

  return (
    <div className="min-h-screen w-full bg-[#dfeff7]">
      <Spin spinning={loading} className="w-full h-full">
        <div className="analytics-container w-full flex flex-col">
          {/* 顶部栏 */}
          <div className="flex justify-between items-center px-4 py-6">
            <div className="flex items-center gap-4 pl-2">
              <h1 className="text-[18px] font-medium text-[#1D4A6B] m-0">
                {connectorName}_数据分析
              </h1>
            </div>
            {/* <div className="flex items-center gap-4 pr-4">
              <Button
                className="h-10 px-4 rounded-xl bg-[#dfeff7] border-[#40A5EE] text-[#40A5EE] hover:bg-[#40A5EE]/5"
                onClick={handleExportCSV}
              >
                导出 CSV
              </Button>
              <Button
                type="primary"
                className="h-10 px-4 rounded-xl bg-[#40A5EE] border-[#40A5EE] hover:bg-[#40A5EE]/90"
                onClick={handleExportPDF}
              >
                导出 PDF
              </Button>
            </div> */}
          </div>

          {/* 内容区域 */}
          <div className="flex-1 px-4 pb-4">
            {/* 主内容卡片容器 */}
            <div
              className="bg-white/60 backdrop-blur-sm border border-white/80 rounded-2xl flex flex-col gap-4 p-[22px_16px]"
              style={{ boxShadow: 'inset 0 0 8px rgba(255, 255, 255, 1)' }}
            >
              {/* 筛选区域 */}
              <div className="flex items-center gap-2">
                <Select
                  value={timeRange}
                  onChange={(value) => {
                    setTimeRange(value);
                    if (value !== 'custom') setCustomDateRange(null);
                  }}
                  options={TIME_RANGE_OPTIONS}
                  className="analytics-filter-select"
                  style={{ width: 110 }}
                  popupMatchSelectWidth={false}
                />
                {timeRange === 'custom' && (
                  <RangePicker
                    value={customDateRange}
                    onChange={(dates) => {
                      if (dates && dates[0] && dates[1]) {
                        setCustomDateRange([dates[0], dates[1]]);
                      } else {
                        setCustomDateRange(null);
                        setTimeRange('last7days');
                      }
                    }}
                    className="rounded-xl border-white/80 bg-white/40 h-10"
                  />
                )}
              </div>

              {/* 统计卡片 */}
              <div className="flex gap-2 flex-wrap">
                {statisticsCards.map((card, index) => (
                  <div
                    key={index}
                    className="relative flex-1 min-w-[200px] bg-white/60 border border-white/80 rounded-2xl p-4 overflow-hidden"
                    style={{ boxShadow: 'inset 0 0 8px rgba(255, 255, 255, 1)' }}
                  >
                    {/* 右下角装饰背景图 */}
                    <img
                      src={cardImages[index]}
                      alt=""
                      className="absolute bottom-[-12px] right-0 w-[124px] h-[124px] object-contain pointer-events-none select-none"
                    />
                    {/* 卡片内容 */}
                    <div className="relative z-10 flex flex-col gap-2">
                      <h3 className="text-[#1D4A6B] text-[18px] font-medium m-0 leading-snug">
                        {card.title}
                      </h3>
                      <div>
                        <div className="text-[30px] font-semibold text-[#1D4A6B] leading-tight">
                          {card.value}
                        </div>
                        <div className="text-[#7C8B98] text-[12px] mt-1">
                          {card.description}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* 图表区域 */}
              <div className="flex gap-4">
                {/* 活跃用户量时间趋势 */}
                <div className="flex-1 bg-white/60 border border-white/80 rounded-2xl p-4 flex flex-col gap-4">
                  <h3 className="text-[16px] font-medium text-[#383F44] m-0">
                    活跃用户量时间趋势
                  </h3>
                  <div ref={trendChartRef} style={{ width: '100%', height: '280px' }} />
                </div>

                {/* 对话轮数分布 */}
                <div className="flex-1 bg-white/60 border border-white/80 rounded-2xl p-4 flex flex-col gap-4">
                  <h3 className="text-[16px] font-medium text-[#383F44] m-0">
                    对话轮数分布
                  </h3>
                  <div ref={roundsChartRef} style={{ width: '100%', height: '280px' }} />
                </div>
              </div>

              {/* 用户对话会话分布 */}
              <div className="bg-white/60 border border-white/80 rounded-2xl p-4 flex flex-col gap-4">
                <h3 className="text-[16px] font-medium text-[#383F44] m-0">
                  用户对话会话分布
                </h3>
                <div ref={sessionChartRef} style={{ width: '100%', height: '280px' }} />
              </div>
            </div>
          </div>
        </div>
      </Spin>
    </div>
  );
};

export default PluginConnectAnalytics;
