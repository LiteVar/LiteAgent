import { message } from "antd";
export const copyToClipboard = async (text: string): Promise<void> => {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text);
      message.success('已复制');
    } else {
      // 回退到 document.execCommand 方法
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed'; // 避免页面滚动
      textArea.style.opacity = '0'; // 隐藏元素
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
      message.success('已复制');
    }
  } catch (error) {
    console.error('复制失败:', error);
  }
};
