import { MAX_PICTURE_SIZE } from "@/constants/common";
import { GetProp, message, Upload, UploadProps } from "antd";
import { RcFile } from "antd/es/upload";
import type { UploadRequestOption } from 'rc-upload/lib/interface';
import compressImage from "./compressImage";
import ResponseCode from "@/constants/ResponseCode";

type FileType = Parameters<GetProp<UploadProps, 'beforeUpload'>>[0];

const beforeUpload = async (file: FileType) => {
  const isLt500 = file.size < MAX_PICTURE_SIZE;
  if (!isLt500) {
    message.error('图片大小不能超过 500KB!');
    return Upload.LIST_IGNORE;
  }
};

const onUploadAction = async (file: RcFile) => {
  const compressedFile = await compressImage(file, {
    maxWidth: 1024,
    maxHeight: 1024,
    quality: 0.8,
  });
  const formData = new FormData();
  formData.append('file', compressedFile);

  const response = await fetch('/v1/file/upload', {
    method: 'POST',
    body: formData,
    headers: {
      Authorization: `Bearer ${localStorage.getItem('access_token')}`,
    },
  });
  if (!response.ok) {
    throw new Error('上传失败');
  }

  const result = await response.json();
  console.log('result', result);
  if (result.code === ResponseCode.S_OK) {
      return '/v1/file/download?filename=' + result.data;
  } else {
      message.error('上传失败');
      return '';
  }
};

// 自定义上传请求 - 用于新的 v2 接口
// 使用 customRequest 可以避免 Upload 组件发送额外的 POST 请求
const customUploadRequest = async (options: UploadRequestOption) => {
  const { file, onSuccess, onError } = options;
  
  try {
    const compressedFile = await compressImage(file as RcFile, {
      maxWidth: 1024,
      maxHeight: 1024,
      quality: 0.8,
    });
    
    const formData = new FormData();
    formData.append('file', compressedFile);

    const response = await fetch('/v2/file/upload', {
      method: 'POST',
      body: formData,
      headers: {
        Authorization: `Bearer ${localStorage.getItem('access_token')}`,
      },
    });
    
    if (!response.ok) {
      throw new Error('上传失败');
    }

    const result = await response.json();
    
    if (result.code === ResponseCode.S_OK) {
      // 直接返回接口返回的图片 URL
      onSuccess?.(result.data);
      message.success('上传成功');
    } else {
      const errorMsg = result.message || '上传失败';
      message.error(errorMsg);
      onError?.(new Error(errorMsg));
    }
  } catch (error) {
    console.error('上传失败', error);
    message.error('上传失败');
    onError?.(error as Error);
  }
};

export {
  beforeUpload,
  onUploadAction,
  customUploadRequest
}