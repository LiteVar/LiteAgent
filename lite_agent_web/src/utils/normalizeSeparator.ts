export const normalizeSeparator = (input: string) => {
  try {
    return JSON.parse(`"${input}"`);
  } catch {
    return input;
  }
}
