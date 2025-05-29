import {ACCESS_TOKEN} from "@/constants/routes";

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN)
}

export function setAccessToken(accessToken: string) {
  localStorage.setItem(ACCESS_TOKEN, accessToken)
}

export function removeAccessToken() {
  localStorage.removeItem(ACCESS_TOKEN)
}
