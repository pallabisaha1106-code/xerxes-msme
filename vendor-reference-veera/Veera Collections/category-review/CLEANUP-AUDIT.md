# Website cleanup audit

Completed 8 September 2026. No deployment performed.

## Removed from the project

- 646 unused files, totalling 246,473,898 bytes (246.5 MB).
- Four obsolete backup/review directories: `backup-before-final-seo-verification-20260907-160428`, `seo-backup-20260907-130524`, `seo-review`, and `final-seo-verification`.
- Old contact sheets, superseded verification output, baseline snapshots and one-time header migration scripts under `category-review`.
- One unreferenced image: `assets/brand/veera-collections-logo-final.png`.

Every removed file was archived outside the website project and its archived SHA-256 hash was verified before deletion. The exact inventory and recovery location are recorded in `cleanup-plan.json` and `cleanup-result.json`.

## Verified and retained

- Audited all 29 current HTML pages and 3,855 local HTML references, including stylesheet/script/image URLs, responsive image sources, page links and static anchors. No missing local files or static anchors were found.
- Inspected CSS, JavaScript, shared components, the product catalogue, manifest, sitemap and redirects when deciding whether an asset was unused. Dynamic product-image references and fallback component references were retained.
- All surviving website files were compared against their pre-cleanup hashes. Page markup, header, footer, CSS, JavaScript, product data, manifest and security/routing configuration are unchanged.
- All 201 product records and their responsive image assets remain in the project.
- Current legacy pages remain because they are referenced by the website, sitemap, redirects, or existing feature configuration. A page being absent from the main header does not make it unused.
- Catalogue builders, source manifest, observations, template, local preview server and owner-review notes remain available for maintenance.

## Findings left unchanged to preserve behaviour

- `_redirects` has two competing entries for `/ethnic-wear-pune.html`. Its first entry points to `/festive-wear`; the later entry points to `/ethnic-wear-pune`. This is existing routing behaviour, not an unused visible link. No routing edits were made.
- The existing sitemap still lists legacy routes and does not include every newly created category page or `more.html`. It was retained because changing indexing/routing is outside this file-only cleanup.
- External destinations (WhatsApp, maps, social profiles and fonts) were not contacted or changed. The audit does not assert their remote availability.

No visible links were removed: the local-reference audit found no broken links, and removing linked legacy destinations would alter the website. Archived backup copies of pages were removed instead.
