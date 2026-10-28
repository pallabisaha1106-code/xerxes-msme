/**
 * A failed API call where the server actually responded (as opposed to a network-level
 * failure - offline, DNS, CORS, backend unreachable - where fetch() itself rejects before
 * any response exists). `status` lets a caller check for a specific outcome (e.g. 404)
 * robustly, instead of string-matching the message text.
 */
export class ApiRequestError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
  }
}

/**
 * Builds an ApiRequestError from a non-ok Response, extracting the backend's real
 * message/fieldErrors (the same ApiError shape GlobalExceptionHandler returns) instead of
 * just reporting the status code - callers should throw the result of this rather than a
 * plain Error, so the real backend message reaches the UI.
 */
export async function toApiError(response: Response, fallback: string): Promise<ApiRequestError> {
  const body = await response.json().catch(() => null);
  const detail = body?.fieldErrors?.length ? body.fieldErrors.join(", ") : body?.message;
  return new ApiRequestError(detail || `${fallback} (${response.status})`, response.status);
}

/**
 * Readable summary for a toast. Distinguishes a real backend error (ApiRequestError, with
 * the server's own message) from a network-level failure (fetch rejected before any
 * response came back) instead of collapsing both into the same generic text.
 */
export function extractErrorMessage(error: unknown): string {
  if (error instanceof ApiRequestError) {
    return error.message;
  }
  return "Could not reach the server. Please check your connection and try again.";
}
