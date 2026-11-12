/**
 * JWT token storage for the "in-memory + localStorage persistence" strategy
 * decided for Phase 13: the token lives in a plain module-level variable for
 * the lifetime of the tab (so it's not re-read from storage on every axios
 * request), and is mirrored to `localStorage` purely so a full page
 * refresh/reopen doesn't force a re-login. This file is intentionally
 * framework-agnostic (no React) because the axios interceptors in `api.ts`
 * need to read/write the token outside of any component tree.
 *
 * Security note: this is still readable by any script running on the page
 * (XSS exposure), same as any localStorage-based approach. An httpOnly
 * cookie would avoid that but requires backend changes (Set-Cookie + CSRF
 * handling) that are out of scope for this phase - see Phase 13 design
 * discussion.
 */

const TOKEN_STORAGE_KEY = "skillnova.auth.token";

let inMemoryToken: string | null = null;
let hydratedFromStorage = false;

function readFromStorage(): string | null {
    if (typeof window === "undefined") {
        return null;
    }
    return window.localStorage.getItem(TOKEN_STORAGE_KEY);
}

/**
 * Returns the current JWT, lazily hydrating the in-memory value from
 * `localStorage` exactly once per page load (browser only; always `null`
 * during SSR/server rendering).
 */
export function getToken(): string | null {
    if (!hydratedFromStorage) {
        inMemoryToken = readFromStorage();
        hydratedFromStorage = true;
    }
    return inMemoryToken;
}

/** Sets (or, when `null`, clears) the JWT in both memory and `localStorage`. */
export function setToken(token: string | null): void {
    inMemoryToken = token;
    hydratedFromStorage = true;
    if (typeof window === "undefined") {
        return;
    }
    if (token) {
        window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
    } else {
        window.localStorage.removeItem(TOKEN_STORAGE_KEY);
    }
}
