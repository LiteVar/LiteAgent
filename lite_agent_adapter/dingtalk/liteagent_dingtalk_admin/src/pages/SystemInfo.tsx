import PageMeta from "../components/common/PageMeta";
import { useSystem } from "../hooks/useSystem";

export default function SystemInfo() {
  const {
    version,
    userInfo,
    loading,
    error,
    fetchVersion,
    refreshAll,
  } = useSystem();

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="text-danger mb-4">❌ 加载失败</div>
          <p className="text-gray-600 dark:text-gray-400 mb-4">{error}</p>
          <button 
            onClick={refreshAll}
            className="px-4 py-2 bg-primary text-white rounded hover:bg-opacity-90"
          >
            重新加载
          </button>
        </div>
      </div>
    );
  }

  return (
    <>
      <PageMeta
        title="系统信息 | LiteAgent DingTalk Admin"
        description="查看系统版本信息和服务状态"
      />
      <div className="grid grid-cols-12 gap-4 md:gap-6">
        <div className="col-span-12 md:col-span-6">
          <div className="rounded-lg border border-stroke bg-white p-6 shadow-default dark:border-strokedark dark:bg-boxdark">
            <h3 className="mb-4 text-xl font-semibold text-black dark:text-white">
              系统版本
            </h3>
            <div className="space-y-4">
              <div className="flex justify-between">
                <span className="text-black dark:text-white">当前版本:</span>
                <span className="font-medium text-primary">
                  {loading ? "加载中..." : version || "v1.0.0"}
                </span>
              </div>
            
              <div className="flex justify-between">
                <span className="text-black dark:text-white">环境:</span>
                <span className="inline-flex rounded-full bg-success bg-opacity-10 px-3 py-1 text-sm font-medium text-success">
                  生产环境
                </span>
              </div>
              {userInfo && (
                <div className="flex justify-between">
                  <span className="text-black dark:text-white">当前用户:</span>
                  <span className="text-black dark:text-white">
                    {userInfo.name || userInfo.username || '管理员'}
                  </span>
                </div>
              )}
            </div>
            <button
              onClick={fetchVersion}
              disabled={loading}
              className="mt-4 inline-flex w-full items-center justify-center rounded-md bg-primary px-6 py-3 text-center font-medium text-white hover:bg-opacity-90 disabled:opacity-50"
            >
              {loading ? "检查中..." : "刷新版本信息"}
            </button>
          </div>
        </div>

        <div className="col-span-12 md:col-span-6">
          <div className="rounded-lg border border-stroke bg-white p-6 shadow-default dark:border-strokedark dark:bg-boxdark">
            <h3 className="mb-4 text-xl font-semibold text-black dark:text-white">
              服务状态
            </h3>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-black dark:text-white">API 服务:</span>
                <span className="inline-flex rounded-full bg-success bg-opacity-10 px-3 py-1 text-sm font-medium text-success">
                  正常
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-black dark:text-white">数据库连接:</span>
                <span className="inline-flex rounded-full bg-success bg-opacity-10 px-3 py-1 text-sm font-medium text-success">
                  正常
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-black dark:text-white">钉钉API:</span>
                <span className="inline-flex rounded-full bg-success bg-opacity-10 px-3 py-1 text-sm font-medium text-success">
                  正常
                </span>
              </div>
            
            </div>
            <button 
              onClick={refreshAll}
              disabled={loading}
              className="mt-4 inline-flex w-full items-center justify-center rounded-md bg-secondary px-6 py-3 text-center font-medium text-white hover:bg-opacity-90 disabled:opacity-50"
            >
              {loading ? "检查中..." : "检查服务状态"}
            </button>
          </div>
        </div>

        {/* <div className="col-span-12">
          <div className="rounded-lg border border-stroke bg-white p-6 shadow-default dark:border-strokedark dark:bg-boxdark">
            <h3 className="mb-4 text-xl font-semibold text-black dark:text-white">
              系统统计
            </h3>
            {loading ? (
              <div className="flex items-center justify-center py-8">
                <div className="inline-block h-6 w-6 animate-spin rounded-full border-4 border-solid border-primary border-r-transparent"></div>
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                <div className="rounded-lg bg-gray-2 p-4 dark:bg-meta-4">
                  <div className="text-2xl font-bold text-primary">{stats.robots}</div>
                  <div className="text-sm text-black dark:text-white">机器人数量</div>
                </div>
                <div className="rounded-lg bg-gray-2 p-4 dark:bg-meta-4">
                  <div className="text-2xl font-bold text-secondary">{stats.agents}</div>
                  <div className="text-sm text-black dark:text-white">Agent数量</div>
                </div>
                <div className="rounded-lg bg-gray-2 p-4 dark:bg-meta-4">
                  <div className="text-2xl font-bold text-success">{stats.users}</div>
                  <div className="text-sm text-black dark:text-white">用户数量</div>
                </div>
                <div className="rounded-lg bg-gray-2 p-4 dark:bg-meta-4">
                  <div className="text-2xl font-bold text-warning">{stats.permissions}</div>
                  <div className="text-sm text-black dark:text-white">权限规则</div>
                </div>
              </div>
            )}
          </div>
        </div> */}
      </div>
    </>
  );
}