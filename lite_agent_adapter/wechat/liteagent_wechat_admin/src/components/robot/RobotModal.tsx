import { useState, useEffect } from "react";
import { Modal } from "../ui/modal";
import Button from "../ui/button/Button";
import Form from "../form/Form";
import Label from "../form/Label";
import InputField from "../form/input/InputField";
import { AgentWxRefDto } from "../../api/types.gen";

interface RobotModalProps {
  isOpen: boolean;
  robot: AgentWxRefDto | null;
  onSave: (data: AgentWxRefDto) => void;
  onCancel: () => void;
  loading: boolean;
}

export default function RobotModal({ 
  isOpen,
  robot, 
  onSave, 
  onCancel, 
  loading 
}: RobotModalProps) {
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

  // 监听robot参数变化，实现表单数据预填写
  useEffect(() => {
    if (robot) {
      setFormData({
        id: robot.id,
        name: robot.name || '',
        agentApiKey: robot.agentApiKey || '',
        agentBaseUrl: robot.agentBaseUrl || '',
        appId: robot.appId || '',
        appSecret: robot.appSecret || '',
        token: robot.token || '',
        aesKey: robot.aesKey || '',
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
  }, [robot]);

  // 判断是否为编辑模式
  const isEditMode = !!robot;

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
          {robot ? '编辑公众号' : '添加公众号'}
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
          <InputField
            id="token"
            type="text"
            value={formData.token}
            onChange={(e) => handleInputChange('token', e.target.value)}
            placeholder="输入公众号Token"
            error={!!errors.token}
            hint={errors.token}
          />
        </div>
        
        <div>
          <Label htmlFor="aesKey">
            消息加密密钥
          </Label>
          <InputField
            id="aesKey"
            type="password"
            value={formData.aesKey}
            onChange={(e) => handleInputChange('aesKey', e.target.value)}
            placeholder="输入消息加密密钥"
            error={!!errors.aesKey}
            hint={errors.aesKey}
          />
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
      </Form>
    </Modal>
  );
}