import { defineConfig, devices } from "@playwright/test";

/**
 * Requires the backend + Postgres to already be running separately
 * (Playwright can't orchestrate that Java/DB stack) - see regression/README.md.
 * The frontend dev server is started automatically if it isn't already up.
 */
export default defineConfig({
  testDir: "./tests",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: [["html", { open: "never" }]],

  use: {
    baseURL: "http://localhost:5173",
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },

  // The default run is the desktop browser over every spec. `MOBILE=android` swaps in an Android phone (Chrome on a
  // Pixel 7) that runs only the shopper-side specs listed below - the specs that set a shop up through the API and
  // then work only on marketplace-web - so a phone-sized screen is checked without running the vendor and admin
  // screens (frontend-web) at phone width. Used while building the Capacitor Android app: `npm run test:mobile`.
  projects: process.env.MOBILE
    ? [
        {
          name: "mobile-android",
          use: { ...devices["Pixel 7"] },
          testMatch: ["**/appointment-booking.spec.ts", "**/mobile-shopper.spec.ts", "**/city-pill.spec.ts", "**/pricing-page.spec.ts"],
        },
      ]
    : [
        {
          name: "chromium",
          use: { ...devices["Desktop Chrome"] },
        },
      ],

  webServer: [
    {
      command: "npm run dev --prefix ../frontend-web",
      url: "http://localhost:5173",
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
    {
      command: "npm run dev --prefix ../marketplace-web",
      url: "http://localhost:5174",
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
  ],
});
