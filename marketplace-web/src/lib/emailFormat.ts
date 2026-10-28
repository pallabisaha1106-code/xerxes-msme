/**
 * The email format a new account needs: a domain with a dot and a letters-only ending ("name@example.com"). The same
 * rule as the backend's EmailFormat.REGEX. Stricter than the browser's own type="email" check, which accepts "a@b".
 */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[A-Za-z]{2,}$/;

export function isValidEmail(value: string): boolean {
  return EMAIL_PATTERN.test(value.trim());
}
