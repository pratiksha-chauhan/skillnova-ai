"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { useAuth } from "@/lib/auth-context";

/**
 * Root route: not a landing page in its own right, just an auth-state
 * dispatcher - redirects to /dashboard once authenticated, or /login
 * otherwise. Replaces the Phase 12 backend-health-check demo page.
 */
export default function Home() {
    const { status } = useAuth();
    const router = useRouter();

    useEffect(() => {
        if (status === "authenticated") {
            router.replace("/dashboard");
        } else if (status === "unauthenticated") {
            router.replace("/login");
        }
    }, [status, router]);

    return (
        <main className="flex min-h-screen items-center justify-center p-8">
            <p className="text-sm text-muted-foreground">Loading...</p>
        </main>
    );
}
