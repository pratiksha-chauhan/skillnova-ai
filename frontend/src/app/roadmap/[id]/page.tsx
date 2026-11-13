"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import Link from "next/link";
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
import type {
    RoadmapResponse,
    RoadmapTaskResponse,
    TaskStatusValue,
} from "@/types/roadmap";

type RoadmapState =
    | { status: "loading" }
    | { status: "error"; message: string }
    | { status: "ready"; data: RoadmapResponse };

const STATUS_CYCLE: Record<TaskStatusValue, TaskStatusValue> = {
    PENDING: "IN_PROGRESS",
    IN_PROGRESS: "COMPLETED",
    COMPLETED: "PENDING",
};

function taskStatusVariant(status: TaskStatusValue): "default" | "secondary" | "outline" {
    if (status === "COMPLETED") return "default";
    if (status === "IN_PROGRESS") return "secondary";
    return "outline";
}

function roadmapStatusVariant(status: string): "default" | "secondary" | "outline" {
    if (status === "COMPLETED") return "default";
    if (status === "ARCHIVED") return "outline";
    return "secondary";
}

function priorityVariant(priority: string): "destructive" | "secondary" | "outline" {
    if (priority === "HIGH") return "destructive";
    if (priority === "MEDIUM") return "secondary";
    return "outline";
}

/** Groups tasks by month, then week, each sorted ascending; sorts tasks within a group by sortOrder. Tasks with no month/week land in a trailing "Unscheduled" bucket. */
function groupTasks(tasks: RoadmapTaskResponse[]) {
    const scheduled = new Map<number, Map<number, RoadmapTaskResponse[]>>();
    const unscheduled: RoadmapTaskResponse[] = [];

    for (const task of tasks) {
        if (task.monthNumber == null || task.weekNumber == null) {
            unscheduled.push(task);
            continue;
        }
        if (!scheduled.has(task.monthNumber)) {
            scheduled.set(task.monthNumber, new Map());
        }
        const weeks = scheduled.get(task.monthNumber)!;
        if (!weeks.has(task.weekNumber)) {
            weeks.set(task.weekNumber, []);
        }
        weeks.get(task.weekNumber)!.push(task);
    }

    const months = [...scheduled.entries()]
        .sort(([a], [b]) => a - b)
        .map(([month, weeks]) => ({
            month,
            weeks: [...weeks.entries()]
                .sort(([a], [b]) => a - b)
                .map(([week, weekTasks]) => ({
                    week,
                    tasks: [...weekTasks].sort(
                        (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0),
                    ),
                })),
        }));

    return { months, unscheduled };
}

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
    const [taskError, setTaskError] = useState<string | null>(null);
    const [pendingTaskId, setPendingTaskId] = useState<string | null>(null);

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

    const cycleTaskStatus = useCallback(
        async (task: RoadmapTaskResponse) => {
            if (state.status !== "ready" || pendingTaskId) {
                return;
            }
            const nextStatus = STATUS_CYCLE[task.status];
            const previousData = state.data;

            // Optimistic update: flip this task's status immediately, roll
            // back to the previous snapshot if the PATCH fails.
            setTaskError(null);
            setPendingTaskId(task.id);
            setState({
                status: "ready",
                data: {
                    ...previousData,
                    tasks: previousData.tasks.map((t) =>
                        t.id === task.id ? { ...t, status: nextStatus } : t,
                    ),
                },
            });

            try {
                const response = await api.patch<RoadmapTaskResponse>(
                    `/api/roadmap/${roadmapId}/tasks/${task.id}`,
                    { status: nextStatus },
                );
                setState((current) =>
                    current.status === "ready"
                        ? {
                              status: "ready",
                              data: {
                                  ...current.data,
                                  tasks: current.data.tasks.map((t) =>
                                      t.id === task.id ? response.data : t,
                                  ),
                              },
                          }
                        : current,
                );
            } catch (error) {
                setState({ status: "ready", data: previousData });
                setTaskError(getErrorMessage(error as AxiosError));
            } finally {
                setPendingTaskId(null);
            }
        },
        [state, pendingTaskId, roadmapId],
    );

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
                    <CardContent className="flex gap-2">
                        <Button variant="outline" onClick={() => void fetchRoadmap()}>
                            Retry
                        </Button>
                        <Button variant="ghost" render={<Link href="/dashboard" />}>
                            Back to dashboard
                        </Button>
                    </CardContent>
                </Card>
            )}

            {state.status === "ready" && (
                <RoadmapDetail
                    roadmap={state.data}
                    taskError={taskError}
                    pendingTaskId={pendingTaskId}
                    onTaskClick={cycleTaskStatus}
                />
            )}
        </AppShell>
    );
}

