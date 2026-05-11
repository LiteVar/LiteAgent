import React, {useCallback, useState} from 'react';
import { Modal, Input, Select, Button, Tag, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import {UserType} from "@/types/User";

const { TextArea } = Input;
const { Option } = Select;

interface AddMemberModalProps {
  visible: boolean;
  onCancel: () => void;
  onOk: (emails: string[], role: UserType) => void;
}

const AddMemberModal: React.FC<AddMemberModalProps> = ({ visible, onCancel, onOk }) => {
  const [emails, setEmails] = useState<string[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [role, setRole] = useState(UserType.Normal);
  const [inputError, setInputError] = useState('');

  const validateEmail = (email: string): boolean => {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return emailRegex.test(email);
  };

  const handleInputChange = (e: any) => {
    setInputValue(e.target.value);
    setInputError('');
  };

  const handleInputConfirm = () => {
    const trimmedEmails = inputValue.split(/[\s,]+/).filter(email => email.trim() !== '');

    const newEmails: string[] = [];
    const invalidEmails: string[] = [];

    trimmedEmails.forEach(email => {
      if (validateEmail(email) && !emails.includes(email)) {
        newEmails.push(email);
      } else if (!validateEmail(email)) {
        invalidEmails.push(email);
      }
    });

    if (invalidEmails.length > 0) {
      setInputError(`以下邮箱格式无效: ${invalidEmails.join(', ')}`);
    } else {
      setInputError('');
    }

    if (newEmails.length > 0) {
      setEmails([...emails, ...newEmails]);
      setInputValue('');
    }
  };

  const handleRemove = (removedEmail: string) => {
    const newEmails = emails.filter(email => email !== removedEmail);
    setEmails(newEmails);
  };

  const handleRoleChange = (value: UserType) => {
    setRole(value);
  };

  const handleSubmit = useCallback(() => {
    if (emails.length === 0) {
      message.error('请输入至少一个有效的邮箱地址');
      return;
    }
    onOk(emails, role);
    setEmails([]);
    setInputValue('');
    setRole(UserType.Normal);
    setInputError('');
  }, [emails, role, onOk]);

  return (
    <Modal
      centered
      title={<span className="text-[#1D4A6B] font-medium text-lg">添加成员</span>}
      open={visible}
      onCancel={onCancel}
      footer={[
        <div key="footer" className="flex justify-end gap-2">
          <Button onClick={onCancel} className="rounded-xl h-[40px] px-6">
            取消
          </Button>
          <Button type="primary" onClick={handleSubmit} className="rounded-xl h-[40px] px-6 bg-[#40A5EE]">
            添加
          </Button>
        </div>
      ]}
      styles={{
        header: { padding: '16px 24px', borderBottom: '1px solid #F2F3F5', marginBottom: 0 },
        body: { padding: '24px' },
        footer: { padding: '10px 16px', borderTop: '1px solid #F2F3F5', marginTop: 0 },
      }}
      width={480}
    >
      <div className="space-y-4">
        <div>
          <div className="text-[#383F44] font-medium mb-2">选择权限</div>
          <Select
            className="w-full rounded-lg [&_.ant-select-selector]:h-10 [&_.ant-select-selection-item]:flex [&_.ant-select-selection-item]:items-center"
            value={role}
            onChange={handleRoleChange}
          >
            <Option value={UserType.Normal}>权限：普通成员</Option>
            <Option value={UserType.Developer}>权限：开发者</Option>
          </Select>
        </div>
        
        <div>
          <div className="text-[#383F44] font-medium mb-2">邮箱账号</div>
          <Input
            value={inputValue}
            onChange={handleInputChange}
            onBlur={handleInputConfirm}
            onPressEnter={(e) => {
              e.preventDefault();
              handleInputConfirm();
            }}
            placeholder="输入成员的邮箱账号进行添加，按 Enter 键确认"
            className="rounded-lg h-10 border-[#E0E3E6]"
          />
          {inputError && <div className="text-[#CC2D3A] text-xs mt-1">{inputError}</div>}
        </div>

        <div className="flex flex-wrap gap-2 min-h-[32px]">
          {emails.map(email => (
            <Tag 
              key={email} 
              closable 
              onClose={() => handleRemove(email)}
              className="bg-[#F2F3F5] border-none rounded-lg px-3 py-1 text-[#383F44] flex items-center gap-1 m-0"
            >
              {email}
            </Tag>
          ))}
        </div>

        <div className="text-[#7C8B98] text-sm leading-relaxed bg-[#F8FAFC] p-3 rounded-xl border border-[#F1F5F9]">
          如果成员的邮箱账号未注册，系统将发送激活邮件到该成员邮箱
        </div>
      </div>
    </Modal>
  );
};

export default AddMemberModal;
