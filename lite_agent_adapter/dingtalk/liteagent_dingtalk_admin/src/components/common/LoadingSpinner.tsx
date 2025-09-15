import React from 'react';

interface LoadingSpinnerProps {
  message?: string;
  size?: 'sm' | 'md' | 'lg';
}

const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ 
  message = "加载中...", 
  size = 'md' 
}) => {
  const sizeClasses = {
    sm: 'h-6 w-6',
    md: 'h-8 w-8',
    lg: 'h-12 w-12'
  };

  return (
    <div className="flex items-center justify-center py-12">
      <div className="text-center">
        <div className={`inline-block ${sizeClasses[size]} animate-spin rounded-full border-4 border-solid border-primary border-r-transparent`}></div>
        {message && <p className="mt-4 text-gray-600 dark:text-gray-400">{message}</p>}
      </div>
    </div>
  );
};

export default LoadingSpinner;