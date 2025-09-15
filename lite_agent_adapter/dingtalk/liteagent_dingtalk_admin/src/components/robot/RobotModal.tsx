import { useState, useEffect } from "react";
import { Modal } from "../ui/modal";
import Button from "../ui/button/Button";
import Form from "../form/Form";
import Label from "../form/Label";
import InputField from "../form/input/InputField";
import { AgentRobotRefDto } from "../../api/types.gen";

interface RobotModalProps {
  isOpen: boolean;
  robot: AgentRobotRefDto | null;
  onSave: (data: AgentRobotRefDto) => void;
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
  const [formData, setFormData] = useState<AgentRobotRefDto>({
    name: '',
    agentApiKey: '',
    agentBaseUrl: '',
    robotCode: '',
    robotClientId: '',
    robotClientSecret: '',
    cardTemplateId: '',
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
        robotCode: robot.robotCode || '',
        robotClientId: robot.robotClientId || '',
        robotClientSecret: robot.robotClientSecret || '',
        cardTemplateId: robot.cardTemplateId || '',
      });
    } else {
      setFormData({
        name: '',
        agentApiKey: '',
        agentBaseUrl: '',
        robotCode: '',
        robotClientId: '',
        robotClientSecret: '',
        cardTemplateId: '',
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
    if (!formData.name.trim()) newErrors.name = '机器人名称不能为空';
    if (!formData.agentApiKey.trim()) newErrors.agentApiKey = 'Agent API Key不能为空';
    if (!formData.agentBaseUrl.trim()) newErrors.agentBaseUrl = 'Agent Base URL不能为空';
    if (!formData.robotCode.trim()) newErrors.robotCode = '机器人Code不能为空';
    if (!formData.robotClientId.trim()) newErrors.robotClientId = '机器人Client ID不能为空';
    if (!formData.robotClientSecret.trim()) newErrors.robotClientSecret = '机器人Client Secret不能为空';
    if (!formData.cardTemplateId.trim()) newErrors.cardTemplateId = '卡片模板ID不能为空';
    
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

  const handleInputChange = (field: keyof AgentRobotRefDto, value: string) => {
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
          {robot ? '编辑机器人' : '添加机器人'}
        </h3>
      </div>
      
      <Form onSubmit={handleSubmit} className="p-6 space-y-4">
        <div>
          <Label htmlFor="name">
            机器人名称 *
          </Label>
          <InputField
            id="name"
            type="text"
            value={formData.name}
            onChange={(e) => handleInputChange('name', e.target.value)}
            placeholder="输入机器人名称"
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
          <Label htmlFor="robotCode">
            机器人Code *
          </Label>
          <InputField
            id="robotCode"
            type="text"
            value={formData.robotCode}
            onChange={(e) => handleInputChange('robotCode', e.target.value)}
            placeholder="输入机器人Code"
            error={!!errors.robotCode}
            hint={errors.robotCode}
          />
        </div>
        
        <div>
          <Label htmlFor="robotClientId">
            机器人Client ID *
          </Label>
          <InputField
            id="robotClientId"
            type="text"
            value={formData.robotClientId}
            onChange={(e) => handleInputChange('robotClientId', e.target.value)}
            placeholder="输入机器人Client ID"
            error={!!errors.robotClientId}
            hint={errors.robotClientId}
          />
        </div>
        
        <div>
          <Label htmlFor="robotClientSecret">
            机器人Client Secret *
          </Label>
          <InputField
            id="robotClientSecret"
            type="password"
            value={formData.robotClientSecret}
            onChange={(e) => handleInputChange('robotClientSecret', e.target.value)}
            placeholder="输入机器人Client Secret"
            error={!!errors.robotClientSecret}
            hint={errors.robotClientSecret}
          />
        </div>
        
        <div>
          <Label htmlFor="cardTemplateId">
            卡片模板ID *
          </Label>
          <InputField
            id="cardTemplateId"
            type="text"
            value={formData.cardTemplateId}
            onChange={(e) => handleInputChange('cardTemplateId', e.target.value)}
            placeholder="输入卡片模板ID"
            error={!!errors.cardTemplateId}
            hint={errors.cardTemplateId}
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