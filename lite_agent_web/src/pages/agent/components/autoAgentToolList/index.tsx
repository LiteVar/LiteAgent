import React, { useState, useCallback, useEffect, useMemo } from 'react';
import {
	List,
	Button,
	Collapse, Divider,
} from 'antd';
import {
	UpOutlined,
	DownOutlined,
} from '@ant-design/icons';

import ToolIcon from '@/pages/workspaces/tools/components/tool-icon';
import { useQuery } from '@tanstack/react-query';
import { getV1ToolListWithFunctionOptions } from '@/client/@tanstack/query.gen';
import { ToolDTO } from '../../../../client';

const { Panel } = Collapse;

interface ToolsListProps {
	workspaceId: string;
	readonly?: boolean;
}

const AutoAgentToolsList: React.FC<ToolsListProps> = ({ workspaceId, readonly }) => {
	const [showAll, setShowAll] = useState(false);
	const [toolList, setToolList] = useState<ToolDTO[]>([]);

	const { data: tools } = useQuery({
		...getV1ToolListWithFunctionOptions({
			headers: {
				'Workspace-id': workspaceId!,
			},
			query: {
				autoAgent: true,
				tab: 0,
			},
		}),
		enabled: !!workspaceId,
	});

	const addToolNameIconToFunc = useCallback((toolList: ToolDTO[]) => {
		const newTools = { functionList: [] };
		toolList?.map(tool => {
			tool.functionList.map(func => {
				newTools.functionList.push({
					...func,
					toolName: tool.name,
					icon: tool.icon
				});
			})

		});
		return newTools;
	}, []);

	useEffect(() => {
		if (tools?.data) {
			//@ts-ignore
			setToolList(addToolNameIconToFunc(tools?.data || []));
		}
	}, [tools]);

	const displayTools = useMemo(() => {
		return showAll ? toolList?.functionList : toolList?.functionList?.slice(0, 2) || [];
	}, [showAll, toolList?.functionList]);

	const toUpperCaseWord = useCallback((word: string | undefined) => {
		return (word || '').toUpperCase();
	}, []);

	const navigateToolPage = useCallback((event: any) => {
		event.stopPropagation();
		window.open(`/workspaces/${workspaceId}/tools`, '_blank')
	}, [workspaceId]);

	return (
		<div className=''>
			<Divider />
			<Collapse ghost>
				<Panel
					header={<span className='text-base font-medium'>工具</span>}
					collapsible='header'
					key='1'
				>
					<div className='flex flex-col'>
						{toolList?.functionList?.length > 0 && <div className='flex justify-between items-center mb-4'>
							<div className='text-base text-gray-500'>
								在工具库中对工具开启“支持 Auto Multi Agent”后，工具将在此处显示。当指令调用工具时，将自动调用。
							</div>
						</div>}
						{toolList?.functionList?.length === 0 && (
							<div>
								{!readonly && <div>
									<span>还没添加可用工具，</span>
									<span className="text-blue-500 cursor-pointer" onClick={navigateToolPage}>前往设置</span>
								</div>}
								{!!readonly && <div>还没添加可用工具</div>}
							</div>
						)}
						{toolList?.functionList?.length > 0 && (
							<List
								className={`flex-grow ${showAll ? 'overflow-y-auto' : 'overflow-hidden'}`}
								style={{ maxHeight: showAll ? '29vh' : 'auto' }}
								dataSource={displayTools}
								renderItem={(func) => (
									<List.Item
										key={func.functionId}
										className="border rounded p-2 mb-2 hover:bg-gray-100"
									>
										<List.Item.Meta
											avatar={<ToolIcon iconName={func.icon} />}
											title={
												<div className='line-clamp-1'>
													<span className='mr-1'>
														{`${func.functionName}${toUpperCaseWord(func.requestMethod)}`}
													</span>
												</div>
											}
											description={<div className='w-full line-clamp-3'>{func.functionDesc}</div>}
										/>
									</List.Item>
								)}
							/>
						)}

						{toolList?.functionList?.length > 2 && (
							<div className='flex justify-center mt-2'>
								<Button
									type='link'
									onClick={() => setShowAll(!showAll)}
									icon={showAll ? <UpOutlined /> : <DownOutlined />}
									iconPosition='end'
								>
									{showAll ? '收起' : '更多'}
								</Button>
							</div>
						)}
					</div>
				</Panel>
			</Collapse>
		</div>
	);
};

export default AutoAgentToolsList;
