import { useState, useCallback, useEffect } from 'react';
import { message } from 'antd';
import { getAccessToken } from '@/utils/cache';
import compressImage from '@/utils/compressImage';
import { RcFile } from 'antd/es/upload';
import { ChatMessageItem } from './useChatSSE';
import ResponseCode from '@/constants/ResponseCode';

/**
 * 已上传的文件接口
 */
export interface UploadedFile {
  id: string;
  fileId: string;
  url: string;
  type: 'image' | 'video';
  name: string;
  thumbnail?: string;
}

/**
 * 文件上传限制常量
 */
export const FILE_LIMITS = {
  MAX_IMAGE_SIZE: 10 * 1024 * 1024, // 10MB
  MAX_VIDEO_SIZE: 100 * 1024 * 1024, // 100MB
  MAX_IMAGES: 10,
  MAX_VIDEOS: 1,
} as const;

/**
 * 文件上传 Hook
 */
export const useChatFileUpload = () => {
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [uploading, setUploading] = useState(false);

  /**
   * 上传文件到接口（使用 /v1/file/chat/upload 接口）
   */
  const uploadFile = useCallback(async (file: File, type: 'image' | 'video'): Promise<string> => {
    try {
      let fileToUpload = file;
      
      // 如果是图片，进行压缩
      if (type === 'image') {
        fileToUpload = await compressImage(file as RcFile, {
          maxWidth: 1024,
          maxHeight: 1024,
          quality: 0.8,
        });
      }

      // 使用 fetch 调用 /v1/file/chat/upload
      const formData = new FormData();
      formData.append('file', fileToUpload);

      const token = getAccessToken() || localStorage.getItem('access_token');
      const response = await fetch('/v1/file/chat/upload', {
        method: 'POST',
        body: formData,
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error('上传失败');
      }

      const result = await response.json();
      if (result.code === ResponseCode.S_OK) {
        // 返回 fileId（而不是下载 URL）
        return result.data;
      } else {
        throw new Error(result.message || '上传失败');
      }
    } catch (error: unknown) {
      console.error('上传失败:', error);
      const errorMessage = error instanceof Error ? error.message : '上传失败';
      throw new Error(errorMessage);
    }
  }, []);

  /**
   * 为视频生成缩略图
   */
  const generateVideoThumbnail = useCallback((videoUrl: string): Promise<string> => {
    return new Promise<string>((resolve) => {
      const video = document.createElement('video');
      video.src = videoUrl;
      video.crossOrigin = 'anonymous';
      video.preload = 'metadata';
      
      const timeout = setTimeout(() => {
        resolve('');
      }, 5000);
      
      video.onloadedmetadata = () => {
        try {
          video.currentTime = Math.min(1, video.duration / 2);
        } catch (err) {
          clearTimeout(timeout);
          resolve('');
        }
      };
      
      video.onseeked = () => {
        try {
          clearTimeout(timeout);
          const canvas = document.createElement('canvas');
          const maxSize = 200;
          let width = video.videoWidth;
          let height = video.videoHeight;
          
          if (width > 0 && height > 0) {
            // 等比例缩放
            if (width > height) {
              if (width > maxSize) {
                height = (height * maxSize) / width;
                width = maxSize;
              }
            } else {
              if (height > maxSize) {
                width = (width * maxSize) / height;
                height = maxSize;
              }
            }
            
            canvas.width = width;
            canvas.height = height;
            const ctx = canvas.getContext('2d');
            if (ctx) {
              ctx.drawImage(video, 0, 0, width, height);
              const dataUrl = canvas.toDataURL('image/jpeg', 0.8);
              resolve(dataUrl);
            } else {
              resolve('');
            }
          } else {
            resolve('');
          }
        } catch (err) {
          clearTimeout(timeout);
          resolve('');
        }
      };
      
      video.onerror = () => {
        clearTimeout(timeout);
        resolve('');
      };
    });
  }, []);

  /**
   * 验证文件数量和大小
   */
  const validateFiles = useCallback((
    files: FileList,
    type: 'image' | 'video',
    currentFiles: UploadedFile[]
  ): boolean => {
    const currentImageCount = currentFiles.filter(f => f.type === 'image').length;
    const currentVideoCount = currentFiles.filter(f => f.type === 'video').length;

    if (type === 'image') {
      if (files.length + currentImageCount > FILE_LIMITS.MAX_IMAGES) {
        message.error(
          `图片最多只能上传 ${FILE_LIMITS.MAX_IMAGES} 张`
        );
        return false;
      }
    } else if (type === 'video') {
      if (currentVideoCount >= FILE_LIMITS.MAX_VIDEOS) {
        message.error(`视频最多只能上传 ${FILE_LIMITS.MAX_VIDEOS} 个`);
        return false;
      }

      if (files.length > FILE_LIMITS.MAX_VIDEOS) {
        message.error(`视频最多只能上传 ${FILE_LIMITS.MAX_VIDEOS} 个`);
        return false;
      }
    }

    return true;
  }, []);

  /**
   * 验证单个文件
   */
  const validateSingleFile = useCallback((file: File, type: 'image' | 'video'): boolean => {
    // 验证文件类型
    if (type === 'image' && !file.type.startsWith('image/')) {
      message.error(`${file.name} 不是有效的图片文件`);
      return false;
    }
    if (type === 'video' && !file.type.startsWith('video/')) {
      message.error(`${file.name} 不是有效的视频文件`);
      return false;
    }

    // 验证文件大小
    if (type === 'image' && file.size > FILE_LIMITS.MAX_IMAGE_SIZE) {
      message.error(`${file.name} 超过大小限制，图片最大为 10MB`);
      return false;
    }
    if (type === 'video' && file.size > FILE_LIMITS.MAX_VIDEO_SIZE) {
      message.error(`${file.name} 超过大小限制，视频最大为 100MB`);
      return false;
    }

    return true;
  }, []);

  /**
   * 排序文件：图片在前，视频在后
   */
  const sortFiles = useCallback((files: UploadedFile[]): UploadedFile[] => {
    return [...files].sort((a, b) => {
      if (a.type === 'image' && b.type === 'video') return -1;
      if (a.type === 'video' && b.type === 'image') return 1;
      return 0; // 同类型保持原有顺序
    });
  }, []);

  /**
   * 判断 URL 是否属于当前站点同域（需要携带认证信息）
   */
  const isNeedAuth = useCallback((url: string): boolean => {
    try {
      const urlObj = new URL(url);
      // 与当前页面同域名
      if (urlObj.hostname === window.location.hostname) return true;
      // 与 API 域名匹配（API_URL 可能带端口，用 hostname 比较）
      const apiUrl = import.meta.env.VITE_API_BASE_URL;
      if (apiUrl) {
        try {
          const apiUrlObj = new URL(apiUrl);
          if (urlObj.hostname === apiUrlObj.hostname) return true;
        } catch { /* ignore */ }
      }
      return false;
    } catch {
      return false;
    }
  }, []);

  /**
   * 构建 fetch 请求头：对同域 / API 域名的请求自动携带 Authorization
   */
  const buildFetchHeaders = useCallback((url: string): HeadersInit | undefined => {
    if (isNeedAuth(url)) {
      const token = getAccessToken();
      if (token) {
        return { Authorization: `Bearer ${token}` };
      }
    }
    return undefined;
  }, [isNeedAuth]);

  /**
   * 检测 URL 是否为图片或视频
   */
  const detectUrlType = useCallback(async (url: string): Promise<'image' | 'video' | null> => {
    try {
      // 通过文件扩展名判断
      const urlLower = url.toLowerCase();
      // 移除查询参数和锚点
      const urlWithoutParams = urlLower.split('?')[0].split('#')[0];
      
      const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.svg', '.ico'];
      const videoExtensions = ['.mp4', '.webm', '.ogg', '.mov', '.avi', '.wmv', '.flv', '.mkv', '.m4v', '.3gp'];
      
      const isImage = imageExtensions.some(ext => urlWithoutParams.endsWith(ext));
      const isVideo = videoExtensions.some(ext => urlWithoutParams.endsWith(ext));
      
      if (isImage) return 'image';
      if (isVideo) return 'video';
      
      // 通过 URL 参数中的关键字判断（如 fileKey=multimedia/image/...）
      if (/[?&]fileKey=.*(?:image|img)/i.test(url)) return 'image';
      if (/[?&]fileKey=.*video/i.test(url)) return 'video';

      // 如果扩展名无法判断，尝试通过 HEAD 请求检测 Content-Type
      try {
        const response = await fetch(url, {
          method: 'HEAD',
          headers: buildFetchHeaders(url),
        });
        if (response.ok) {
          const contentType = response.headers.get('content-type');
          if (contentType) {
            if (contentType.startsWith('image/')) return 'image';
            if (contentType.startsWith('video/')) return 'video';
          }
        }
      } catch {
        // 如果 HEAD 请求失败（可能是 CORS 问题），返回 null
      }
      
      // 如果无法判断，返回 null
      return null;
    } catch {
      return null;
    }
  }, [buildFetchHeaders]);

  /**
   * 从 URL 下载并上传文件
   */
  const handleUrlUpload = useCallback(async (url: string, type: 'image' | 'video') => {
    try {
      setUploading(true);
      
      // 验证文件数量
      const currentImageCount = uploadedFiles.filter(f => f.type === 'image').length;
      const currentVideoCount = uploadedFiles.filter(f => f.type === 'video').length;
      
      if (type === 'image' && currentImageCount >= FILE_LIMITS.MAX_IMAGES) {
        message.error(`图片最多只能上传 ${FILE_LIMITS.MAX_IMAGES} 张`);
        return;
      }
      if (type === 'video' && currentVideoCount >= FILE_LIMITS.MAX_VIDEOS) {
        message.error(`视频最多只能上传 ${FILE_LIMITS.MAX_VIDEOS} 个`);
        return;
      }

      // 下载文件（对 API 域名自动携带 Authorization）
      const response = await fetch(url, {
        headers: buildFetchHeaders(url),
      });
      if (!response.ok) {
        throw new Error(`下载文件失败 (${response.status})`);
      }

      const blob = await response.blob();
      
      // 验证文件类型
      // 某些响应的 Content-Type 可能为 application/octet-stream 或空，
      // 此时根据已知的 type 参数跳过类型检查
      const hasValidMime = blob.type && blob.type !== 'application/octet-stream';
      if (hasValidMime && type === 'image' && !blob.type.startsWith('image/')) {
        message.error('链接不是有效的图片文件');
        return;
      }
      if (hasValidMime && type === 'video' && !blob.type.startsWith('video/')) {
        message.error('链接不是有效的视频文件');
        return;
      }

      // 验证文件大小
      if (type === 'image' && blob.size > FILE_LIMITS.MAX_IMAGE_SIZE) {
        message.error('图片超过大小限制，最大为 10MB');
        return;
      }
      if (type === 'video' && blob.size > FILE_LIMITS.MAX_VIDEO_SIZE) {
        message.error('视频超过大小限制，最大为 100MB');
        return;
      }

      // 创建 File 对象
      // 从 URL 路径中提取文件名，去除查询参数和锚点
      let fileName = (type === 'image' ? 'image.jpg' : 'video.mp4');
      try {
        const urlPath = new URL(url).pathname;
        const pathName = urlPath.split('/').pop();
        if (pathName && pathName.includes('.')) {
          fileName = decodeURIComponent(pathName);
        }
      } catch { /* 使用默认文件名 */ }
      // 如果 blob 有 MIME 类型但文件名扩展名不匹配，修正文件名
      const mimeToExt: Record<string, string> = {
        'image/jpeg': '.jpg', 'image/png': '.png', 'image/gif': '.gif',
        'image/webp': '.webp', 'image/bmp': '.bmp', 'image/svg+xml': '.svg',
        'video/mp4': '.mp4', 'video/webm': '.webm', 'video/ogg': '.ogg',
      };
      if (blob.type && mimeToExt[blob.type] && !fileName.toLowerCase().endsWith(mimeToExt[blob.type])) {
        // 文件名没有正确扩展名时，追加正确扩展名
        const baseName = fileName.replace(/\.[^.]+$/, '');
        fileName = baseName + mimeToExt[blob.type];
      }
      const file = new File([blob], fileName, { type: blob.type || (type === 'image' ? 'image/jpeg' : 'video/mp4') });

      // 使用本地资源创建预览 URL
      const localPreviewUrl = URL.createObjectURL(file);
      let thumbnail: string | undefined;
      
      if (type === 'image') {
        thumbnail = localPreviewUrl;
      } else if (type === 'video') {
        try {
          thumbnail = await generateVideoThumbnail(localPreviewUrl);
        } catch (err) {
          thumbnail = undefined;
        }
      }

      // 上传文件
      const fileId = await uploadFile(file, type);

      const newFile: UploadedFile = {
        id: `${Date.now()}`,
        fileId,
        url: localPreviewUrl,
        type,
        name: fileName,
        thumbnail: thumbnail || localPreviewUrl,
      };

      // 添加新文件
      setUploadedFiles((prev) => {
        const updatedFiles = [...prev, newFile];
        return sortFiles(updatedFiles);
      });

      message.success('成功上传文件');
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : '上传失败';
      message.error(errorMessage);
    } finally {
      setUploading(false);
    }
  }, [uploadedFiles, uploadFile, generateVideoThumbnail, sortFiles, buildFetchHeaders]);

  /**
   * 处理文件选择
   */
  const handleFileSelect = useCallback(async (
    e: React.ChangeEvent<HTMLInputElement>,
    type: 'image' | 'video'
  ) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    // 验证文件数量
    if (!validateFiles(files, type, uploadedFiles)) {
      if (e.target) {
        e.target.value = '';
      }
      return;
    }

    setUploading(true);
    const newFiles: UploadedFile[] = [];

    try {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        
        // 验证单个文件
        if (!validateSingleFile(file, type)) {
          continue;
        }

        // 使用本地资源创建预览 URL
        const localPreviewUrl = URL.createObjectURL(file);
        let thumbnail: string | undefined;
        
        if (type === 'image') {
          // 图片直接使用本地预览URL
          thumbnail = localPreviewUrl;
        } else if (type === 'video') {
          // 为视频生成缩略图
          try {
            thumbnail = await generateVideoThumbnail(localPreviewUrl);
          } catch (err) {
            thumbnail = undefined;
          }
        }

        // 上传文件（上传后只保存fileId，不保存后端URL）
        const fileId = await uploadFile(file, type);

        newFiles.push({
          id: `${Date.now()}-${i}`,
          fileId,
          url: localPreviewUrl, // 使用本地预览URL
          type,
          name: file.name,
          thumbnail: thumbnail || localPreviewUrl, // 视频使用缩略图，图片使用本地URL
        });
      }

      // 添加新文件时，需要保持排序：图片在前，视频在后
      setUploadedFiles((prev) => {
        const updatedFiles = [...prev, ...newFiles];
        return sortFiles(updatedFiles);
      });

      if (newFiles.length > 0) {
        message.success(`成功上传 ${newFiles.length} 个文件`);
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : '上传失败';
      message.error(errorMessage);
    } finally {
      setUploading(false);
      // 清空 input 值，以便可以重复选择同一文件
      if (e.target) {
        e.target.value = '';
      }
    }
  }, [uploadFile, uploadedFiles, validateFiles, validateSingleFile, generateVideoThumbnail, sortFiles]);

  /**
   * 删除已上传的文件（同时清理本地预览URL）
   */
  const handleRemoveFile = useCallback((id: string) => {
    setUploadedFiles((prev) => {
      const fileToRemove = prev.find((file) => file.id === id);
      if (fileToRemove) {
        // 清理本地预览URL
        URL.revokeObjectURL(fileToRemove.url);
        if (fileToRemove.thumbnail && fileToRemove.thumbnail !== fileToRemove.url) {
          // 如果缩略图是data URL，不需要清理；如果是object URL，需要清理
          if (fileToRemove.thumbnail.startsWith('blob:')) {
            URL.revokeObjectURL(fileToRemove.thumbnail);
          }
        }
      }
      return prev.filter((file) => file.id !== id);
    });
  }, []);

  /**
   * 构建文件消息数组
   */
  const buildFileMessages = useCallback((): ChatMessageItem[] => {
    const messages: ChatMessageItem[] = [];
    
    uploadedFiles.forEach((file) => {
      const messageType = file.type === 'image' ? 'imageUrl' : 'videoUrl';
      messages.push({
        type: messageType,
        message: file.fileId, // 使用 fileId
      });
    });
    
    return messages;
  }, [uploadedFiles]);

  /**
   * 清理所有文件的本地预览URL
   */
  const cleanupFileUrls = useCallback(() => {
    uploadedFiles.forEach((file) => {
      URL.revokeObjectURL(file.url);
      if (file.thumbnail && file.thumbnail !== file.url) {
        if (file.thumbnail.startsWith('blob:')) {
          URL.revokeObjectURL(file.thumbnail);
        }
      }
    });
  }, [uploadedFiles]);

  /**
   * 清空所有文件
   */
  const clearFiles = useCallback(() => {
    cleanupFileUrls();
    setUploadedFiles([]);
  }, [cleanupFileUrls]);

  /**
   * 组件卸载时清理所有本地预览URL
   */
  useEffect(() => {
    return () => {
      cleanupFileUrls();
    };
  }, [cleanupFileUrls]);

  return {
    uploadedFiles,
    uploading,
    handleFileSelect,
    handleRemoveFile,
    buildFileMessages,
    clearFiles,
    sortFiles,
    handleUrlUpload,
    detectUrlType,
  };
};

