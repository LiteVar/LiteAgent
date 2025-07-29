import React from 'react';
import { ArrowDownOutlined } from '@ant-design/icons';

interface ScrollToBottomProps {
    onClick: () => void;
}

const ScrollToBottom: React.FC<ScrollToBottomProps> = ({ onClick }) => {
    return (
        <div className="absolute top-0 left-1/2 z-50 transform -translate-x-1/2 -translate-y-8">
            <div
                className="text-black bg-white w-8 h-8 flex items-center justify-center border border-solid border-gray-200 rounded-full shadow-lg cursor-pointer hover:bg-gray-100"
                onClick={onClick}
            >
                <ArrowDownOutlined />
            </div>
        </div>
    );
};

export default ScrollToBottom;
