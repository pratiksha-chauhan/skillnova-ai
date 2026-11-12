"use client";

import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from "react";

import { api } from "@/lib/api";
import { getToken, setToken } from "@/lib/auth-token";
import type {
    AuthUser,
    LoginRequest,
    LoginResponse,
    MeResponse,
    RegisterRequest,
    RegisterResponse,
} from "@/types/auth";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
    user: AuthUser | null;
    status: AuthStatus;
    login: (request: LoginRequest) => Promise<void>;
    register: (request: RegisterRequest) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/**
 * Holds authenticated-user state for the whole app and exposes
 * login/register/logout. On mount, if a token was persisted in
 * localStorage from a previous session, it's validated against the real
 * backend (`GET /api/auth/me`) rather than trusted blindly - an
 * expired/invalid token is cleared and the user is treated as logged out.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<AuthUser | null>(null);
    const [status, setStatus] = useState<AuthStatus>("loading");

    const hydrate = useCallback(async () => {
        const token = getToken();
        if (!token) {
            setStatus("unauthenticated");
            return;
        }
        try {
            const response = await api.get<MeResponse>("/api/auth/me");
            setUser(response.data);
            setStatus("authenticated");
        } catch {
            setToken(null);
            setUser(null);
            setStatus("unauthenticated");
        }
    }, []);

    useEffect(() => {
        // Inline IIFE (rather than a named function reference) to satisfy
        // eslint-plugin-react-hooks v7's react-hooks/set-state-in-effect
        // rule - see frontend conventions note carried over from Phase 12.
        void (async () => {
            await hydrate();
        })();
    }, [hydrate]);

    const login = useCallback(async (request: LoginRequest) => {
        const response = await api.post<LoginResponse>("/api/auth/login", request);
        setToken(response.data.token);
        setUser({
            id: response.data.id,
            email: response.data.email,
            fullName: response.data.fullName,
        });
        setStatus("authenticated");
    }, []);

    const register = useCallback(
        async (request: RegisterRequest) => {
            // Registration deliberately returns no token (RegisterResponse
            // has no `token` field), so immediately log in with the same
            // credentials to establish a session right after sign-up.
            await api.post<RegisterResponse>("/api/auth/register", request);
            await login({ email: request.email, password: request.password });
        },
        [login],
    );

    const logout = useCallback(() => {
        setToken(null);
        setUser(null);
        setStatus("unauthenticated");
    }, []);

    const value = useMemo(
        () => ({ user, status, login, register, logout }),
        [user, status, login, register, logout],
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
}
