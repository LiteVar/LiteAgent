import {API_URL} from "@/config/common";

export const buildImageUrl = (name: string) => {
  if (!name) return
  if (name.startsWith('http')) return name
  return `${API_URL}/liteAgent/v1/file/download?filename=${name}`
}
