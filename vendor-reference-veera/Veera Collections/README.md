# Veera Collections static website

The site is plain HTML, CSS and JavaScript. It has no framework, package install or build step.

## Shared site shell

- `components/header.html` is the only header source.
- `components/footer.html` is the only footer source.
- `js/site.js` loads both components, resolves site-relative links, highlights the active route and controls the mobile menu.

The component loader uses `fetch` on normal hosting and includes an offline fallback, so navigation still renders when a browser blocks local `file://` requests. A local HTTP server remains the most accurate preview of production hosting.

## Deployment

Upload the project root to any static host. For Cloudflare Pages, leave the build command blank and set the publish directory to `.`. Keep `_headers` and `_redirects` in the published root.

The existing public page URLs are preserved. No environment variables or secrets are required.
