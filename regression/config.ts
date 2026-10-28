/**
 * Standing test account, created once by hand (not by a test - it's a real,
 * persistent business in the target database). Future test specs that need
 * to be logged in as an existing user should import TEST_USER rather than
 * hardcoding credentials or registering a fresh account each time.
 *
 * This is separate from tests/register-business.spec.ts, which deliberately
 * registers and tears down its own throwaway accounts to test the
 * registration flow itself.
 */
export const TEST_USER = {
  email: "veera@test.com",
  password: "password1234",
};

/**
 * Standing Super Admin login, same idea as TEST_USER - a real account in the
 * target database, not created by a test. The seed migration
 * (V5__platform_admin.sql) creates admin@xerxes.internal with a bcrypt hash
 * whose plaintext isn't recoverable, so this password only works once you've
 * run the one-time SQL update given alongside it (see regression/README.md's
 * "Super Admin login" section) - generated and verified against this
 * codebase's own BCryptPasswordEncoder, not guessed.
 */
export const SUPER_ADMIN = {
  email: "admin@xerxes.internal",
  password: "SuperAdmin@123",
};

/** frontend-web's own origin - the specs that live under marketplace-web's baseURL (port 5174) need this to cross over for admin actions. */
export const FRONTEND_WEB_URL = "http://localhost:5173";
