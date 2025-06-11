export function validateApiKey(apiKey: string): boolean {
  return apiKey?.startsWith('sk-');
}