import PageMeta from "../../components/common/PageMeta";
import { useSystem } from "../../hooks/useSystem";

export default function Home() {
  const { stats, version } = useSystem();

  return (
    <>
      <PageMeta
        title="Dashboard | LiteAgent Wechat Admin"
        description="LiteAgent 微信公众号机器人管理后台概览"
      />
      <div className="grid grid-cols-12 gap-4 md:gap-6">
        {/* 统计卡片 */}
        <div className="col-span-12">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 md:gap-6 xl:grid-cols-4 2xl:gap-7.5">
            <div className="rounded-lg border border-stroke bg-white px-7.5 py-6 shadow-default dark:border-strokedark dark:bg-boxdark">
              <div className="flex h-11.5 w-11.5 items-center justify-center rounded-full bg-meta-2 dark:bg-meta-4">
                <svg className="fill-primary dark:fill-white" width="22" height="16" viewBox="0 0 22 16">
                  <path d="M11 15.1156C4.19376 15.1156 0.825012 8.61876 0.687512 8.34376C0.584387 8.13751 0.584387 7.86251 0.687512 7.65626C0.825012 7.38126 4.19376 0.918762 11 0.918762C17.8063 0.918762 21.175 7.38126 21.3125 7.65626C21.4156 7.86251 21.4156 8.13751 21.3125 8.34376C21.175 8.61876 17.8063 15.1156 11 15.1156ZM2.26876 8.00001C3.02501 9.27189 5.98126 13.5688 11 13.5688C16.0188 13.5688 18.975 9.27189 19.7313 8.00001C18.975 6.72814 16.0188 2.43126 11 2.43126C5.98126 2.43126 3.02501 6.72814 2.26876 8.00001Z"/>
                  <path d="M11 10.9219C9.38438 10.9219 8.07812 9.61562 8.07812 8C8.07812 6.38438 9.38438 5.07812 11 5.07812C12.6156 5.07812 13.9219 6.38438 13.9219 8C13.9219 9.61562 12.6156 10.9219 11 10.9219ZM11 6.625C10.2437 6.625 9.625 7.24375 9.625 8C9.625 8.75625 10.2437 9.375 11 9.375C11.7563 9.375 12.375 8.75625 12.375 8C12.375 7.24375 11.7563 6.625 11 6.625Z"/>
                </svg>
              </div>
              
            </div>

            <div className="rounded-lg border border-stroke bg-white px-7.5 py-6 shadow-default dark:border-strokedark dark:bg-boxdark">
              <div className="flex h-11.5 w-11.5 items-center justify-center rounded-full bg-meta-2 dark:bg-meta-4">
                <svg className="fill-primary dark:fill-white" width="20" height="22" viewBox="0 0 20 22">
                  <path d="M11.7531 16.4312C10.3781 16.4312 9.27808 15.3312 9.27808 13.9562C9.27808 12.5812 10.3781 11.4812 11.7531 11.4812C13.1281 11.4812 14.2281 12.5812 14.2281 13.9562C14.2281 15.3312 13.1281 16.4312 11.7531 16.4312ZM11.7531 12.7812C11.0968 12.7812 10.5781 13.3 10.5781 13.9562C10.5781 14.6125 11.0968 15.1312 11.7531 15.1312C12.4094 15.1312 12.9281 14.6125 12.9281 13.9562C12.9281 13.3 12.4094 12.7812 11.7531 12.7812Z"/>
                  <path d="M11.7531 7.84374C10.3781 7.84374 9.27808 6.74374 9.27808 5.36874C9.27808 3.99374 10.3781 2.89374 11.7531 2.89374C13.1281 2.89374 14.2281 3.99374 14.2281 5.36874C14.2281 6.74374 13.1281 7.84374 11.7531 7.84374ZM11.7531 4.19374C11.0968 4.19374 10.5781 4.71249 10.5781 5.36874C10.5781 6.02499 11.0968 6.54374 11.7531 6.54374C12.4094 6.54374 12.9281 6.02499 12.9281 5.36874C12.9281 4.71249 12.4094 4.19374 11.7531 4.19374Z"/>
                </svg>
              </div>
              <div className="mt-4 flex items-end justify-between">
                <div>
                  <h4 className="text-title-md font-bold text-black dark:text-white">
                    {stats.agents}
                  </h4>
                  <span className="text-sm font-medium">Agent数量</span>
                </div>
              </div>
            </div>

            <div className="rounded-lg border border-stroke bg-white px-7.5 py-6 shadow-default dark:border-strokedark dark:bg-boxdark">
              <div className="flex h-11.5 w-11.5 items-center justify-center rounded-full bg-meta-2 dark:bg-meta-4">
                <svg className="fill-primary dark:fill-white" width="22" height="18" viewBox="0 0 22 18">
                  <path d="M7.18418 8.03751C9.31543 8.03751 11.0686 6.35313 11.0686 4.25626C11.0686 2.15938 9.31543 0.475006 7.18418 0.475006C5.05293 0.475006 3.2998 2.15938 3.2998 4.25626C3.2998 6.35313 5.05293 8.03751 7.18418 8.03751ZM7.18418 2.05626C8.45605 2.05626 9.52168 3.05313 9.52168 4.29063C9.52168 5.52813 8.49043 6.52501 7.18418 6.52501C5.87793 6.52501 4.84668 5.52813 4.84668 4.29063C4.84668 3.05313 5.9123 2.05626 7.18418 2.05626Z"/>
                  <path d="M15.8124 9.6875C17.6687 9.6875 19.1468 8.24375 19.1468 6.42188C19.1468 4.6 17.6343 3.15625 15.8124 3.15625C13.9905 3.15625 12.478 4.6 12.478 6.42188C12.478 8.24375 13.9905 9.6875 15.8124 9.6875ZM15.8124 4.7375C16.8093 4.7375 17.5999 5.49375 17.5999 6.45625C17.5999 7.41875 16.8093 8.175 15.8124 8.175C14.8155 8.175 14.0249 7.41875 14.0249 6.45625C14.0249 5.49375 14.8155 4.7375 15.8124 4.7375Z"/>
                </svg>
              </div>
              <div className="mt-4 flex items-end justify-between">
                <div>
                  <h4 className="text-title-md font-bold text-black dark:text-white">
                    {stats.users}
                  </h4>
                  <span className="text-sm font-medium">用户数量</span>
                </div>
              </div>
            </div>

            <div className="rounded-lg border border-stroke bg-white px-7.5 py-6 shadow-default dark:border-strokedark dark:bg-boxdark">
              <div className="flex h-11.5 w-11.5 items-center justify-center rounded-full bg-meta-2 dark:bg-meta-4">
                <svg className="fill-primary dark:fill-white" width="22" height="22" viewBox="0 0 22 22">
                  <path d="M21.1063 18.0469L19.3875 3.23126C19.2157 1.71876 17.9438 0.584381 16.3969 0.584381H5.56878C4.05628 0.584381 2.78441 1.71876 2.57816 3.23126L0.859406 18.0469C0.756281 18.9063 1.03128 19.7313 1.61566 20.3844C2.20003 21.0375 2.99378 21.3813 3.85316 21.3813H18.1469C19.0063 21.3813 19.8 21.0375 20.3844 20.3844C20.9688 19.7313 21.2438 18.9063 21.1063 18.0469ZM19.2157 19.3531C18.9407 19.6625 18.5625 19.8344 18.1469 19.8344H3.85316C3.4375 19.8344 3.05941 19.6625 2.78441 19.3531C2.50941 19.0438 2.37191 18.6313 2.44066 18.2157L4.12503 3.43751C4.19378 2.71563 4.81253 2.16563 5.56878 2.16563H16.4313C17.1875 2.16563 17.8063 2.71563 17.875 3.43751L19.5594 18.2157C19.6281 18.6313 19.4906 19.0438 19.2157 19.3531Z"/>
                  <path d="M14.3844 9.00001C13.7281 9.00001 13.2094 9.51876 13.2094 10.175V13.2281C13.2094 13.8844 13.7281 14.4031 14.3844 14.4031C15.0406 14.4031 15.5594 13.8844 15.5594 13.2281V10.175C15.5594 9.51876 15.0406 9.00001 14.3844 9.00001Z"/>
                </svg>
              </div>
              <div className="mt-4 flex items-end justify-between">
                <div>
                  <h4 className="text-title-md font-bold text-black dark:text-white">
                    {stats.permissions}
                  </h4>
                  <span className="text-sm font-medium">权限规则</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* 版本信息和快速操作 */}
        <div className="col-span-12 xl:col-span-8">
          <div className="rounded-lg border border-stroke bg-white p-6 shadow-default dark:border-strokedark dark:bg-boxdark">
            <h4 className="mb-6 text-xl font-semibold text-black dark:text-white">
              快速操作
            </h4>
            <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <button className="flex flex-col items-center rounded-lg border border-stroke p-4 hover:bg-gray-2 dark:border-strokedark dark:hover:bg-meta-4">
                <div className="mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
                  <svg className="fill-primary" width="24" height="24" viewBox="0 0 24 24">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                  </svg>
                </div>
                <span className="text-sm font-medium text-black dark:text-white">
                  添加公众号
                </span>
              </button>

              <button className="flex flex-col items-center rounded-lg border border-stroke p-4 hover:bg-gray-2 dark:border-strokedark dark:hover:bg-meta-4">
                <div className="mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-secondary/10">
                  <svg className="fill-secondary" width="24" height="24" viewBox="0 0 24 24">
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                  </svg>
                </div>
                <span className="text-sm font-medium text-black dark:text-white">
                  配置Agent
                </span>
              </button>

              <button className="flex flex-col items-center rounded-lg border border-stroke p-4 hover:bg-gray-2 dark:border-strokedark dark:hover:bg-meta-4">
                <div className="mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-success/10">
                  <svg className="fill-success" width="24" height="24" viewBox="0 0 24 24">
                    <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                </div>
                <span className="text-sm font-medium text-black dark:text-white">
                  设置权限
                </span>
              </button>
            </div>
          </div>
        </div>

        <div className="col-span-12 xl:col-span-4">
          <div className="rounded-lg border border-stroke bg-white p-6 shadow-default dark:border-strokedark dark:bg-boxdark">
            <h4 className="mb-6 text-xl font-semibold text-black dark:text-white">
              系统信息
            </h4>
            <div className="space-y-4">
              <div className="flex justify-between">
                <span className="text-black dark:text-white">当前版本:</span>
                <span className="font-medium text-primary">{version}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-black dark:text-white">运行状态:</span>
                <span className="inline-flex rounded-full bg-success bg-opacity-10 px-3 py-1 text-sm font-medium text-success">
                  正常
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-black dark:text-white">上次更新:</span>
                <span className="text-black dark:text-white">2024-01-15</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
