import { Modal } from "../ui/modal";
import Button from "../ui/button/Button";

interface WechatDeleteConfirmModalProps {
  isOpen: boolean;
  wechatAccountId: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading: boolean;
}

export default function WechatDeleteConfirmModal({ 
  isOpen,
  wechatAccountId, 
  onConfirm, 
  onCancel, 
  loading 
}: WechatDeleteConfirmModalProps) {
  return (
    <Modal 
      isOpen={isOpen} 
      onClose={onCancel}
      showCloseButton={false}
      className="w-full max-w-md"
    >
      <div className="p-6">
        <div className="mb-4 text-center">
          <div className="mx-auto mb-4 w-16 h-16 flex items-center justify-center rounded-full bg-red-100 dark:bg-red-900">
            <svg className="w-8 h-8 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-black dark:text-white mb-2">
            确认删除
          </h3>
          <p className="text-gray-600 dark:text-gray-400">
            确定要删除公众号 <span className="font-medium text-black dark:text-white">{wechatAccountId}</span> 的绑定关系吗？
            <br />
            <span className="text-red-600 dark:text-red-400 text-sm">此操作不可撤销</span>
          </p>
        </div>
        
        <div className="flex justify-end space-x-4">
          <Button
            variant="outline"
            onClick={onCancel}
            disabled={loading}
          >
            取消
          </Button>
          <Button
            onClick={onConfirm}
            disabled={loading}
            className="bg-red-600 hover:bg-red-700"
          >
            {loading ? '删除中...' : '删除'}
          </Button>
        </div>
      </div>
    </Modal>
  );
}