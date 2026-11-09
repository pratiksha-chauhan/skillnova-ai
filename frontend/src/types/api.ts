/**
 * Mirrors backend `com.skillnova.backend.exception.ApiError` - the
 * consistent error-response shape returned by every failed API call (see
 * GlobalExceptionHandler on the backend). `fieldErrors` is populated only
 * for Bean Validation failures (400 Bad Request); otherwise empty.
 */
export interface ApiError {
    timestamp: string;
    status: number;
    error: string;
    message: string;
    path: string;
    fieldErrors: ApiFieldError[];
}

export interface ApiFieldError {
    field: string;
    message: string;
}
