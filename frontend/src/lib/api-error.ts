import { AxiosError } from "axios";

import type { ApiError } from "@/types/api";

/**
 * Extracts a user-displayable message from a failed axios call against the
 * backend, falling back gracefully for network errors (backend unreachable)
 * or unexpected non-ApiError bodies.
 */
export function getErrorMessage(error: unknown): string {
    if (error instanceof AxiosError) {
        const apiError = error.response?.data as ApiError | undefined;
        if (apiError?.message) {
            return apiError.message;
        }
        if (!error.response) {
            return "Unable to reach the server. Please check your connection and try again.";
        }
    }
    return "Something went wrong. Please try again.";
}

/**
 * Extracts field-level validation errors (400 Bad Request from Bean
 * Validation) as a `field -> message` map, for inline form display.
 */
export function getFieldErrors(error: unknown): Record<string, string> {
    if (error instanceof AxiosError) {
        const apiError = error.response?.data as ApiError | undefined;
        if (apiError?.fieldErrors?.length) {
            return Object.fromEntries(apiError.fieldErrors.map((fe) => [fe.field, fe.message]));
        }
    }
    return {};
}
