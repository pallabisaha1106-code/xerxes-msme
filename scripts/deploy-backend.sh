#!/usr/bin/env bash
#
# Deploys backend/ to Cloud Run using `gcloud run deploy --source`, which lets
# Cloud Build read the Dockerfile straight out of backend/ (this must be run
# with backend/ as the build context - a repo-root context was the cause of
# an earlier "Dockerfile not found" Cloud Build failure on this project).
#
# Env vars (DB_*, JWT_SECRET, ...) are NOT set here. They're assumed to
# already be configured on the Cloud Run service itself (Console > Edit &
# Deploy New Revision > Variables & Secrets); a redeploy without
# --set-env-vars/--update-env-vars carries the previous revision's env vars
# forward unchanged, so this script won't touch them.
#
# The exceptions are SPRING_PROFILES_ACTIVE (always set to "prod" - see
# application-prod.yml, e.g. Swagger UI defaults off there), and
# CORS_ALLOWED_ORIGINS/DB_USER/DB_PASSWORD (optional - pass any of these
# explicitly, via env var or flag, to update them on deploy; leave them
# unset to carry the current values forward unchanged, same as every other
# env var). DB_PASSWORD on the command line/env var is visible in shell
# history and `ps` output on this machine - fine for a one-off deploy from a
# trusted machine, but prefer Secret Manager (--set-secrets) over this for
# anything more permanent.
#
# Usage:
#   ./scripts/deploy-backend.sh
#   PROJECT_ID=other-project REGION=asia-south2 ./scripts/deploy-backend.sh
#   ./scripts/deploy-backend.sh -y   # skip the confirmation prompt
#   ./scripts/deploy-backend.sh --cors-allowed-origins "http://localhost:5173,http://localhost:5174,https://xerxes-msme-marketplace-web-xxxxx.asia-south1.run.app"
#   ./scripts/deploy-backend.sh --db-user myuser --db-password 'mypassword'
#   CORS_ALLOWED_ORIGINS="https://example.com" DB_USER=myuser DB_PASSWORD='mypassword' ./scripts/deploy-backend.sh

set -euo pipefail

PROJECT_ID="${PROJECT_ID:-ps-customer-engagement-suite}"
SERVICE_NAME="${SERVICE_NAME:-xerxes-msme-backend}"
REGION="${REGION:-asia-south1}"
CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-}"
DB_USER="${DB_USER:-}"
DB_PASSWORD="${DB_PASSWORD:-}"

SKIP_CONFIRM=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    -y|--yes) SKIP_CONFIRM=true; shift ;;
    --cors-allowed-origins)
      CORS_ALLOWED_ORIGINS="${2:-}"
      if [[ -z "$CORS_ALLOWED_ORIGINS" ]]; then
        echo "Error: --cors-allowed-origins requires a value" >&2
        exit 1
      fi
      shift 2
      ;;
    --db-user)
      DB_USER="${2:-}"
      if [[ -z "$DB_USER" ]]; then
        echo "Error: --db-user requires a value" >&2
        exit 1
      fi
      shift 2
      ;;
    --db-password)
      DB_PASSWORD="${2:-}"
      if [[ -z "$DB_PASSWORD" ]]; then
        echo "Error: --db-password requires a value" >&2
        exit 1
      fi
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      echo "Usage: $0 [-y|--yes] [--cors-allowed-origins <origins>] [--db-user <user>] [--db-password <password>]" >&2
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$REPO_ROOT/backend"

if [[ ! -f "$BACKEND_DIR/Dockerfile" ]]; then
  echo "Error: $BACKEND_DIR/Dockerfile not found. Run this from the xerxes-msme repo." >&2
  exit 1
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
echo "  Source:   $BACKEND_DIR"
echo "  Account:  $ACTIVE_ACCOUNT"
echo "  SPRING_PROFILES_ACTIVE -> prod"
if [[ -n "$CORS_ALLOWED_ORIGINS" ]]; then
  echo "  CORS_ALLOWED_ORIGINS -> $CORS_ALLOWED_ORIGINS"
else
  echo "  CORS_ALLOWED_ORIGINS: unchanged (not passed)"
fi
if [[ -n "$DB_USER" ]]; then
  echo "  DB_USER -> $DB_USER"
else
  echo "  DB_USER: unchanged (not passed)"
fi
if [[ -n "$DB_PASSWORD" ]]; then
  echo "  DB_PASSWORD -> (set, hidden)"
else
  echo "  DB_PASSWORD: unchanged (not passed)"
fi
echo

if [[ "$SKIP_CONFIRM" != true ]]; then
  read -r -p "Proceed with deploy? [y/N] " confirm
  if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
  fi
fi

DEPLOY_ARGS=(
  "$SERVICE_NAME"
  --source "$BACKEND_DIR"
  --project "$PROJECT_ID"
  --region "$REGION"
  --quiet
)

ENV_UPDATES=("SPRING_PROFILES_ACTIVE=prod")
[[ -n "$CORS_ALLOWED_ORIGINS" ]] && ENV_UPDATES+=("CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}")
[[ -n "$DB_USER" ]] && ENV_UPDATES+=("DB_USER=${DB_USER}")
[[ -n "$DB_PASSWORD" ]] && ENV_UPDATES+=("DB_PASSWORD=${DB_PASSWORD}")

if [[ ${#ENV_UPDATES[@]} -gt 0 ]]; then
  # CORS_ALLOWED_ORIGINS is itself comma-separated, and DB_USER/DB_PASSWORD
  # can contain arbitrary characters - all of which collide with
  # --update-env-vars' own default "," KEY=VALUE delimiter. Switch that
  # delimiter to a control character (gcloud's "^DELIM^" syntax) that's
  # effectively guaranteed not to appear in any real value.
  DELIM=$'\x01'
  JOINED="${ENV_UPDATES[0]}"
  for kv in "${ENV_UPDATES[@]:1}"; do
    JOINED="${JOINED}${DELIM}${kv}"
  done
  DEPLOY_ARGS+=(--update-env-vars "^${DELIM}^${JOINED}")
fi

gcloud run deploy "${DEPLOY_ARGS[@]}"

echo
echo "Deployed. Service URL:"
gcloud run services describe "$SERVICE_NAME" \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --format "value(status.url)"
