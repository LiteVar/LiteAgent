import React, { useRef, useImperativeHandle, forwardRef } from 'react';
import { Button, message, Space, Spin } from 'antd';
import { CopyOutlined, FormatPainterOutlined } from '@ant-design/icons';
import Editor, { OnMount } from '@monaco-editor/react';

interface JsonEditorProps {
  value: string;
  onChange?: (value: string) => void;
  readOnly?: boolean;
  height?: string | number;
  hideToolbar?: boolean;
}

export interface JsonEditorRef {
  format: () => void;
  copy: () => void;
  getLineCount: () => number;
}

const JsonEditor = forwardRef<JsonEditorRef, JsonEditorProps>(({ value, onChange, readOnly = false, height = '400px', hideToolbar = false }, ref) => {
  const editorRef = useRef<any>(null);

  const handleEditorDidMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;
    
    // Configure JSON validation options
    monaco.languages.json.jsonDefaults.setDiagnosticsOptions({
      validate: true,
      allowComments: false,
      schemas: [],
      enableSchemaRequest: false,
    });
  };

  useImperativeHandle(ref, () => ({
    format: () => {
      if (editorRef.current) {
        editorRef.current.getAction('editor.action.formatDocument').run();
        message.success('格式化成功');
      }
    },
    copy: () => {
      if (editorRef.current) {
        const val = editorRef.current.getValue();
        navigator.clipboard.writeText(val).then(() => {
          message.success('复制成功');
        });
      }
    },
    getLineCount: () => {
      return editorRef.current ? editorRef.current.getModel()?.getLineCount() || 0 : 0;
    }
  }));

  const handleFormat = () => {
    if (editorRef.current) {
      editorRef.current.getAction('editor.action.formatDocument').run();
      message.success('格式化成功');
    }
  };

  const handleCopy = () => {
    if (editorRef.current) {
      const value = editorRef.current.getValue();
      navigator.clipboard.writeText(value).then(() => {
        message.success('复制成功');
      });
    }
  };

  const handleChange = (value: string | undefined) => {
    onChange?.(value || '');
  };

  return (
    <div className={`flex flex-col bg-white ${hideToolbar ? '' : 'border rounded-md overflow-hidden'}`} style={{ height }}>
      {!hideToolbar && (
        <div className="flex justify-end p-2 border-b bg-gray-50">
          <Space>
            <Button size="small" icon={<FormatPainterOutlined />} onClick={handleFormat} disabled={readOnly}>
              格式化
            </Button>
            <Button size="small" icon={<CopyOutlined />} onClick={handleCopy}>
              复制
            </Button>
          </Space>
        </div>
      )}
      <div className="flex-1 relative">
        <Editor
          height="100%"
          defaultLanguage="json"
          value={value}
          onChange={handleChange}
          onMount={handleEditorDidMount}
          options={{
            readOnly,
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            fontSize: 14,
            tabSize: 2,
            formatOnPaste: true,
            automaticLayout: true,
            wordWrap: 'on',
          }}
          loading={<div className="flex justify-center items-center h-full"><Spin tip="加载编辑器..." /></div>}
        />
      </div>
    </div>
  );
});

JsonEditor.displayName = 'JsonEditor';

export default JsonEditor;
