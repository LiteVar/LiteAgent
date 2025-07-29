import { FC } from 'react';
import { Image } from 'antd';

interface ImgIconProps {
  src: string;
  width?: number;
  onClick?: () => void;
  className?: string;
  style?: React.CSSProperties;
}

const ImgIcon: FC<ImgIconProps> = ({
  src,
  width,
  onClick = () => {},
  style = {},
  className = '',
}) => {

  const imgWidth = width || 40;

  return (
    <Image
      src={src}
      preview={false}
      alt="Img Icon"
      width={imgWidth}
      height={imgWidth}
      className={"mr-4 rounded " + className}
      onClick={onClick}
      style={style}
    />
  );
};

export default ImgIcon;