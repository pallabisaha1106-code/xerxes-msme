#!/usr/bin/env bash
set -euo pipefail

# Resets the local dev database to a clean slate for manual testing.
#
# Deletes every tenant except the seeded platform tenant, which cascades
# (ON DELETE CASCADE on tenant_id everywhere) to remove all users, products,
# variants, stock, vendors, customers, purchases, invoices, storefront
# settings/orders, and marketplace listings for every other tenant - one
# statement handles the whole tree, see backend/src/main/resources/db/migration.
#
# Left untouched (platform reference data, not tenant data):
#   - marketplace_categories, default_product_categories, default_products
#   - the platform tenant + its Super Admin login (admin@xerxes.internal)

CONTAINER="${POSTGRES_CONTAINER:-xerxes-msme-postgres}"
DB_NAME="${DB_NAME:-xerxes_msme}"
DB_USER="${DB_USER:-postgres}"
PLATFORM_TENANT_ID="00000000-0000-0000-0000-000000000001"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STORAGE_ROOT="${STORAGE_ROOT:-$SCRIPT_DIR/../backend/storage}"

if [[ "${1:-}" != "-y" && "${1:-}" != "--yes" ]]; then
  echo "This will permanently delete ALL tenants, users, products, orders, invoices,"
  echo "and marketplace listings except the platform Super Admin (admin@xerxes.internal)."
  read -r -p "Continue? [y/N] " confirm
  if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Aborted."
    exit 1
  fi
fi

echo "Tenants before cleanup:"
docker exec "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
  "SELECT id, business_name, email, is_platform FROM tenants ORDER BY created_at;"

echo "Deleting all non-platform tenants (cascades to their users/products/orders/etc.)..."
docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$SCRIPT_DIR/reset-test-data.sql"

if [[ -d "$STORAGE_ROOT" ]]; then
  echo "Removing uploaded-file folders under $STORAGE_ROOT ..."
  find "$STORAGE_ROOT" -mindepth 1 -maxdepth 1 -type d ! -name "$PLATFORM_TENANT_ID" -exec rm -rf {} +
else
  echo "Storage root not found at $STORAGE_ROOT, skipping file cleanup (set STORAGE_ROOT to override)."
fi

echo
echo "Done. Remaining tenants:"
docker exec "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
  "SELECT id, business_name, email, is_platform FROM tenants;"
echo
echo "Marketplace categories and the starter product catalog were left untouched."
echo "Super Admin login is unchanged: admin@xerxes.internal"
