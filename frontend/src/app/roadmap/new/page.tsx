"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { AxiosError } from "axios";

import { useRequireAuth } from "@/hooks/use-require-auth";
import { api } from "@/lib/api";
import { getErrorMessage, getFieldErrors } from "@/lib/api-error";
import { AppShell } from "@/components/layout/app-shell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card";
import type { ProfileResponse } from "@/types/profile";
import type { RoadmapResponse } from "@/types/roadmap";

interface RoadmapFormValues {
    targetRole: string;
    targetTimelineMonths: string;
    currentJobRole: string;
    yearsExperience: string;
    currentSkills: string;
    dailyStudyHours: string;
}

const EMPTY_DEFAULTS: RoadmapFormValues = {
    targetRole: "",
    targetTimelineMonths: "",
    currentJobRole: "",
    yearsExperience: "",
    currentSkills: "",
    dailyStudyHours: "",
};

export default function GenerateRoadmapPage() {
    const authStatus = useRequireAuth();
    const router = useRouter();
    const [prefillLoaded, setPrefillLoaded] = useState(false);
    const [formError, setFormError] = useState<string | null>(null);
    const [generating, setGenerating] = useState(false);

    const {
        register,
        handleSubmit,
        reset,
        setError,
        formState: { errors },
    } = useForm<RoadmapFormValues>({ defaultValues: EMPTY_DEFAULTS });

    const loadProfile = useCallback(async () => {
        try {
            const response = await api.get<ProfileResponse>("/api/users/profile");
            const profile = response.data;
            reset({
                targetRole: profile.targetRole ?? "",
                targetTimelineMonths: profile.targetTimelineMonths?.toString() ?? "",
                currentJobRole: profile.currentJobRole ?? "",
                yearsExperience: profile.yearsExperience ?? "",
                currentSkills: profile.currentSkills.join(", "),
                dailyStudyHours: profile.dailyStudyHours ?? "",
            });
        } catch {
            // No stored profile defaults available (e.g. brand-new user) -
            // the form simply starts blank; generation still works as long
            // as the user fills in targetRole + targetTimelineMonths here.
        } finally {
            setPrefillLoaded(true);
        }
    }, [reset]);

    useEffect(() => {
        if (authStatus !== "authenticated") {
            return;
        }
        void (async () => {
            await loadProfile();
        })();
    }, [authStatus, loadProfile]);

    const onSubmit = async (values: RoadmapFormValues) => {
        setFormError(null);
        setGenerating(true);
        try {
            const skills = values.currentSkills
                .split(",")
                .map((s) => s.trim())
                .filter((s) => s.length > 0);

            const response = await api.post<RoadmapResponse>("/api/roadmap/generate", {
                targetRole: values.targetRole,
                targetTimelineMonths: Number(values.targetTimelineMonths),
                currentJobRole: values.currentJobRole || undefined,
                yearsExperience: values.yearsExperience ? Number(values.yearsExperience) : undefined,
                currentSkills: skills.length > 0 ? skills : undefined,
                dailyStudyHours: values.dailyStudyHours ? Number(values.dailyStudyHours) : undefined,
            });
            router.push(`/roadmap/${response.data.id}`);
        } catch (error) {
            const fieldErrors = getFieldErrors(error);
            if (Object.keys(fieldErrors).length > 0) {
                for (const [field, message] of Object.entries(fieldErrors)) {
                    if (field in EMPTY_DEFAULTS) {
                        setError(field as keyof RoadmapFormValues, { message });
                    }
                }
                setGenerating(false);
                return;
            }
            setFormError(getErrorMessage(error as AxiosError));
            setGenerating(false);
        }
    };

    if (authStatus !== "authenticated" || !prefillLoaded) {
        return (
            <main className="flex min-h-screen items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Loading...</p>
            </main>
        );
    }

    return (
        <AppShell>
            <Card className="mx-auto max-w-xl">
                <CardHeader>
                    <CardTitle>Generate a career roadmap</CardTitle>
                    <CardDescription>
                        Fields are pre-filled from your saved profile where available - adjust
                        anything below to override it for this roadmap only.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="targetRole">Target role</Label>
                            <Input
                                id="targetRole"
                                placeholder="e.g. Backend Engineer"
                                aria-invalid={!!errors.targetRole}
                                {...register("targetRole", { required: "Target role is required" })}
                            />
                            {errors.targetRole && (
                                <p className="text-sm text-destructive">{errors.targetRole.message}</p>
                            )}
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="targetTimelineMonths">Target timeline (months)</Label>
                            <Input
                                id="targetTimelineMonths"
                                type="number"
                                min={1}
                                max={120}
                                placeholder="e.g. 6"
                                aria-invalid={!!errors.targetTimelineMonths}
                                {...register("targetTimelineMonths", {
                                    required: "Target timeline is required",
                                    min: { value: 1, message: "Must be at least 1 month" },
                                    max: { value: 120, message: "Must be at most 120 months" },
                                })}
                            />
                            {errors.targetTimelineMonths && (
                                <p className="text-sm text-destructive">
                                    {errors.targetTimelineMonths.message}
                                </p>
                            )}
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="currentJobRole">Current job role (optional)</Label>
                            <Input
                                id="currentJobRole"
                                placeholder="e.g. Junior Developer"
                                {...register("currentJobRole")}
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="flex flex-col gap-1.5">
                                <Label htmlFor="yearsExperience">Years of experience</Label>
                                <Input
                                    id="yearsExperience"
                                    type="number"
                                    step="0.1"
                                    min={0}
                                    max={99.9}
                                    aria-invalid={!!errors.yearsExperience}
                                    {...register("yearsExperience", {
                                        min: { value: 0, message: "Must be at least 0" },
                                        max: { value: 99.9, message: "Must be at most 99.9" },
                                    })}
                                />
                                {errors.yearsExperience && (
                                    <p className="text-sm text-destructive">
                                        {errors.yearsExperience.message}
                                    </p>
                                )}
                            </div>
                            <div className="flex flex-col gap-1.5">
                                <Label htmlFor="dailyStudyHours">Daily study hours</Label>
                                <Input
                                    id="dailyStudyHours"
                                    type="number"
                                    step="0.1"
                                    min={0}
                                    max={24}
                                    aria-invalid={!!errors.dailyStudyHours}
                                    {...register("dailyStudyHours", {
                                        min: { value: 0, message: "Must be at least 0" },
                                        max: { value: 24, message: "Must be at most 24" },
                                    })}
                                />
                                {errors.dailyStudyHours && (
                                    <p className="text-sm text-destructive">
                                        {errors.dailyStudyHours.message}
                                    </p>
                                )}
                            </div>
                        </div>

                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="currentSkills">Current skills (comma-separated, optional)</Label>
                            <Input
                                id="currentSkills"
                                placeholder="e.g. Java, SQL, Git"
                                {...register("currentSkills")}
                            />
                        </div>

                        {formError && <p className="text-sm text-destructive">{formError}</p>}
                        {generating && (
                            <p className="text-sm text-muted-foreground">
                                Generating your roadmap - this can take up to 30 seconds...
                            </p>
                        )}
                        <Button type="submit" className="mt-2 w-full" disabled={generating}>
                            {generating ? "Generating..." : "Generate roadmap"}
                        </Button>
                    </form>
                </CardContent>
            </Card>
        </AppShell>
    );
}
