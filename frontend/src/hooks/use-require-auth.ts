"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { useAuth } from "@/lib/auth-context";

/**
 * Client-side route guard: redirects to /login once hydration has
 * determined the user is unauthenticated. Returns the current auth status
 * so the calling page can render a loading state until the redirect (or
 * authenticated render) happens - avoids a flash of protected content.
 */
export function useRequireAuth() {
    const { status } = useAuth();
    const router = useRouter();

    useEffect(() => {
        if (status === "unauthenticated") {
            router.replace("/login");
        }
    }, [status, router]);

    return status;
}
