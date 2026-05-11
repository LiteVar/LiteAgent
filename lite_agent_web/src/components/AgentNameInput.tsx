import agentPcWhiteSvg from '@/assets/dashboard/agent-pc-white.svg';
import { Dropdown, Tooltip } from 'antd';
import { EllipsisOutlined, DeleteOutlined, Loading3QuartersOutlined } from '@ant-design/icons';
import { useCallback, useEffect, useRef, useState } from 'react';
import { AgentSessionVO, postV1ChatClearDebugRecord } from '@/client';
import { useNavigate } from 'react-router-dom';

interface AgentNameInputProps {
	agent: AgentSessionVO;
	workspaceId: string;
	collapsed: boolean;
	responding: boolean;
}

export default function AgentNameInput({ agent, workspaceId, responding, collapsed }: AgentNameInputProps) {
	const navigate = useNavigate();
	const textRef = useRef<HTMLSpanElement>(null);
	const [isOverflow, setIsOverflow] = useState(false);
	const [hovered, setHovered] = useState(false);
	const [dropdownOpen, setDropdownOpen] = useState(false);

	const checkOverflow = useCallback(() => {
		const el = textRef.current;
		if (!el) return;
		const next =
			el.scrollWidth > el.clientWidth + 1 || el.scrollHeight > el.clientHeight + 1;
		setIsOverflow(next);
	}, []);

	useEffect(() => {
		checkOverflow();
		const el = textRef.current;
		if (!el) return;

		const ro = new ResizeObserver(() => {
			requestAnimationFrame(checkOverflow);
		});
		ro.observe(el);
		if (el.parentElement) {
			ro.observe(el.parentElement);
		}

		return () => ro.disconnect();
	}, [agent.name, checkOverflow]);

	const onDeleteAgentChat = useCallback(async () => {
		await postV1ChatClearDebugRecord({
			query: {
				agentId: agent.agentId!,
				debugFlag: 0,
			},
		});
		navigate(`/dashboard/${workspaceId}`);
	}, [agent.agentId, navigate, workspaceId]);

	const agentDropDownItems = [
		{
			key: `agent-delete-${agent.agentId}`,
			label: (
				<div onClick={onDeleteAgentChat} className='flex items-center'>
					<DeleteOutlined />
					<div className='ml-2'>删除</div>
				</div>
			),
		},
	];

	return (
		<div
			className='flex items-center w-full min-w-0'
			onMouseEnter={() => setHovered(true)}
			onMouseLeave={() => setHovered(false)}
		>
			<Tooltip title={agent.name} open={(isOverflow && !collapsed) ? undefined : false}>
				<div className='h-10 flex-1 min-w-0 pr-2 flex items-center overflow-hidden'>
					<span
						ref={textRef}
						className='block w-full truncate text-[#383F44] text-sm leading-[22px]'
					>
						{agent.name}
					</span>
				</div>
			</Tooltip>
			{agent.localFlag && <img className='flex-none w-4 mr-2' src={agentPcWhiteSvg} />}
			{responding && <Loading3QuartersOutlined className='ml-1 text-[#94A0AB]' spin />}
			<div className='flex-none w-5 flex items-center justify-center'>
				{(hovered || dropdownOpen) && (
					<Dropdown
						menu={{ items: agentDropDownItems }}
						trigger={['click']}
						placement='bottom'
						onOpenChange={setDropdownOpen}
					>
						<EllipsisOutlined
							onClick={(e) => e.stopPropagation()}
							className='text-[#94A0AB] p-2 hover:text-[#383F44] transition-colors cursor-pointer'
						/>
					</Dropdown>
				)}
			</div>
		</div>
	);
}
