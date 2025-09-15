import { Table, TableHeader, TableBody, TableRow, TableCell } from "../ui/table";
import { AgentWxRefDto } from "../../api/types.gen";

interface RobotTableProps {
  robots: AgentWxRefDto[];
  loading: boolean;
  updating: boolean;
  deleting: boolean;
  onEdit: (robot: AgentWxRefDto) => void;
  onDelete: (id: string) => void;
}

export default function RobotTable({ 
  robots, 
  loading, 
  updating, 
  deleting, 
  onEdit, 
  onDelete,
}: RobotTableProps) {
  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-center">
          <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-primary border-r-transparent"></div>
          <p className="mt-4 text-gray-600 dark:text-gray-400">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <Table className="w-full table-auto">
        <TableHeader>
          <TableRow className="bg-gray-2 text-left dark:bg-meta-4">
            <TableCell 
              isHeader 
              className="min-w-[220px] px-4 py-4 font-medium text-black dark:text-white xl:pl-11"
            >
              公众号AppID
            </TableCell>
            <TableCell 
              isHeader 
              className="min-w-[150px] px-4 py-4 font-medium text-black dark:text-white"
            >
              公众号名称
            </TableCell>
            <TableCell 
              isHeader 
              className="min-w-[150px] px-4 py-4 font-medium text-black dark:text-white"
            >
              Agent API Key
            </TableCell>
            <TableCell 
              isHeader 
              className="min-w-[150px] px-4 py-4 font-medium text-black dark:text-white"
            >
              Agent Base URL
            </TableCell>
            <TableCell 
              isHeader 
              className="min-w-[120px] px-4 py-4 font-medium text-black dark:text-white"
            >
              更新时间
            </TableCell>

            <TableCell 
              isHeader 
              className="px-4 py-4 font-medium text-black dark:text-white"
            >
              操作
            </TableCell>
          </TableRow>
        </TableHeader>
        <TableBody>
          {robots.length === 0 ? (
            <tr>
              <td colSpan={6} className="px-4 py-8 text-center text-gray-500 dark:text-gray-400">
                暂无数据
              </td>
            </tr>
          ) : (
            robots.map((robot) => (
              <TableRow key={robot.id}>
                <TableCell className="border-b border-[#eee] px-4 py-5 pl-9 dark:border-strokedark xl:pl-11">
                  <span className="font-medium text-black dark:text-white">
                    {robot.appId}
                  </span>
                </TableCell>
                <TableCell className="border-b border-[#eee] px-4 py-5 dark:border-strokedark">
                  <span className="text-black dark:text-white">
                    {robot.name}
                  </span>
                </TableCell>
                <TableCell className="border-b border-[#eee] px-4 py-5 dark:border-strokedark">
                  <span className="text-black dark:text-white">
                    {robot.agentApiKey ? `${robot.agentApiKey.substring(0, 10)}...` : '-'}
                  </span>
                </TableCell>
                <TableCell className="border-b border-[#eee] px-4 py-5 dark:border-strokedark">
                  <span className="text-black dark:text-white">
                    {robot.agentBaseUrl}
                  </span>
                </TableCell>
                <TableCell className="border-b border-[#eee] px-4 py-5 dark:border-strokedark">
                  <span className="text-black dark:text-white">
                    {robot.updateTime ? new Date(robot.updateTime).toLocaleDateString() : new Date(robot.createTime!).toLocaleDateString()}
                  </span>
                </TableCell>

                <TableCell className="border-b border-[#eee] px-4 py-5 dark:border-strokedark">
                  <div className="flex items-center space-x-2">
                    <button 
                      onClick={() => onEdit(robot)}
                      disabled={updating}
                      className="text-blue-500 hover:text-primary disabled:opacity-50 text-sm px-2 py-1"
                    >
                      {updating ? "处理中..." : "编辑"}
                    </button>
                    
                    <button 
                      onClick={() => onDelete(robot.id!)}
                      disabled={deleting}
                      className="text-red-500 hover:text-danger disabled:opacity-50 text-sm px-2 py-1"
                    >
                      {deleting ? "删除中..." : "删除"}
                    </button>
                  </div>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </div>
  );
}