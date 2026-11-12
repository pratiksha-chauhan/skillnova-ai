"use client";

import Link from "next/link";

import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";

/**
 * Shared header for authenticated pages (Dashboard, Roadmap, ...): app
 * branding, current user's name/email, and a logout button. Intentionally
 * minimal (no sidebar/nav yet) - introduced in Phase 14 so Phase 15+ pages
 * don't each reinvent this chrome.
 */
export function AppShell({ children }: { children: React.ReactNode }) {
    const { user, logout } = useAuth();

    return (
        <div className="flex min-h-screen flex-col">
            <header className="flex items-center justify-between border-b px-6 py-3">
                <Link href="/dashboard" className="text-lg font-bold tracking-tight">
                    SkillNova AI
                </Link>
                {user && (
                    <div className="flex items-center gap-4">
                        <div className="text-right text-sm leading-tight">
                            <p className="font-medium">{user.fullName}</p>
                            <p className="text-muted-foreground">{user.email}</p>
                        </div>
                        <Button variant="outline" size="sm" onClick={logout}>
                            Log out
                        </Button>
                    </div>
                )}
            </header>
            <main className="flex-1 p-6">{children}</main>
        </div>
    );
}
