import axios, { type AxiosRequestConfig } from "axios";

/** Shared axios instance. JWT is attached from localStorage; 401 clears it and bounces to /login. */
export const http = axios.create({ baseURL: "" });

const TOKEN_KEY = "receituario.accessToken";

export function setAccessToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}
export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

http.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error?.response?.status === 401) {
      setAccessToken(null);
      if (location.pathname !== "/login") location.assign("/login");
    }
    return Promise.reject(error);
  },
);

/** Mutator used by the orval-generated client. */
export const apiRequest = <T>(config: AxiosRequestConfig): Promise<T> =>
  http(config).then((res) => res.data);

/** RFC-7807 problem shape returned by the backend GlobalExceptionHandler. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  errors?: Record<string, string>;
}
