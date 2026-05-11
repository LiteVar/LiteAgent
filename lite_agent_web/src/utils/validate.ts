export const validateMaxLength = (maxLength: number, message: string) => {
  return (_: any, value: string) => {
    if (value && value.length > maxLength) {
      return Promise.reject(new Error(message));
    }
    return Promise.resolve();
  };
};

export function maskPhone(phone: string) {
  if (!phone) return "";
  return phone.replace(/^(\d{3})\d{4}(\d{4})$/, "$1****$2");
}