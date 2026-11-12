"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { isAxiosError } from "axios";

import { useAuth } from "@/lib/auth-context";
import { getErrorMessage, getFieldErrors } from "@/lib/api-error";
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
import type { RegisterRequest } from "@/types/auth";

interface RegisterFormValues extends RegisterRequest {
    confirmPassword: string;
}

export default function RegisterPage() {
    const { register: registerUser, status } = useAuth();
    const router = useRouter();
    const [formError, setFormError] = useState<string | null>(null);

    const {
        register,
        handleSubmit,
        getValues,
        setError,
        formState: { errors, isSubmitting },
    } = useForm<RegisterFormValues>({
        defaultValues: { fullName: "", email: "", password: "", confirmPassword: "" },
    });

    useEffect(() => {
        if (status === "authenticated") {
            router.replace("/dashboard");
        }
    }, [status, router]);

    const onSubmit = async (values: RegisterFormValues) => {
        const { confirmPassword, ...data } = values;
        void confirmPassword; // excluded from the payload; only used for client-side matching above
        setFormError(null);
        try {
            await registerUser(data);
            router.push("/dashboard");
        } catch (error) {
            // 409 Conflict (duplicate email) carries its message at the top
            // level, not in fieldErrors - map it onto the email field.
            if (isAxiosError(error) && error.response?.status === 409) {
                setError("email", { message: getErrorMessage(error) });
                return;
            }
            // 400 Bean Validation failures carry per-field messages.
            const fieldErrors = getFieldErrors(error);
            if (Object.keys(fieldErrors).length > 0) {
                for (const [field, message] of Object.entries(fieldErrors)) {
                    if (field === "fullName" || field === "email" || field === "password") {
                        setError(field, { message });
                    }
                }
                return;
            }
            setFormError(getErrorMessage(error));
        }
    };

    return (
        <main className="flex min-h-screen flex-col items-center justify-center p-8">
            <Card className="w-full max-w-sm">
                <CardHeader>
                    <CardTitle>Create your SkillNova AI account</CardTitle>
                    <CardDescription>
                        Start building your personalized career roadmap.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="fullName">Full name</Label>
                            <Input
                                id="fullName"
                                autoComplete="name"
                                aria-invalid={!!errors.fullName}
                                {...register("fullName", {
                                    required: "Full name is required",
                                    maxLength: { value: 150, message: "Full name must be at most 150 characters" },
                                })}
                            />
                            {errors.fullName && (
                                <p className="text-sm text-destructive">{errors.fullName.message}</p>
                            )}
                        </div>
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
                                autoComplete="new-password"
                                aria-invalid={!!errors.password}
                                {...register("password", {
                                    required: "Password is required",
                                    minLength: { value: 8, message: "Password must be at least 8 characters" },
                                    maxLength: { value: 72, message: "Password must be at most 72 characters" },
                                })}
                            />
                            {errors.password && (
                                <p className="text-sm text-destructive">{errors.password.message}</p>
                            )}
                        </div>
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="confirmPassword">Confirm password</Label>
                            <Input
                                id="confirmPassword"
                                type="password"
                                autoComplete="new-password"
                                aria-invalid={!!errors.confirmPassword}
                                {...register("confirmPassword", {
                                    required: "Please confirm your password",
                                    validate: (value) =>
                                        value === getValues("password") || "Passwords do not match",
                                })}
                            />
                            {errors.confirmPassword && (
                                <p className="text-sm text-destructive">{errors.confirmPassword.message}</p>
                            )}
                        </div>
                        {formError && <p className="text-sm text-destructive">{formError}</p>}
                        <Button type="submit" className="mt-2 w-full" disabled={isSubmitting}>
                            {isSubmitting ? "Creating account..." : "Create account"}
                        </Button>
                    </form>
                    <p className="mt-4 text-center text-sm text-muted-foreground">
                        Already have an account?{" "}
                        <Link href="/login" className="font-medium text-primary hover:underline">
                            Sign in
                        </Link>
                    </p>
                </CardContent>
            </Card>
        </main>
    );
}
