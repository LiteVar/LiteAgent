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
      title="添加成员"
      open={visible}
      onCancel={onCancel}
      footer={[
        <Button key="cancel" onClick={onCancel}>
          取消
        </Button>,
        <Button key="submit" type="primary" onClick={handleSubmit}>
          添加
        </Button>,
      ]}
    >
      <div className="mt-8 mb-2">
        <Select
          className="w-full"
          value={role}
          onChange={handleRoleChange}
        >
          <Option value={UserType.Normal}>权限：普通成员</Option>
          <Option value={UserType.Developer}>权限：开发者</Option>
        </Select>
        <div className="mt-3">
          <Input
            value={inputValue}
            onChange={handleInputChange}
            onBlur={handleInputConfirm}
            onPressEnter={(e) => {
              e.preventDefault();
              handleInputConfirm();
            }}
            placeholder="输入成员的邮箱账号进行添加，按 Enter 键确认"
          />
        </div>
        {inputError && <div style={{ color: 'red', marginTop: 4 }}>{inputError}</div>}
      </div>
      <div style={{ marginBottom: 16 }}>
        {emails.map(email => (
          <Tag key={email} closable onClose={() => handleRemove(email)} style={{ marginBottom: 8 }}>
            {email}
          </Tag>
        ))}
      </div>
      <div style={{ marginTop: 16, color: 'rgba(0,0,0,0.45)', fontSize: 14 }}>
        如果成员的邮箱账号未注册，系统将发送激活邮件到该成员邮箱
      </div>
    </Modal>
  );
};

export default AddMemberModal;
