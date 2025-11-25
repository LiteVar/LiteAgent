import { MAX_PICTURE_SIZE } from "@/constants/common";
import { GetProp, message, Upload, UploadProps } from "antd";
import { RcFile } from "antd/es/upload";
import compressImage from "./compressImage";
import { postV1FileUpload } from "@/client";
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

export {
  beforeUpload,
  onUploadAction
}