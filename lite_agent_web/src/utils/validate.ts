export const validateMaxLength = (maxLength: number, message: string) => {
  return (_: any, value: string) => {
    if (value && value.length > maxLength) {
      return Promise.reject(new Error(message));
    }
    return Promise.resolve();
  };
};