"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";

import { useAuth } from "@/lib/auth-context";
import { getErrorMessage } from "@/lib/api-error";
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
import type { LoginRequest } from "@/types/auth";

export default function LoginPage() {
    const { login, status } = useAuth();
    const router = useRouter();
    const [formError, setFormError] = useState<string | null>(null);

    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
    } = useForm<LoginRequest>({ defaultValues: { email: "", password: "" } });

    // Already-authenticated users (e.g. navigating back to /login manually)
    // should land straight on the dashboard instead of seeing the form.
    useEffect(() => {
        if (status === "authenticated") {
            router.replace("/dashboard");
        }
    }, [status, router]);

    const onSubmit = async (data: LoginRequest) => {
        setFormError(null);
        try {
            await login(data);
            router.push("/dashboard");
        } catch (error) {
            setFormError(getErrorMessage(error));
        }
    };

    return (
        <main className="flex min-h-screen flex-col items-center justify-center p-8">
            <Card className="w-full max-w-sm">
                <CardHeader>
                    <CardTitle>Sign in to SkillNova AI</CardTitle>
                    <CardDescription>
                        Enter your credentials to access your career roadmap.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="email">Email</Label>
                            <Input
                                id="email"
                                type="email"
                                autoComplete="email"
                                aria-invalid={!!errors.email}
                                {...register("email", {
                                    required: "Email is required",
                                    pattern: {
                                        value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                                        message: "Enter a valid email address",
                                    },
                                })}
                            />
                            {errors.email && (
                                <p className="text-sm text-destructive">{errors.email.message}</p>
                            )}
                        </div>
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="password">Password</Label>
                            <Input
                                id="password"
                                type="password"
                                autoComplete="current-password"
                                aria-invalid={!!errors.password}
                                {...register("password", { required: "Password is required" })}
                            />
                            {errors.password && (
                                <p className="text-sm text-destructive">{errors.password.message}</p>
                            )}
                        </div>
                        {formError && <p className="text-sm text-destructive">{formError}</p>}
                        <Button type="submit" className="mt-2 w-full" disabled={isSubmitting}>
                            {isSubmitting ? "Signing in..." : "Sign in"}
                        </Button>
                    </form>
                    <p className="mt-4 text-center text-sm text-muted-foreground">
                        Don&apos;t have an account?{" "}
                        <Link href="/register" className="font-medium text-primary hover:underline">
                            Register
                        </Link>
                    </p>
                </CardContent>
            </Card>
        </main>
    );
}
