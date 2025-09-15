import React, { useState, useEffect } from 'react';
import { createRoot, Root } from 'react-dom/client';

interface MessageProps {
  type: 'success' | 'error' | 'warning' | 'info';
  content: string;
  duration?: number;
  onClose?: () => void;
}

const Message: React.FC<MessageProps> = ({ type, content, duration = 3000, onClose }) => {
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setVisible(false);
      setTimeout(() => {
        onClose?.();
      }, 300);
    }, duration);

    return () => clearTimeout(timer);
  }, [duration, onClose]);

  const getIcon = () => {
    switch (type) {
      case 'success':
        return '✅';
      case 'error':
        return '❌';
      case 'warning':
        return '⚠️';
      case 'info':
        return 'ℹ️';
      default:
        return 'ℹ️';
    }
  };

  const getTypeStyles = () => {
    switch (type) {
      case 'success':
        return 'bg-green-50 border-green-200 text-green-800 dark:bg-green-900/20 dark:border-green-800 dark:text-green-200';
      case 'error':
        return 'bg-red-50 border-red-200 text-red-800 dark:bg-red-900/20 dark:border-red-800 dark:text-red-200';
      case 'warning':
        return 'bg-yellow-50 border-yellow-200 text-yellow-800 dark:bg-yellow-900/20 dark:border-yellow-800 dark:text-yellow-200';
      case 'info':
        return 'bg-blue-50 border-blue-200 text-blue-800 dark:bg-blue-900/20 dark:border-blue-800 dark:text-blue-200';
      default:
        return 'bg-blue-50 border-blue-200 text-blue-800 dark:bg-blue-900/20 dark:border-blue-800 dark:text-blue-200';
    }
  };

  return (
    <div
      className={`fixed top-4 left-1/2 transform -translate-x-1/2 z-50 transition-all duration-300 ${
        visible ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-2'
      }`}
    >
      <div
        className={`flex items-center px-4 py-3 rounded-lg border shadow-lg ${getTypeStyles()}`}
      >
        <span className="mr-2 text-lg">{getIcon()}</span>
        <span className="text-sm font-medium">{content}</span>
        <button
          onClick={() => {
            setVisible(false);
            setTimeout(() => onClose?.(), 300);
          }}
          className="ml-4 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
        >
          ×
        </button>
      </div>
    </div>
  );
};

let messageContainer: HTMLDivElement | null = null;
let messageRoot: Root | null = null;
let messageCount = 0;

const getContainer = () => {
  if (!messageContainer) {
    messageContainer = document.createElement('div');
    messageContainer.id = 'message-container';
    messageContainer.style.position = 'fixed';
    messageContainer.style.top = '0';
    messageContainer.style.left = '0';
    messageContainer.style.width = '100%';
    messageContainer.style.height = '0';
    messageContainer.style.zIndex = '9999999';
    messageContainer.style.pointerEvents = 'none';
    document.body.appendChild(messageContainer);
  }
  if (!messageRoot) {
    messageRoot = createRoot(messageContainer);
  }
  return { container: messageContainer, root: messageRoot };
};

const message = {
  success: (content: string, duration?: number) => {
    return showMessage('success', content, duration);
  },
  error: (content: string, duration?: number) => {
    return showMessage('error', content, duration);
  },
  warning: (content: string, duration?: number) => {
    return showMessage('warning', content, duration);
  },
  info: (content: string, duration?: number) => {
    return showMessage('info', content, duration);
  },
};

const showMessage = (type: MessageProps['type'], content: string, duration = 3000) => {
  const { root } = getContainer();
  const messageId = ++messageCount;

  const destroy = () => {
    if (messageCount === messageId && messageContainer && messageRoot) {
      messageRoot.unmount();
      document.body.removeChild(messageContainer);
      messageContainer = null;
      messageRoot = null;
      messageCount = 0;
    }
  };

  root.render(
    <Message
      type={type}
      content={content}
      duration={duration}
      onClose={destroy}
    />
  );

  return destroy;
};

export default Message;
export { message };