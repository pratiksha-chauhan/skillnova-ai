"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { AxiosError } from "axios";

import { useRequireAuth } from "@/hooks/use-require-auth";
import { api } from "@/lib/api";
import { getErrorMessage } from "@/lib/api-error";
import { AppShell } from "@/components/layout/app-shell";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import type { DashboardResponse } from "@/types/dashboard";

type DashboardState =
    | { status: "loading" }
    | { status: "error"; message: string }
    | { status: "ready"; data: DashboardResponse };

/** Maps backend roadmap/task status strings to a Badge visual variant. */
function statusVariant(status: string): "default" | "secondary" | "outline" {
    if (status === "COMPLETED") return "default";
    if (status === "ARCHIVED") return "outline";
    return "secondary";
}

export default function DashboardPage() {
    const authStatus = useRequireAuth();
    const [state, setState] = useState<DashboardState>({ status: "loading" });

    const fetchDashboard = useCallback(async () => {
        try {
            const response = await api.get<DashboardResponse>("/api/dashboard");
            setState({ status: "ready", data: response.data });
        } catch (error) {
            setState({ status: "error", message: getErrorMessage(error as AxiosError) });
        }
    }, []);

    useEffect(() => {
        if (authStatus !== "authenticated") {
            return;
        }
        // Inline IIFE (not a named function reference) to satisfy
        // eslint-plugin-react-hooks v7's react-hooks/set-state-in-effect
        // rule - see frontend conventions carried over from Phase 12/13.
        void (async () => {
            await fetchDashboard();
        })();
    }, [authStatus, fetchDashboard]);

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
                <p className="text-sm text-muted-foreground">Loading dashboard...</p>
            )}

            {state.status === "error" && (
                <Card className="max-w-md">
                    <CardHeader>
                        <CardTitle>Couldn&apos;t load your dashboard</CardTitle>
                        <CardDescription>{state.message}</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <Button variant="outline" onClick={() => void fetchDashboard()}>
                            Retry
                        </Button>
                    </CardContent>
                </Card>
            )}

            {state.status === "ready" && (
                <DashboardContent data={state.data} />
            )}
        </AppShell>
    );
}

function DashboardContent({ data }: { data: DashboardResponse }) {
    if (data.totalRoadmaps === 0) {
        return (
            <Card className="max-w-md">
                <CardHeader>
                    <CardTitle>No roadmaps yet</CardTitle>
                    <CardDescription>
                        You haven&apos;t generated a career roadmap yet. Once you do, your
                        progress and stats will show up here.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <Button render={<Link href="/roadmap/new" />}>Generate a roadmap</Button>
                </CardContent>
            </Card>
        );
    }

    return (
        <div className="flex flex-col gap-6">
            <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <StatCard label="Total roadmaps" value={data.totalRoadmaps} />
                <StatCard label="Active roadmaps" value={data.activeRoadmaps} />
                <StatCard label="Total tasks" value={data.overallTaskStats.totalTasks} />
                <StatCard
                    label="Overall completion"
                    value={`${data.overallTaskStats.completionPercentage}%`}
                />
            </section>

            <section className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                <Card>
                    <CardHeader>
                        <CardTitle>Task breakdown</CardTitle>
                    </CardHeader>
                    <CardContent className="flex flex-col gap-3">
                        <TaskRow label="Completed" count={data.overallTaskStats.completedTasks} />
                        <TaskRow label="In progress" count={data.overallTaskStats.inProgressTasks} />
                        <TaskRow label="Pending" count={data.overallTaskStats.pendingTasks} />
                        <Progress value={data.overallTaskStats.completionPercentage} />
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle>Skill gaps by priority</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {Object.keys(data.skillGapsByPriority).length === 0 ? (
                            <p className="text-sm text-muted-foreground">
                                No skill gaps recorded.
                            </p>
                        ) : (
                            <ul className="flex flex-col gap-2">
                                {Object.entries(data.skillGapsByPriority).map(([priority, count]) => (
                                    <li
                                        key={priority}
                                        className="flex items-center justify-between text-sm"
                                    >
                                        <span>{priority}</span>
                                        <Badge variant="secondary">{count}</Badge>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </CardContent>
                </Card>
            </section>

            <section>
                <div className="mb-3 flex items-center justify-between">
                    <h2 className="text-lg font-semibold">Your roadmaps</h2>
                    <Button size="sm" render={<Link href="/roadmap/new" />}>
                        Generate new roadmap
                    </Button>
                </div>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {data.roadmaps.map((roadmap) => (
                        <Link key={roadmap.roadmapId} href={`/roadmap/${roadmap.roadmapId}`}>
                            <Card className="transition-colors hover:bg-muted/50">
                                <CardHeader>
                                    <div className="flex items-center justify-between">
                                        <CardTitle className="text-base">
                                            {roadmap.targetRole}
                                        </CardTitle>
                                        <Badge variant={statusVariant(roadmap.status)}>
                                            {roadmap.status}
                                        </Badge>
                                    </div>
                                </CardHeader>
                                <CardContent className="flex flex-col gap-2">
                                    <p className="text-sm text-muted-foreground">
                                        {roadmap.completedTasks}/{roadmap.totalTasks} tasks complete
                                    </p>
                                    <Progress value={roadmap.completionPercentage} />
                                </CardContent>
                            </Card>
                        </Link>
                    ))}
                </div>
            </section>

            <section>
                <h2 className="mb-3 text-lg font-semibold">Upcoming tasks</h2>
                {data.upcomingTasks.length === 0 ? (
                    <p className="text-sm text-muted-foreground">
                        No upcoming tasks - everything active is complete.
                    </p>
                ) : (
                    <Card>
                        <CardContent className="flex flex-col gap-0">
                            {data.upcomingTasks.map((task, index) => (
                                <div key={task.taskId}>
                                    {index > 0 && <Separator className="my-3" />}
                                    <div className="flex items-center justify-between gap-4">
                                        <div>
                                            <p className="text-sm font-medium">{task.title}</p>
                                            <p className="text-xs text-muted-foreground">
                                                {task.monthNumber != null && task.weekNumber != null
                                                    ? `Month ${task.monthNumber}, Week ${task.weekNumber}`
                                                    : "Unscheduled"}
                                            </p>
                                        </div>
                                        <Badge variant="outline">{task.taskType}</Badge>
                                    </div>
                                </div>
                            ))}
                        </CardContent>
                    </Card>
                )}
            </section>
        </div>
    );
}

function StatCard({ label, value }: { label: string; value: string | number }) {
    return (
        <Card>
            <CardContent className="flex flex-col gap-1">
                <p className="text-sm text-muted-foreground">{label}</p>
                <p className="text-2xl font-semibold">{value}</p>
            </CardContent>
        </Card>
    );
}

function TaskRow({ label, count }: { label: string; count: number }) {
    return (
        <div className="flex items-center justify-between text-sm">
            <span>{label}</span>
            <span className="font-medium">{count}</span>
        </div>
    );
}
