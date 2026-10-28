#!/usr/bin/env bash
#
# Builds frontend-web (the tenant/admin Vite SPA) into a static nginx image
# and deploys it to Cloud Run, using `gcloud run deploy --source` the same
# way deploy-backend.sh and deploy-marketplace-web.sh do.
#
# VITE_API_BASE_URL is baked in at build time from frontend-web/.env.production
# (already pointed at the deployed backend) - nginx just serves static
# files, it doesn't proxy or read any runtime env vars.
#
# Since this app calls the backend directly cross-origin (no same-origin
# proxy), the backend's CORS_ALLOWED_ORIGINS must include this service's
# deployed URL - see the reminder printed at the end of this script.
#
# This is a public-facing app (tenant users log in from anywhere), so
# --allow-unauthenticated is passed explicitly to skip the interactive
# "Allow unauthenticated invocations?" prompt gcloud shows on first deploy.
#
# Usage:
#   ./scripts/deploy-frontend-web.sh
#   PROJECT_ID=other-project REGION=asia-south2 ./scripts/deploy-frontend-web.sh
#   ./scripts/deploy-frontend-web.sh -y   # skip the confirmation prompt

set -euo pipefail

PROJECT_ID="${PROJECT_ID:-ps-customer-engagement-suite}"
SERVICE_NAME="${SERVICE_NAME:-xerxes-msme-frontend-web}"
REGION="${REGION:-asia-south1}"

SKIP_CONFIRM=false
for arg in "$@"; do
  case "$arg" in
    -y|--yes) SKIP_CONFIRM=true ;;
    *)
      echo "Unknown argument: $arg" >&2
      echo "Usage: $0 [-y|--yes]" >&2
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_DIR="$REPO_ROOT/frontend-web"

if [[ ! -f "$APP_DIR/Dockerfile" ]]; then
  echo "Error: $APP_DIR/Dockerfile not found. Run this from the xerxes-msme repo." >&2
  exit 1
fi

if [[ ! -f "$APP_DIR/.env.production" ]]; then
  echo "Warning: $APP_DIR/.env.production not found - the build will fall back to VITE_API_BASE_URL's empty-string default (relative /api calls, no backend host)." >&2
fi

if ! command -v gcloud >/dev/null 2>&1; then
  echo "Error: gcloud CLI not found. Install it: https://cloud.google.com/sdk/docs/install" >&2
  exit 1
fi

ACTIVE_ACCOUNT="$(gcloud config get-value account 2>/dev/null || true)"
if [[ -z "$ACTIVE_ACCOUNT" || "$ACTIVE_ACCOUNT" == "(unset)" ]]; then
  echo "Error: no active gcloud account. Run 'gcloud auth login' first." >&2
  exit 1
fi

echo "About to deploy:"
echo "  Service:  $SERVICE_NAME"
echo "  Project:  $PROJECT_ID"
echo "  Region:   $REGION"
echo "  Source:   $APP_DIR"
echo "  Account:  $ACTIVE_ACCOUNT"
echo "  Public:   yes (--allow-unauthenticated)"
echo

if [[ "$SKIP_CONFIRM" != true ]]; then
  read -r -p "Proceed with deploy? [y/N] " confirm
  if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
  fi
fi

gcloud run deploy "$SERVICE_NAME" \
  --source "$APP_DIR" \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --port 8080 \
  --allow-unauthenticated \
  --quiet

echo
echo "Deployed. Service URL:"
SERVICE_URL="$(gcloud run services describe "$SERVICE_NAME" \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --format "value(status.url)")"
echo "$SERVICE_URL"
echo
echo "Next step: add this URL to the backend's CORS_ALLOWED_ORIGINS env var"
echo "(./scripts/deploy-backend.sh --cors-allowed-origins \"...,${SERVICE_URL}\"),"
echo "otherwise every API call from this app will be blocked by CORS."
