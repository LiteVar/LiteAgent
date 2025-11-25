export const isJSON = (str: string): boolean => {
  if (typeof str !== "string") return false;
  try {
    const obj = JSON.parse(str);
    return typeof obj === "object" && obj !== null;
  } catch {
    return false;
  }
}