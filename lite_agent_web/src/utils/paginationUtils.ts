export interface Pagination {
  current: number;
  pageSize: number;
}

export const handlePaginationAfterDelete = (
  total: number,
  itemsToDelete: number,
  pagination: Pagination,
  setPagination: (value: Pagination) => void
) => {
  const totalAfterDelete = total - itemsToDelete;
  const totalPages = Math.ceil(totalAfterDelete / pagination.pageSize);

  if (pagination.current > totalPages && totalPages > 0) {
    setPagination({
      ...pagination,
      current: totalPages,
    });
  }
};
