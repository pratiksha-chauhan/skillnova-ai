import axios, { AxiosError } from "axios";

import { getToken, setToken } from "@/lib/auth-token";

/**
 * Base URL of the SkillNova AI backend API. Must be prefixed with
 * NEXT_PUBLIC_ to be readable in the browser (Next.js only inlines
 * NEXT_PUBLIC_* env vars into client bundles). Falls back to the local
 * backend default (see backend/application-dev.properties) so `npm run dev`
 * works out of the box without an .env.local file.
 */
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

/** Endpoints that never need (and shouldn't send) a stale/missing Bearer token. */
const PUBLIC_PATHS = ["/api/auth/login", "/api/auth/register", "/api/health"];

/**
 * Shared axios instance for all backend calls. A request interceptor
 * attaches the JWT (if present) to every non-public call, and a response
 * interceptor reacts to 401s by clearing the stored token and redirecting to
 * /login - centralizing both here means every feature module picks up this
 * behavior automatically.
 */
export const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        "Content-Type": "application/json",
    },
});

api.interceptors.request.use((config) => {
    const isPublicPath = PUBLIC_PATHS.some((path) => config.url?.startsWith(path));
    if (!isPublicPath) {
        const token = getToken();
        if (token) {
            config.headers.set("Authorization", `Bearer ${token}`);
        }
    }
    return config;
});

api.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
        if (error.response?.status === 401) {
            setToken(null);
            if (typeof window !== "undefined" && window.location.pathname !== "/login") {
                // Intentional full page reload (not SPA navigation): this
                // runs inside an axios interceptor, outside any component
                // tree, so `useRouter()` isn't available here. A hard
                // redirect also guarantees all in-memory React state
                // (AuthContext, component state, etc.) is wiped on an
                // auth failure, which is the behavior we want on 401.
                // eslint-disable-next-line @next/next/no-location-assign-relative-destination
                window.location.href = `${window.location.origin}/login`;
            }
        }
        return Promise.reject(error);
    },
);
