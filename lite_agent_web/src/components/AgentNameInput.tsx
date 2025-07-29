import { Link, useNavigate } from 'react-router-dom';
import agentPcWhiteSvg from '@/assets/dashboard/agent-pc-white.svg';
import { Dropdown, Tooltip } from 'antd';
import { EllipsisOutlined, DeleteOutlined, Loading3QuartersOutlined } from '@ant-design/icons';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { AgentSessionVO, postV1ChatClearDebugRecord } from '@/client';
import { a } from 'vite/dist/node/types.d-aGj9QkWt';

interface AgentNameInputProps {
	agent: AgentSessionVO;
	workspaceId: string;
	collapsed: boolean;
	responding: boolean;
}

export default function AgentNameInput({ agent, workspaceId, collapsed, responding }: AgentNameInputProps) {
	const navigate = useNavigate();
	const textRef = useRef<HTMLAnchorElement>(null);
	const [isOverflow, setIsOverflow] = useState(false);
	const [dropdownOpen, setDropdownOpen] = useState(false);
	const [menuOpen, setMenuOpen] = useState(false);

	const checkOverflow = () => {
		if (textRef.current) {
			setIsOverflow(
				textRef.current.scrollWidth > textRef.current.clientWidth ||
				textRef.current.scrollHeight > textRef.current.clientHeight,
			);
		}
	};

	useEffect(() => {
		checkOverflow();
	}, [textRef]);

	const onDeleteAgentChat = useCallback(async () => {
		await postV1ChatClearDebugRecord({
			query: {
				agentId: agent.agentId,
				debugFlag: 0,
			},
		});
		navigate(`/dashboard/${workspaceId}`);
	}, [agent.agentId]);

	const agentDropDownItems = [
		{
			key: `agent-delete-${agent.agentId}`,
			label: <div onClick={onDeleteAgentChat} className='flex items-center'><DeleteOutlined />
				<div className='ml-2'>删除</div>
			</div>,
		},
	];

	const onMouseEnter = () => {
		setDropdownOpen(true);
	};

	const onMouseLeave = () => {
		setDropdownOpen(false);
	};

	return (
		<div className='flex items-center w-full' onMouseEnter={onMouseEnter} onMouseLeave={onMouseLeave}>
			<Tooltip title={agent.name} open={(isOverflow && !collapsed) ? undefined : false}>
				<div className='h-10 flex-1 pr-3 line-clamp-1 break-all text-white truncate'>
					<Link
						ref={textRef}
						to={`/dashboard/${workspaceId}/chat/${agent.agentId}`}
						className='inline-block w-full truncate'
					>
						{agent.name}
					</Link>
				</div>
			</Tooltip>
			{agent.localFlag && <img className='flex-none w-4 mr-2' src={agentPcWhiteSvg} />}
			{responding && <Loading3QuartersOutlined className='ml-2 text-[#fff]' spin />}
			{!dropdownOpen && !menuOpen && <div className='w-[18px] h-10'></div>}
			{(dropdownOpen || menuOpen) && <Dropdown onOpenChange={value => setMenuOpen(value)} menu={{ items: agentDropDownItems }} trigger={['click']} placement='bottom'>
				<a className='agentActionWrapper' onClick={(e) => e.preventDefault()}>
					<EllipsisOutlined />
				</a>
			</Dropdown>}
		</div>
	);
}