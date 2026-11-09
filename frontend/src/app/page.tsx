"use client";

import { useCallback, useEffect, useState } from "react";
import { AxiosError } from "axios";

import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";

type HealthState =
  | { status: "loading" }
  | { status: "ok"; message: string }
  | { status: "error"; message: string };

/**
 * Phase 12 (Frontend Setup) verification page: confirms the Next.js +
 * Tailwind + Shadcn scaffold renders, and that the shared axios client in
 * `lib/api.ts` can reach the real backend's `GET /api/health` endpoint
 * (public, no JWT required). Will be replaced by the real landing/dashboard
 * page in later phases.
 */
export default function Home() {
  const [health, setHealth] = useState<HealthState>({ status: "loading" });

  // No synchronous setState before the await - only the async callbacks
  // update state, so this is safe to call directly from the effect below
  // without triggering cascading renders.
  const fetchHealth = useCallback(async () => {
    try {
      const response = await api.get<string>("/api/health");
      setHealth({ status: "ok", message: response.data });
    } catch (error) {
      setHealth({ status: "error", message: (error as AxiosError).message });
    }
  }, []);

  const handleRetry = useCallback(() => {
    setHealth({ status: "loading" });
    void fetchHealth();
  }, [fetchHealth]);

  useEffect(() => {
    // Inline IIFE (rather than calling fetchHealth directly) keeps the
    // react-hooks/set-state-in-effect rule happy: it can't statically prove
    // an inlined async closure doesn't set state synchronously, whereas a
    // named function reference trips its conservative analysis.
    void (async () => {
      await fetchHealth();
    })();
  }, [fetchHealth]);

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-6 p-8">
      <div className="flex flex-col items-center gap-2 text-center">
        <h1 className="text-3xl font-bold tracking-tight">SkillNova AI</h1>
        <p className="text-muted-foreground max-w-md">
          AI Powered Career Roadmap Generator and Skill Gap Analysis System -
          frontend scaffold (Phase 12).
        </p>
      </div>

      <div className="w-full max-w-sm rounded-lg border p-4 text-center">
        <p className="text-sm font-medium">Backend connectivity</p>
        <p
          className={
            health.status === "ok"
              ? "mt-1 text-sm text-green-600"
              : health.status === "error"
                ? "mt-1 text-sm text-red-600"
                : "mt-1 text-sm text-muted-foreground"
          }
        >
          {health.status === "loading" && "Checking..."}
          {health.status === "ok" && health.message}
          {health.status === "error" && `Unreachable: ${health.message}`}
        </p>
        <Button className="mt-4" onClick={handleRetry} variant="outline">
          Check again
        </Button>
      </div>
    </main>
  );
}
