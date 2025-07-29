import { RcFile } from "antd/es/upload";

interface CompressOptions {
  maxWidth?: number;
  maxHeight?: number;
  quality?: number;
}

const compressImage = (
  file: RcFile, 
  options: CompressOptions = {}
): Promise<File> => {
  const { 
    maxWidth = 800, 
    maxHeight = 800, 
    quality = 0.8 
  } = options;

  return new Promise((resolve) => {
    try {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onerror = () => {
        resolve(file); // Return original file on read error
      };
      reader.onload = (e) => {
        const img = new Image();
        img.src = e.target?.result as string;
        img.onerror = () => {
          resolve(file); // Return original file on image load error
        };
        img.onload = () => {
          try {
            const canvas = document.createElement('canvas');
            let width = img.width;
            let height = img.height;

            // 等比例缩放
            if (width > height && width > maxWidth) {
              height = Math.round((height * maxWidth) / width);
              width = maxWidth;
            } else if (height > maxHeight) {
              width = Math.round((width * maxHeight) / height);
              height = maxHeight;
            }

            canvas.width = width;
            canvas.height = height;
            const ctx = canvas.getContext('2d');
            if (!ctx) {
              resolve(file); // Return original file if can't get context
              return;
            }
            ctx.drawImage(img, 0, 0, width, height);
            
            canvas.toBlob(
              (blob) => {
                if (!blob) {
                  resolve(file); // Return original file if blob creation fails
                  return;
                }
                const newFile = new File([blob], file.name, {
                  type: file.type,
                  lastModified: Date.now(),
                });
                resolve(newFile);
              },
              file.type,
              quality
            );
          } catch (error) {
            resolve(file); // Return original file on any error during compression
          }
        };
      };
    } catch (error) {
      resolve(file); // Return original file on any outer error
    }
  });
};

export default compressImage;