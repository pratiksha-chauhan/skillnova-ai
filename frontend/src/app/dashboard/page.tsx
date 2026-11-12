"use client";

import { useRequireAuth } from "@/hooks/use-require-auth";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

/**
 * Minimal Phase 13 placeholder: proves the protected-route guard and
 * JWT-attached-request flow work end-to-end (user identity was already
 * fetched via GET /api/auth/me during auth hydration - see
 * lib/auth-context.tsx). The real dashboard UI arrives in Phase 14.
 */
export default function DashboardPage() {
    const status = useRequireAuth();
    const { user, logout } = useAuth();

    if (status !== "authenticated" || !user) {
        return (
            <main className="flex min-h-screen items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Loading...</p>
            </main>
        );
    }

    return (
        <main className="flex min-h-screen flex-col items-center justify-center gap-6 p-8">
            <Card className="w-full max-w-sm">
                <CardHeader>
                    <CardTitle>Welcome, {user.fullName}</CardTitle>
                </CardHeader>
                <CardContent className="flex flex-col gap-4">
                    <p className="text-sm text-muted-foreground">{user.email}</p>
                    <p className="text-sm text-muted-foreground">
                        This is a Phase 13 placeholder dashboard confirming your JWT session is
                        active. The full dashboard UI arrives in Phase 14.
                    </p>
                    <Button variant="outline" onClick={logout}>
                        Log out
                    </Button>
                </CardContent>
            </Card>
        </main>
    );
}
