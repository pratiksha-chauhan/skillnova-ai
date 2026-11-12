"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { AxiosError } from "axios";

import { useRequireAuth } from "@/hooks/use-require-auth";
import { api } from "@/lib/api";
import { getErrorMessage } from "@/lib/api-error";
import { AppShell } from "@/components/layout/app-shell";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import type { RoadmapResponse } from "@/types/roadmap";

type RoadmapState =
    | { status: "loading" }
    | { status: "error"; message: string }
    | { status: "ready"; data: RoadmapResponse };

/**
 * Phase 14 placeholder detail page: proves the dashboard's roadmap cards
 * link somewhere real and renders the raw persisted shape via
 * `GET /api/roadmap/{id}`. The full task-checklist / skill-gap / cert /
 * project UI is built out in Phase 15 - this is intentionally minimal.
 */
export default function RoadmapDetailPage() {
    return (
        <Suspense
            fallback={
                <main className="flex min-h-screen items-center justify-center p-8">
                    <p className="text-sm text-muted-foreground">Loading...</p>
                </main>
            }
        >
            <RoadmapDetailContent />
        </Suspense>
    );
}

function RoadmapDetailContent() {
    const authStatus = useRequireAuth();
    const params = useParams<{ id: string }>();
    const roadmapId = params.id;
    const [state, setState] = useState<RoadmapState>({ status: "loading" });

    const fetchRoadmap = useCallback(async () => {
        try {
            const response = await api.get<RoadmapResponse>(`/api/roadmap/${roadmapId}`);
            setState({ status: "ready", data: response.data });
        } catch (error) {
            setState({ status: "error", message: getErrorMessage(error as AxiosError) });
        }
    }, [roadmapId]);

    useEffect(() => {
        if (authStatus !== "authenticated") {
            return;
        }
        void (async () => {
            await fetchRoadmap();
        })();
    }, [authStatus, fetchRoadmap]);

    if (authStatus !== "authenticated") {
        return (
            <main className="flex min-h-screen items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Loading...</p>
            </main>
        );
    }

    return (
        <AppShell>
            {state.status === "loading" && (
                <p className="text-sm text-muted-foreground">Loading roadmap...</p>
            )}

            {state.status === "error" && (
                <Card className="max-w-md">
                    <CardHeader>
                        <CardTitle>Couldn&apos;t load this roadmap</CardTitle>
                        <CardDescription>{state.message}</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <Button variant="outline" onClick={() => void fetchRoadmap()}>
                            Retry
                        </Button>
                    </CardContent>
                </Card>
            )}

            {state.status === "ready" && (
                <div className="flex flex-col gap-6">
                    <div className="flex items-center justify-between">
                        <div>
                            <h1 className="text-2xl font-bold">{state.data.targetRole}</h1>
                            <p className="text-sm text-muted-foreground">{state.data.summary}</p>
                        </div>
                        <Badge>{state.data.status}</Badge>
                    </div>

                    <Card>
                        <CardHeader>
                            <CardTitle>Tasks ({state.data.tasks.length})</CardTitle>
                        </CardHeader>
                        <CardContent className="flex flex-col gap-2">
                            {state.data.tasks.map((task) => (
                                <div
                                    key={task.id}
                                    className="flex items-center justify-between border-b py-2 text-sm last:border-b-0"
                                >
                                    <span>{task.title}</span>
                                    <Badge variant="secondary">{task.status}</Badge>
                                </div>
                            ))}
                        </CardContent>
                    </Card>

                    <p className="text-xs text-muted-foreground">
                        This is a Phase 14 placeholder view. The full roadmap experience
                        (task checklist interactions, skill gaps, certifications, projects)
                        arrives in Phase 15.
                    </p>
                </div>
            )}
        </AppShell>
    );
}
