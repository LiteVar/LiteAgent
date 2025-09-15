interface PaginationProps {
  currentPage: number;
  totalSize: number;
  pageSize: number;
  loading: boolean;
  onPageChange: (newPage: number) => void;
}

export default function Pagination({ 
  currentPage, 
  totalSize, 
  pageSize, 
  loading, 
  onPageChange 
}: PaginationProps) {
  const totalPages = Math.ceil(totalSize / pageSize);

  if (totalSize === 0) return null;

  return (
    <div className="mt-6 flex items-center justify-between">
      <div className="text-sm text-gray-600 dark:text-gray-400">
        共 {totalSize} 条记录，第 {currentPage} / {totalPages} 页
      </div>
      <div className="flex items-center space-x-2">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage <= 1 || loading}
          className="px-3 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed dark:border-gray-600 dark:hover:bg-gray-700"
        >
          上一页
        </button>
        <span className="px-3 py-2 text-sm text-gray-600 dark:text-gray-400">
          {currentPage}
        </span>
        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages || loading}
          className="px-3 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed dark:border-gray-600 dark:hover:bg-gray-700"
        >
          下一页
        </button>
      </div>
    </div>
  );
}