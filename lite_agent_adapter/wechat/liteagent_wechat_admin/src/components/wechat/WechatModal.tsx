import { useState, useEffect } from "react";
import { Modal } from "../ui/modal";
import Button from "../ui/button/Button";
import Form from "../form/Form";
import Label from "../form/Label";
import InputField from "../form/input/InputField";
import { AgentWxRefDto } from "../../api/types.gen";

interface WechatModalProps {
  isOpen: boolean;
  wechatAccount: AgentWxRefDto | null;
  onSave: (data: AgentWxRefDto) => void;
  onCancel: () => void;
  loading: boolean;
}

export default function WechatModal({ 
  isOpen,
  wechatAccount, 
  onSave, 
  onCancel, 
  loading 
}: WechatModalProps) {
  const [formData, setFormData] = useState<AgentWxRefDto>({
    name: '',
    agentApiKey: '',
    agentBaseUrl: '',
    appId: '',
    appSecret: '',
    token: '',
    aesKey: '',
  });
  
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [copyStatus, setCopyStatus] = useState<Record<string, string>>({});

  // 生成随机字符串
  const generateRandomString = (length: number): string => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
  };

  // 复制到剪贴板
  const copyToClipboard = async (text: string, field: string) => {
    try {
      if (navigator.clipboard && window.isSecureContext) {
        // 优先使用现代 API
        await navigator.clipboard.writeText(text);
      } else {
        // 兼容性处理：创建临时 textarea
        const textArea = document.createElement("textarea");
        textArea.value = text;
        textArea.style.position = "fixed";  // 避免页面滚动
        textArea.style.opacity = "0";       // 隐藏
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        if (!document.execCommand("copy")) {
          throw new Error("execCommand 复制失败");
        }
        document.body.removeChild(textArea);
      }
  
      // 成功提示
      setCopyStatus(prev => ({ ...prev, [field]: "已复制" }));
      setTimeout(() => {
        setCopyStatus(prev => ({ ...prev, [field]: "" }));
      }, 2000);
  
    } catch (err) {
      // 失败提示
      setCopyStatus(prev => ({ ...prev, [field]: "复制失败" }));
      setTimeout(() => {
        setCopyStatus(prev => ({ ...prev, [field]: "" }));
      }, 2000);
    }
  };
  

  // 生成并复制Token
  const handleGenerateToken = () => {
    const newToken = generateRandomString(32);
    handleInputChange('token', newToken);
    copyToClipboard(newToken, 'token');
  };

  // 生成并复制密钥
  const handleGenerateAesKey = () => {
    const newAesKey = generateRandomString(43);
    handleInputChange('aesKey', newAesKey);
    copyToClipboard(newAesKey, 'aesKey');
  };

  // 监听wechatAccount参数变化，实现表单数据预填写
  useEffect(() => {
    if (wechatAccount) {
      setFormData({
        id: wechatAccount.id,
        name: wechatAccount.name || '',
        agentApiKey: wechatAccount.agentApiKey || '',
        agentBaseUrl: wechatAccount.agentBaseUrl || '',
        appId: wechatAccount.appId || '',
        appSecret: wechatAccount.appSecret || '',
        token: wechatAccount.token || '',
        aesKey: wechatAccount.aesKey || '',
      });
    } else {
      setFormData({
        name: '',
        agentApiKey: '',
        agentBaseUrl: '',
        appId: '',
        appSecret: '',
        token: '',
        aesKey: '',
      });
    }
    // 切换模式时清空错误信息
    setErrors({});
  }, [wechatAccount]);

  // 判断是否为编辑模式
  const isEditMode = !!wechatAccount;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: Record<string, string> = {};
    if (!formData.name.trim()) newErrors.name = '公众号名称不能为空';
    if (!formData.agentApiKey.trim()) newErrors.agentApiKey = 'Agent API Key不能为空';
    if (!formData.agentBaseUrl.trim()) newErrors.agentBaseUrl = 'Agent Base URL不能为空';
    if (!formData.appId.trim()) newErrors.appId = '公众号AppID不能为空';
    if (!formData.appSecret.trim()) newErrors.appSecret = '公众号AppSecret不能为空';
    if (!formData.token.trim()) newErrors.token = '公众号Token不能为空';
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }
    
    // 根据编辑模式决定是否包含id字段
    const submitData = { ...formData };
    if (!isEditMode) {
      // 新增模式：移除id字段，让后端生成
      delete submitData.id;
    }
    
    onSave(submitData);
  };

  const handleInputChange = (field: keyof AgentWxRefDto, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  return (
    <Modal 
      isOpen={isOpen} 
      onClose={onCancel}
      showCloseButton={false}
      className="w-full max-w-2xl max-h-[90vh] overflow-y-auto"
    >
      <div className="p-6 border-b border-stroke dark:border-strokedark">
        <h3 className="text-lg font-semibold text-black dark:text-white">
          {wechatAccount ? '编辑公众号' : '添加公众号'}
        </h3>
      </div>
      
      <Form onSubmit={handleSubmit} className="p-6 space-y-4">
        <div>
          <Label htmlFor="name">
          公众号名称 *
          </Label>
          <InputField
            id="name"
            type="text"
            value={formData.name}
            onChange={(e) => handleInputChange('name', e.target.value)}
            placeholder="输入公众号名称"
            error={!!errors.name}
            hint={errors.name}
          />
        </div>
        
        <div>
          <Label htmlFor="agentApiKey">
            Agent API Key *
          </Label>
          <InputField
            id="agentApiKey"
            type="text"
            value={formData.agentApiKey}
            onChange={(e) => handleInputChange('agentApiKey', e.target.value)}
            placeholder="输入Agent API Key"
            error={!!errors.agentApiKey}
            hint={errors.agentApiKey}
          />
        </div>
        
        <div>
          <Label htmlFor="agentBaseUrl">
            Agent Base URL *
          </Label>
          <InputField
            id="agentBaseUrl"
            type="url"
            value={formData.agentBaseUrl}
            onChange={(e) => handleInputChange('agentBaseUrl', e.target.value)}
            placeholder="https://example.com"
            error={!!errors.agentBaseUrl}
            hint={errors.agentBaseUrl}
          />
        </div>
        
        <div>
          <Label htmlFor="appId">
            公众号AppID *
          </Label>
          <InputField
            id="appId"
            type="text"
            value={formData.appId}
            onChange={(e) => handleInputChange('appId', e.target.value)}
            placeholder="输入公众号AppID"
            error={!!errors.appId}
            hint={errors.appId}
          />
        </div>
        
        <div>
          <Label htmlFor="appSecret">
            公众号AppSecret *
          </Label>
          <InputField
            id="appSecret"
            type="password"
            value={formData.appSecret}
            onChange={(e) => handleInputChange('appSecret', e.target.value)}
            placeholder="输入公众号AppSecret"
            error={!!errors.appSecret}
            hint={errors.appSecret}
          />
        </div>
        
        <div>
          <Label htmlFor="token">
            公众号Token *
          </Label>
          <div className="relative">
            <InputField
              id="token"
              type="text"
              value={formData.token}
              onChange={(e) => handleInputChange('token', e.target.value)}
              placeholder="输入公众号Token"
              error={!!errors.token}
              hint={errors.token}
              className="pr-24"
            />
            <div className="absolute right-2 top-1/2 -translate-y-1/2 flex gap-1">
              <button
                type="button"
                onClick={handleGenerateToken}
                className="px-2 py-1 text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 hover:bg-blue-200 dark:hover:bg-blue-900/50 rounded border border-blue-200 dark:border-blue-700 transition-colors"
                disabled={loading}
              >
                {copyStatus.token || '生成'}
              </button>
              {formData.token && (
                <button
                  type="button"
                  onClick={() => copyToClipboard(formData.token, 'token')}
                  className="px-2 py-1 text-xs bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 hover:bg-green-200 dark:hover:bg-green-900/50 rounded border border-green-200 dark:border-green-700 transition-colors"
                  disabled={loading}
                >
                  复制
                </button>
              )}
            </div>
          </div>
          <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
            点击生成并复制，将 Token 填入公众号对应配置项（32 个随机字符）
          </p>
        </div>
        
        <div>
          <Label htmlFor="aesKey">
            消息加密密钥
          </Label>
          <div className="relative">
            <InputField
              id="aesKey"
              type="password"
              value={formData.aesKey}
              onChange={(e) => handleInputChange('aesKey', e.target.value)}
              placeholder="输入消息加密密钥"
              error={!!errors.aesKey}
              hint={errors.aesKey}
              className="pr-24"
            />
            <div className="absolute right-2 top-1/2 -translate-y-1/2 flex gap-1">
              <button
                type="button"
                onClick={handleGenerateAesKey}
                className="px-2 py-1 text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 hover:bg-blue-200 dark:hover:bg-blue-900/50 rounded border border-blue-200 dark:border-blue-700 transition-colors"
                disabled={loading}
              >
                {copyStatus.aesKey || '生成'}
              </button>
              {formData.aesKey && (
                <button
                  type="button"
                  onClick={() => copyToClipboard(formData.aesKey, 'aesKey')}
                  className="px-2 py-1 text-xs bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 hover:bg-green-200 dark:hover:bg-green-900/50 rounded border border-green-200 dark:border-green-700 transition-colors"
                  disabled={loading}
                >
                  复制
                </button>
              )}
            </div>
          </div>
          <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
            点击生成并复制，将密钥填入公众号对应配置项（43 个随机字符）, 消息加密方式为安全模式时必填
          </p>
        </div>
        
        <div className="space-y-4">
          <div className="p-3 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 rounded-lg">
            <p className="text-sm text-yellow-800 dark:text-yellow-200">
              💡 提示：使用 Auto Multi Agent 时，回复内容可能过长被微信截断
            </p>
          </div>
          
          <div className="flex justify-end space-x-4 pt-4">
            <Button
              variant="outline"
              onClick={onCancel}
              disabled={loading}
            >
              取消
            </Button>
            <button
              type="submit"
              disabled={loading}
              className="inline-flex items-center justify-center gap-2 rounded-lg transition px-5 py-3.5 text-sm bg-brand-500 text-white shadow-theme-xs hover:bg-brand-600 disabled:bg-brand-300 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? '保存中...' : '保存'}
            </button>
          </div>
        </div>
      </Form>
    </Modal>
  );
}