export const VOICE_CONVERSION_FAIL = '语音转化失败';
export const UNSUPPORT_CONVERSION_CODE = 30017;
export const CHINESE_ASR_AUTO_SEND_LIMIT = 60;
export const ENGLISH_ASR_AUTO_SEND_LIMIT = 120;

export const getAsrTextLanguage = (text: string): 'chinese' | 'english' | 'other' => {
  const trimmedText = text.trim();

  if (/[\u4e00-\u9fff]/.test(trimmedText)) {
    return 'chinese';
  }

  if (/[A-Za-z]/.test(trimmedText)) {
    return 'english';
  }

  return 'other';
};

export const shouldAutoSendStreamAsrText = (text: string) => {
  const trimmedText = text.trim();
  const language = getAsrTextLanguage(trimmedText);

  if (language === 'chinese') {
    const chineseCharacterCount = (trimmedText.match(/[\u4e00-\u9fff]/g) || []).length;
    return chineseCharacterCount <= CHINESE_ASR_AUTO_SEND_LIMIT;
  }

  if (language === 'english') {
    return trimmedText.length <= ENGLISH_ASR_AUTO_SEND_LIMIT;
  }

  return true;
};

