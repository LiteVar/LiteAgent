import { useState } from "react";
import PageMeta from "../components/common/PageMeta";
import Button from "../components/ui/button/Button";
import { message } from "../components/ui/message";
import { useWechatAccounts } from "../hooks/useWechatAccounts";
import { AgentWxRefDto } from "../api/types.gen";
import WechatModal from "../components/wechat/WechatModal";
import WechatDeleteConfirmModal from "../components/wechat/WechatDeleteConfirmModal";
import WechatTable from "../components/wechat/WechatTable";
import SearchForm from "../components/wechat/SearchForm";
import Pagination from "../components/wechat/Pagination";

export default function WechatManagement() {
  const {
    wechatAccounts,
    pagination,
    loading,
    error,
    creating,
    updating,
    deleting,
    deleteWechatAccount,
    createWechatAccount,
    updateWechatAccount,
    fetchWechatAccounts,
  } = useWechatAccounts();

  const [searchTerm, setSearchTerm] = useState("");
  const [editingWechatAccount, setEditingWechatAccount] = useState<AgentWxRefDto | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);

  const handleSearch = (searchValue: string) => {
    setSearchTerm(searchValue);
    fetchWechatAccounts({ search: searchValue, pageNum: 1 });
  };

  const handleDelete = async (id: string) => {
    const result = await deleteWechatAccount(id);
    if (result.success) {
      message.success(result.message);
      setShowDeleteConfirm(null);
    } else {
      message.error(result.message);
    }
  };

  const handleEdit = (wechatAccount: AgentWxRefDto) => {
    setEditingWechatAccount(wechatAccount);
    setShowModal(true);
  };

  const handleAdd = () => {
    setEditingWechatAccount(null);
    setShowModal(true);
  };

  const handleSave = async (wechatAccountData: AgentWxRefDto) => {
    let result;
    if (editingWechatAccount?.id) {
      result = await updateWechatAccount({ ...wechatAccountData, id: editingWechatAccount.id });
    } else {
      result = await createWechatAccount(wechatAccountData);
    }
    
    if (result.success) {
      message.success(result.message);
      setShowModal(false);
      setEditingWechatAccount(null);
    } else {
      message.error(result.message);
    }
    fetchWechatAccounts();
  };

  const handlePageChange = (newPage: number) => {
    fetchWechatAccounts({ 
      pageNum: newPage, 
      pageSize: pagination.pageSize,
      search: searchTerm 
    });
  };



  if (error) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="text-danger mb-4">❌ 加载失败</div>
          <p className="text-gray-600 dark:text-gray-400 mb-4">{error}</p>
          <Button onClick={() => fetchWechatAccounts()}>
            重新加载
          </Button>
        </div>
      </div>
    );
  }

  return (
    <>
      <PageMeta
        title="公众号管理 | LiteAgent Wechat Admin"
        description="管理微信公众号与 LiteAgent Agent 的绑定关系"
      />
      <div className="grid grid-cols-12 gap-4 md:gap-6">
        <div className="col-span-12">
          <div className="rounded-lg border border-stroke bg-white p-6 shadow-default dark:border-strokedark dark:bg-boxdark">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-title-md2 font-semibold text-black dark:text-white">
                公众号管理
              </h2>
              <Button 
                onClick={handleAdd}
                disabled={creating}
                className="inline-flex items-center justify-center rounded-md bg-primary px-6 py-3 text-center font-medium text-white hover:bg-opacity-90 disabled:opacity-50"
              >
                {creating ? "添加中..." : "添加公众号"}
              </Button>
            </div>

            <div className="mb-6">
              <SearchForm 
                onSearch={handleSearch}
                loading={loading}
                initialValue={searchTerm}
              />
            </div>
            
            <WechatTable
              wechatAccounts={wechatAccounts}
              loading={loading}
              updating={updating}
              deleting={deleting}
              onEdit={handleEdit}
              onDelete={(id) => setShowDeleteConfirm(id)}
            />
            
            <Pagination
              currentPage={pagination.pageNum}
              totalSize={pagination.totalSize}
              pageSize={pagination.pageSize}
              loading={loading}
              onPageChange={handlePageChange}
            />
          </div>
        </div>
      </div>
      
      <WechatModal
        isOpen={showModal}
        wechatAccount={editingWechatAccount}
        onSave={handleSave}
        onCancel={() => {
          setShowModal(false);
          setEditingWechatAccount(null);
        }}
        loading={creating || updating}
      />
      
      <WechatDeleteConfirmModal
        isOpen={!!showDeleteConfirm}
        wechatAccountId={showDeleteConfirm || ""}
        onConfirm={() => handleDelete(showDeleteConfirm!)}
        onCancel={() => setShowDeleteConfirm(null)}
        loading={deleting}
      />
      

    </>
  );
}