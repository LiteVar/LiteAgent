import {ACCESS_TOKEN} from "@/config/constants";

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN)
}

export function setAccessToken(accessToken: string) {
  localStorage.setItem(ACCESS_TOKEN, accessToken)
}

export function removeAccessToken() {
  localStorage.removeItem(ACCESS_TOKEN)
}
