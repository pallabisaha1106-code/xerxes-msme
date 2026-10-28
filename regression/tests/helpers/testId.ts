/**
 * A YYYYMMDDHHMMSS stamp, unique enough per run that a "no teardown" spec
 * never collides with its own leftover data from a previous run - unlike the
 * suite's other specs, these are meant to accumulate real-looking data
 * rather than clean up after themselves, so there's nothing to delete first.
 */
export function timestampSuffix(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return (
    String(now.getFullYear()) +
    pad(now.getMonth() + 1) +
    pad(now.getDate()) +
    pad(now.getHours()) +
    pad(now.getMinutes()) +
    pad(now.getSeconds())
  );
}
