# GCP permissions set up for the `neighbourly-513106` project

Two one-time IAM grants were needed when the app moved to the new Google Cloud project. Both give a
permission to the same account, the project's **default Compute Engine service account**.

| Item | Value |
|---|---|
| Project ID | `neighbourly-513106` |
| Project number | `800834595924` |
| Service account | `800834595924-compute@developer.gserviceaccount.com` |
| Run as (the person who ran the commands) | `pallabisaha1106@gmail.com` (needs Owner or IAM admin on the project) |

## Why this account needs permissions

This one service account does two different jobs, so it needed two different grants:

| Job | When | What it does |
|---|---|---|
| **Build** | During `./scripts/deploy-backend.sh` (`gcloud run deploy --source`) | Cloud Build runs as this account to read the uploaded source, build the image and push it to Artifact Registry. |
| **Run** | While the backend is serving requests | Cloud Run starts the container as this account, so it is the identity the backend uses to read and write images in the Cloud Storage bucket. |

## The two commands

| # | Command | Role granted | Where | Why it was needed |
|---|---|---|---|---|
| 1 | `gcloud projects add-iam-policy-binding neighbourly-513106 --member="serviceAccount:800834595924-compute@developer.gserviceaccount.com" --role="roles/run.builder"` | **Cloud Run Builder** (`roles/run.builder`) | The whole project | Fixes the build. The first deploy failed with `PERMISSION_DENIED: Build failed because the default service account is missing required IAM permissions`. New Google Cloud projects no longer give the Compute Engine account any build permissions. |
| 2 | `gcloud storage buckets add-iam-policy-binding gs://neighbourly_document --member="serviceAccount:800834595924-compute@developer.gserviceaccount.com" --role="roles/storage.objectAdmin"` | **Storage Object Admin** (`roles/storage.objectAdmin`) | Only the bucket `neighbourly_document` | Lets the running backend upload, read, list and delete the shop and product images stored in that bucket. |

## What each role contains

| Role | Permissions that matter here | Which step it covers |
|---|---|---|
| `roles/run.builder` | `storage.objects.get` | Read the uploaded source zip (the step that failed) |
| | `artifactregistry.repositories.uploadArtifacts` (and download, delete) | Push the built image |
| | `logging.logEntries.create` | Write build logs |
| | `source.repos.get` | Read source repositories |
| `roles/storage.objectAdmin` | Create, read, list, update and delete objects | The backend storing and serving images |

Because `run.builder` already includes the Artifact Registry and logging permissions, separate
"Artifact Registry Writer" and "Logs Writer" grants were not needed for the build.

## Why the bucket grant is on the bucket, not the project

The bucket is deliberately **private** (no public access). `ImageStorageService` writes images to it and
`MediaImageController` streams them back out through `/media/images/...`, using the Cloud Run service
account's own credentials (Application Default Credentials, so `GCS_CREDENTIALS_JSON` stays unset in
production). Granting the role on this one bucket gives the backend only what it needs, instead of access
to every bucket in the project.

## How this differs from the old project

The old project (`ps-customer-engagement-suite`) gave the same service account the broad **Editor** role
(plus Cloud Run Admin, Artifact Registry Writer, Logs Writer and Service Account User). Editor covered both
builds and bucket access automatically. New projects do not grant it, so the specific roles above replace it.
Editor was deliberately not copied: it gives access to almost everything in the project.

## If something fails

| Symptom | Likely cause | What to do |
|---|---|---|
| Deploy fails again with the same `PERMISSION_DENIED` | The grant has not taken effect yet | Wait a minute and retry |
| Still failing after that | Another build permission is missing | Also grant `roles/storage.objectAdmin`, `roles/artifactregistry.writer` and `roles/logging.logWriter` on the project to the same service account |
| Images fail to upload or load, with `403` in the backend logs | The bucket grant (command 2) is missing, or `GCS_BUCKET_NAME` is not `neighbourly_document` | Check the grant on the bucket and the env var on the Cloud Run service |
| Backend runtime logs do not appear in Cloud Logging | The runtime account cannot write logs | Grant `roles/logging.logWriter` on the project to the same service account |

## Related one-time setup (not covered by these two commands)

| Item | Done by |
|---|---|
| Bucket `neighbourly_document` created as private, with uniform bucket-level access | Console or `gcloud storage buckets create` |
| Environment variables on the Cloud Run service (`DB_*`, `JWT_SECRET`, `GCS_BUCKET_NAME`, `RAZORPAY_*`, `STOREFRONT_API_BASE_URL`, `CORS_ALLOWED_ORIGINS`) | Console, or the flags on `deploy-backend.sh` |
| Public access to the **backend** (`allUsers` as Cloud Run Invoker). The two web apps do not need this: `deploy-frontend-web.sh` and `deploy-marketplace-web.sh` pass `--allow-unauthenticated` themselves | Console or `gcloud run services add-iam-policy-binding` |
| Razorpay webhook URL pointed at the new backend | Razorpay dashboard |

## CORS allowed origins (set manually in the Console)

The browser blocks the two web apps from calling the backend unless the backend lists their addresses in
`CORS_ALLOWED_ORIGINS`. This is set **by hand in the Google Cloud Console**, not through the deploy script.

| Step | Detail |
|---|---|
| Where | Cloud Run > `neighbourly-backend` > Edit & deploy new revision > Variables & Secrets |
| Variable | `CORS_ALLOWED_ORIGINS` |
| Value | One comma-separated line, no spaces and no trailing slash: `http://localhost:5173,http://localhost:5174,https://neighbourly-marketplace-web-800834595924.asia-south1.run.app,https://neighbourly-frontend-web-800834595924.asia-south1.run.app` |
| What the entries are | The two `localhost` entries keep local development working; the other two are the deployed marketplace and dashboard |
| Also check | `STOREFRONT_API_BASE_URL` is the backend URL, `https://neighbourly-backend-800834595924.asia-south1.run.app` |
| Deploy the revision | Click Deploy; the new revision takes effect when it is ready |

| Later deploys | Effect |
|---|---|
| `./scripts/deploy-backend.sh` with no `--cors-allowed-origins` | Keeps the value set in the Console (the script only changes variables it is given) |
| `./scripts/deploy-backend.sh --cors-allowed-origins "..."` | Overwrites the Console value, so include all four origins |
| A new or renamed web service | Its URL changes (it contains the service name and the project number), so add the new URL here |

| Symptom | Likely cause |
|---|---|
| Pages load but shops, cities and forms show errors, and the browser console says `blocked by CORS policy` | An origin is missing, has a trailing slash or a typo, or the new revision is not deployed yet |
| The same pages show a `403` instead | The backend is still private (see the public access row above) |
