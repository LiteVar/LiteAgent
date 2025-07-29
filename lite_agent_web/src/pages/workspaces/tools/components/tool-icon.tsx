import { FC } from 'react';
import { Image } from 'antd';

import placeholderIcon from '@/assets/agent/tool.png'
import { buildImageUrl } from '@/utils/buildImageUrl';

interface ToolIconProps {
  iconName: string | undefined;
}

const ToolIcon: FC<ToolIconProps> = ({ iconName }) => {
  
  const imgSrc = iconName 
    ? buildImageUrl(iconName)
    : placeholderIcon;

  return (
    <Image
      src={imgSrc}
      preview={false}
      alt="tool 图标"
      width={40}
      height={40}
      className="mr-4 rounded"
    />
  );
};

export default ToolIcon;