function RoadmapDetail({
    roadmap,
    taskError,
    pendingTaskId,
    onTaskClick,
}: {
    roadmap: RoadmapResponse;
    taskError: string | null;
    pendingTaskId: string | null;
    onTaskClick: (task: RoadmapTaskResponse) => void;
}) {
    const { months, unscheduled } = groupTasks(roadmap.tasks);

    return (
        <div className="mx-auto flex max-w-3xl flex-col gap-6">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold">{roadmap.targetRole}</h1>
                    <p className="mt-1 text-sm text-muted-foreground">{roadmap.summary}</p>
                    <div className="mt-2 flex flex-wrap gap-2 text-xs text-muted-foreground">
                        {roadmap.currentJobRole && <span>From: {roadmap.currentJobRole}</span>}
                        {roadmap.targetTimelineMonths != null && (
                            <span>&middot; {roadmap.targetTimelineMonths} month timeline</span>
                        )}
                        {roadmap.dailyStudyHours != null && (
                            <span>&middot; {roadmap.dailyStudyHours}h/day</span>
                        )}
                    </div>
                </div>
                <Badge variant={roadmapStatusVariant(roadmap.status)}>{roadmap.status}</Badge>
            </div>

            {taskError && (
                <p className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
                    {taskError}
                </p>
            )}

            {roadmap.skillGaps.length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle>Skill gaps</CardTitle>
                    </CardHeader>
                    <CardContent className="flex flex-col gap-2">
                        {roadmap.skillGaps.map((gap) => (
                            <div key={gap.id} className="flex items-center justify-between text-sm">
                                <span>
                                    {gap.skillName}{" "}
                                    <span className="text-muted-foreground">
                                        ({gap.currentLevel} &rarr; {gap.requiredLevel})
                                    </span>
                                </span>
                                <Badge variant={priorityVariant(gap.priority)}>{gap.priority}</Badge>
                            </div>
                        ))}
                    </CardContent>
                </Card>
            )}

            <Card>
                <CardHeader>
                    <CardTitle>Tasks ({roadmap.tasks.length})</CardTitle>
                    <CardDescription>Click a task to cycle its status.</CardDescription>
                </CardHeader>
                <CardContent className="flex flex-col gap-5">
                    {months.map(({ month, weeks }) => (
                        <div key={month} className="flex flex-col gap-3">
                            <h3 className="text-sm font-semibold">Month {month}</h3>
                            {weeks.map(({ week, tasks }) => (
                                <div key={week} className="flex flex-col gap-1.5 pl-3">
                                    <p className="text-xs font-medium text-muted-foreground">
                                        Week {week}
                                    </p>
                                    {tasks.map((task) => (
                                        <TaskRow
                                            key={task.id}
                                            task={task}
                                            disabled={pendingTaskId === task.id}
                                            onClick={() => onTaskClick(task)}
                                        />
                                    ))}
                                </div>
                            ))}
                        </div>
                    ))}
                    {unscheduled.length > 0 && (
                        <div className="flex flex-col gap-1.5">
                            <h3 className="text-sm font-semibold">Unscheduled</h3>
                            {unscheduled.map((task) => (
                                <TaskRow
                                    key={task.id}
                                    task={task}
                                    disabled={pendingTaskId === task.id}
                                    onClick={() => onTaskClick(task)}
                                />
                            ))}
                        </div>
                    )}
                </CardContent>
            </Card>

            {roadmap.certifications.length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle>Recommended certifications</CardTitle>
                    </CardHeader>
                    <CardContent className="flex flex-col gap-2">
                        {roadmap.certifications.map((cert) => (
                            <div key={cert.id} className="flex items-center justify-between text-sm">
                                <span>
                                    {cert.url ? (
                                        <a
                                            href={cert.url}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="font-medium underline-offset-4 hover:underline"
                                        >
                                            {cert.title}
                                        </a>
                                    ) : (
                                        <span className="font-medium">{cert.title}</span>
                                    )}{" "}
                                    <span className="text-muted-foreground">({cert.provider})</span>
                                </span>
                                <Badge variant={priorityVariant(cert.priority)}>{cert.priority}</Badge>
                            </div>
                        ))}
                    </CardContent>
                </Card>
            )}

            {roadmap.projects.length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle>Recommended projects</CardTitle>
                    </CardHeader>
                    <CardContent className="flex flex-col gap-3">
                        {roadmap.projects.map((project) => (
                            <div key={project.id} className="flex flex-col gap-1 text-sm">
                                <div className="flex items-center justify-between">
                                    <span className="font-medium">{project.title}</span>
                                    <Badge variant="outline">{project.difficulty}</Badge>
                                </div>
                                <p className="text-muted-foreground">{project.description}</p>
                                <p className="text-xs text-muted-foreground">{project.techStack}</p>
                            </div>
                        ))}
                    </CardContent>
                </Card>
            )}
        </div>
    );
}

function TaskRow({
    task,
    disabled,
    onClick,
}: {
    task: RoadmapTaskResponse;
    disabled: boolean;
    onClick: () => void;
}) {
    return (
        <button
            type="button"
            onClick={onClick}
            disabled={disabled}
            className="flex items-center justify-between gap-4 rounded-md border px-3 py-2 text-left text-sm transition-colors hover:bg-muted/50 disabled:cursor-wait disabled:opacity-60"
        >
            <div>
                <p className={task.status === "COMPLETED" ? "line-through" : undefined}>
                    {task.title}
                </p>
                <p className="text-xs text-muted-foreground">{task.description}</p>
            </div>
            <div className="flex shrink-0 items-center gap-2">
                <Badge variant="outline">{task.taskType}</Badge>
                <Badge variant={taskStatusVariant(task.status)}>{task.status}</Badge>
            </div>
        </button>
    );
}
