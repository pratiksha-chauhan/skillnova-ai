import axios from "axios";

/**
 * Base URL of the SkillNova AI backend API. Must be prefixed with
 * NEXT_PUBLIC_ to be readable in the browser (Next.js only inlines
 * NEXT_PUBLIC_* env vars into client bundles). Falls back to the local
 * backend default (see backend/application-dev.properties) so `npm run dev`
 * works out of the box without an .env.local file.
 */
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

/**
 * Shared axios instance for all backend calls. JWT attachment (Phase 13),
 * 401-refresh/redirect handling, etc. will be added here as interceptors in
 * later phases - keeping the instance centralized now means every feature
 * module picks up those behaviors automatically once added.
 */
export const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        "Content-Type": "application/json",
    },
});
