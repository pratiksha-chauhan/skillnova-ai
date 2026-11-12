/**
 * Mirrors backend `com.skillnova.backend.dto.*` auth/identity DTOs. Keep in
 * sync with the backend records - these are the exact wire shapes exchanged
 * with `/api/auth/*`.
 */

export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    fullName: string;
    email: string;
    password: string;
}

/** Response body of `POST /api/auth/login`. */
export interface LoginResponse {
    id: string;
    email: string;
    fullName: string;
    token: string;
    expiresInMs: number;
}

/** Response body of `POST /api/auth/register` (no token - caller must log in separately). */
export interface RegisterResponse {
    id: string;
    email: string;
    fullName: string;
    createdAt: string;
}

/** Response body of the protected `GET /api/auth/me` identity-check endpoint. */
export interface MeResponse {
    id: string;
    email: string;
    fullName: string;
}

/** Minimal authenticated-user shape held in frontend auth state. */
export interface AuthUser {
    id: string;
    email: string;
    fullName: string;
}
