import React from 'react';
import { VideoCameraOutlined } from '@ant-design/icons';
import { UploadedFile } from '@/hooks/chat/useChatFileUpload';

interface UploadedFilesPreviewProps {
  uploadedFiles: UploadedFile[];
  onRemoveFile: (fileId: string) => void;
}

export const UploadedFilesPreview: React.FC<UploadedFilesPreviewProps> = ({
  uploadedFiles,
  onRemoveFile,
}) => {
  if (uploadedFiles.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-wrap gap-2 mb-3">
      {uploadedFiles.map((file) => (
        <div key={file.id} className="relative group">
          {file.type === 'image' ? (
            <div
              className={`relative bg-white rounded-xl overflow-hidden shadow-sm border border-[#E0E3E6] ${
                uploadedFiles.length === 1 ? 'w-[100px] h-[100px]' : 'w-16 h-16'
              }`}
            >
              <div className="mask absolute top-0 left-0 w-full h-full group-hover:bg-black/50 group-hover:opacity-100 transition-opacity"></div>
              <img
                src={file.url}
                alt={file.name}
                className="w-full h-full object-cover"
              />
              <button
                onClick={() => onRemoveFile(file.id)}
                className={`absolute cursor-pointer flex items-center top-1 right-1 justify-center bg-transparent rounded-full opacity-0 group-hover:opacity-100 transition-opacity p-0 m-0 border-0 ${
                  uploadedFiles.length === 1 ? 'w-6 h-6' : 'w-4 h-4'
                }`}
              >
                <svg width={uploadedFiles.length === 1 ? 20 : 16} height={uploadedFiles.length === 1 ? 20 : 16} viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M8 1.5C6.71442 1.5 5.45772 1.88122 4.3888 2.59545C3.31988 3.30968 2.48676 4.32484 1.99479 5.51256C1.50282 6.70028 1.37409 8.00721 1.6249 9.26809C1.8757 10.529 2.49477 11.6872 3.40381 12.5962C4.31285 13.5052 5.47104 14.1243 6.73192 14.3751C7.99279 14.6259 9.29973 14.4972 10.4874 14.0052C11.6752 13.5132 12.6903 12.6801 13.4046 11.6112C14.1188 10.5423 14.5 9.28558 14.5 8C14.4982 6.27665 13.8128 4.62441 12.5942 3.40582C11.3756 2.18722 9.72335 1.50182 8 1.5ZM10.3538 9.64625C10.4002 9.69271 10.4371 9.74786 10.4622 9.80855C10.4873 9.86925 10.5003 9.9343 10.5003 10C10.5003 10.0657 10.4873 10.1308 10.4622 10.1914C10.4371 10.2521 10.4002 10.3073 10.3538 10.3538C10.3073 10.4002 10.2521 10.4371 10.1915 10.4622C10.1308 10.4873 10.0657 10.5003 10 10.5003C9.93431 10.5003 9.86925 10.4873 9.80855 10.4622C9.74786 10.4371 9.69271 10.4002 9.64625 10.3538L8 8.70687L6.35375 10.3538C6.3073 10.4002 6.25215 10.4371 6.19145 10.4622C6.13075 10.4873 6.0657 10.5003 6 10.5003C5.93431 10.5003 5.86925 10.4873 5.80855 10.4622C5.74786 10.4371 5.69271 10.4002 5.64625 10.3538C5.5998 10.3073 5.56295 10.2521 5.53781 10.1914C5.51266 10.1308 5.49972 10.0657 5.49972 10C5.49972 9.9343 5.51266 9.86925 5.53781 9.80855C5.56295 9.74786 5.5998 9.69271 5.64625 9.64625L7.29313 8L5.64625 6.35375C5.55243 6.25993 5.49972 6.13268 5.49972 6C5.49972 5.86732 5.55243 5.74007 5.64625 5.64625C5.74007 5.55243 5.86732 5.49972 6 5.49972C6.13268 5.49972 6.25993 5.55243 6.35375 5.64625L8 7.29313L9.64625 5.64625C9.69271 5.59979 9.74786 5.56294 9.80855 5.5378C9.86925 5.51266 9.93431 5.49972 10 5.49972C10.0657 5.49972 10.1308 5.51266 10.1915 5.5378C10.2521 5.56294 10.3073 5.59979 10.3538 5.64625C10.4002 5.6927 10.4371 5.74786 10.4622 5.80855C10.4873 5.86925 10.5003 5.9343 10.5003 6C10.5003 6.0657 10.4873 6.13075 10.4622 6.19145C10.4371 6.25214 10.4002 6.3073 10.3538 6.35375L8.70688 8L10.3538 9.64625Z" fill="white"/>
                </svg>
              </button>
            </div>
          ) : (
            <div className="relative bg-white border border-[#E0E3E6] rounded-xl p-3 shadow-sm min-w-32 max-w-[300px]">
              <div className="mask absolute top-0 left-0 w-full h-full group-hover:bg-black/50 group-hover:opacity-100 transition-opacity rounded-xl"></div>
              <div className="flex items-center gap-3">
                <div className="bg-gradient-to-b from-[#40A5EE] to-[#1D4A6B] rounded-lg w-10 h-10 flex items-center justify-center flex-shrink-0 shadow-sm">
                  <VideoCameraOutlined className="text-white text-lg" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-[#383f44] text-sm truncate">{file.name}</div>
                  <div className="text-[#7c8b98] text-xs">文件</div>
                </div>
                <button
                  onClick={() => onRemoveFile(file.id)}
                  className="absolute cursor-pointer bg-transparent top-1 right-1 w-4 h-4 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0 rounded-full p-0 m-0 border-0"
                >
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M8 1.5C6.71442 1.5 5.45772 1.88122 4.3888 2.59545C3.31988 3.30968 2.48676 4.32484 1.99479 5.51256C1.50282 6.70028 1.37409 8.00721 1.6249 9.26809C1.8757 10.529 2.49477 11.6872 3.40381 12.5962C4.31285 13.5052 5.47104 14.1243 6.73192 14.3751C7.99279 14.6259 9.29973 14.4972 10.4874 14.0052C11.6752 13.5132 12.6903 12.6801 13.4046 11.6112C14.1188 10.5423 14.5 9.28558 14.5 8C14.4982 6.27665 13.8128 4.62441 12.5942 3.40582C11.3756 2.18722 9.72335 1.50182 8 1.5ZM10.3538 9.64625C10.4002 9.69271 10.4371 9.74786 10.4622 9.80855C10.4873 9.86925 10.5003 9.9343 10.5003 10C10.5003 10.0657 10.4873 10.1308 10.4622 10.1914C10.4371 10.2521 10.4002 10.3073 10.3538 10.3538C10.3073 10.4002 10.2521 10.4371 10.1915 10.4622C10.1308 10.4873 10.0657 10.5003 10 10.5003C9.93431 10.5003 9.86925 10.4873 9.80855 10.4622C9.74786 10.4371 9.69271 10.4002 9.64625 10.3538L8 8.70687L6.35375 10.3538C6.3073 10.4002 6.25215 10.4371 6.19145 10.4622C6.13075 10.4873 6.0657 10.5003 6 10.5003C5.93431 10.5003 5.86925 10.4873 5.80855 10.4622C5.74786 10.4371 5.69271 10.4002 5.64625 10.3538C5.5998 10.3073 5.56295 10.2521 5.53781 10.1914C5.51266 10.1308 5.49972 10.0657 5.49972 10C5.49972 9.9343 5.51266 9.86925 5.53781 9.80855C5.56295 9.74786 5.5998 9.69271 5.64625 9.64625L7.29313 8L5.64625 6.35375C5.55243 6.25993 5.49972 6.13268 5.49972 6C5.49972 5.86732 5.55243 5.74007 5.64625 5.64625C5.74007 5.55243 5.86732 5.49972 6 5.49972C6.13268 5.49972 6.25993 5.55243 6.35375 5.64625L8 7.29313L9.64625 5.64625C9.69271 5.59979 9.74786 5.56294 9.80855 5.5378C9.86925 5.51266 9.93431 5.49972 10 5.49972C10.0657 5.49972 10.1308 5.51266 10.1915 5.5378C10.2521 5.56294 10.3073 5.59979 10.3538 5.64625C10.4002 5.6927 10.4371 5.74786 10.4622 5.80855C10.4873 5.86925 10.5003 5.9343 10.5003 6C10.5003 6.0657 10.4873 6.13075 10.4622 6.19145C10.4371 6.25214 10.4002 6.3073 10.3538 6.35375L8.70688 8L10.3538 9.64625Z" fill="white"/>
                  </svg>
                </button>
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

