import React from 'react';

interface KnowledgeBaseIconProps {
  size?: number;
}

const KnowledgeBaseIcon: React.FC<KnowledgeBaseIconProps> = ({ size = 40 }) => {
  return (
    <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="40" height="40" rx="8" fill="url(#kb_gradient)" />
      <path
        d="M13.5 11C12.6716 11 12 11.6716 12 12.5V27.5C12 28.3284 12.6716 29 13.5 29H22.5C22.7652 29 23.0196 28.8946 23.2071 28.7071L28.7071 23.2071C28.8946 23.0196 29 22.7652 29 22.5V12.5C29 11.6716 28.3284 11 27.5 11H13.5ZM14.5 14C14.5 13.7239 14.7239 13.5 15 13.5H26C26.2761 13.5 26.5 13.7239 26.5 14C26.5 14.2761 26.2761 14.5 26 14.5H15C14.7239 14.5 14.5 14.2761 14.5 14ZM15 16.5C14.7239 16.5 14.5 16.7239 14.5 17C14.5 17.2761 14.7239 17.5 15 17.5H26C26.2761 17.5 26.5 17.2761 26.5 17C26.5 16.7239 26.2761 16.5 26 16.5H15ZM14.5 20C14.5 19.7239 14.7239 19.5 15 19.5H21C21.2761 19.5 21.5 19.7239 21.5 20C21.5 20.2761 21.2761 20.5 21 20.5H15C14.7239 20.5 14.5 20.2761 14.5 20Z"
        fill="white"
        fillRule="evenodd"
      />
      <defs>
        <linearGradient id="kb_gradient" x1="20" y1="0" x2="20" y2="40" gradientUnits="userSpaceOnUse">
          <stop stopColor="#5BB5F5" />
          <stop offset="1" stopColor="#2B8FE3" />
        </linearGradient>
      </defs>
    </svg>
  );
};

export default KnowledgeBaseIcon;
