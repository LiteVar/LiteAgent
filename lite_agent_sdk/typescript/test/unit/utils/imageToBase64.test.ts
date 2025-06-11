// test/unit/utils/imageToBase64.test.ts

import { imageToBase64 } from '../../../src/utils/base64';

describe('imageToBase64', () => {
  let originalFileReader: typeof FileReader;

  beforeEach(() => {
    originalFileReader = global.FileReader;
  });

  afterEach(() => {
    global.FileReader = originalFileReader;
    jest.restoreAllMocks();
  });

  it('should convert a Blob to a base64 string', async () => {
    const fakeBase64 = 'data:image/png;base64,fakeBase64Data';
    const blob = new Blob(['hello world'], { type: 'text/plain' });

    // 模拟 FileReader
    class MockFileReader {
      result: string | null = null;
      onload: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null;
      onerror: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null;

      readAsDataURL() {
        this.result = fakeBase64;
        // 模拟异步调用
        setTimeout(() => {
          if (this.onload) this.onload.call(this as any, {} as any);
        }, 10);
      }
    }

    global.FileReader = MockFileReader as any;

    const result = await imageToBase64(blob);
    expect(result).toBe('fakeBase64Data');
  });

  it('should reject if reader.result is null', async () => {
    const blob = new Blob(['hello'], { type: 'text/plain' });

    class MockFileReader {
      result: string | null = null;
      onload: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null;

      readAsDataURL() {
        setTimeout(() => {
          if (this.onload) this.onload.call(this as any, {} as any);
        }, 10);
      }
    }

    global.FileReader = MockFileReader as any;

    await expect(imageToBase64(blob)).rejects.toThrow('Failed to read file');
  });

  it('should reject on FileReader error', async () => {
    const blob = new Blob(['hello'], { type: 'text/plain' });
    const mockError = new Error('read failed');

    class MockFileReader {
      onerror: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null;

      readAsDataURL() {
        setTimeout(() => {
          if (this.onerror) this.onerror.call(this as any, mockError as any);
        }, 10);
      }
    }

    global.FileReader = MockFileReader as any;

    await expect(imageToBase64(blob)).rejects.toThrow('read failed');
  });
});
