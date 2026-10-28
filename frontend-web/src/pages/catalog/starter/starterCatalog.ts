import type { StarterCatalogGroup, StarterProductOption } from "../../../api/starterCatalogApi";

/** How two product names are compared: ignoring capital letters and stray spaces. */
export function nameKey(name: string): string {
  return name.trim().toLowerCase();
}

/** The groups to show for a search: a group matches by its own name or by any product inside it. */
export function filterGroups(catalog: StarterCatalogGroup[], term: string): StarterCatalogGroup[] {
  const wanted = nameKey(term);
  if (!wanted) return catalog;
  return catalog.filter(
    (group) => group.name.toLowerCase().includes(wanted) || group.products.some((p) => p.name.toLowerCase().includes(wanted)),
  );
}

/** How many of each group's products are picked, by group id. */
export function selectedCountByGroup(catalog: StarterCatalogGroup[], selectedIds: Set<string>): Map<string, number> {
  return new Map(catalog.map((group) => [group.id, group.products.filter((p) => selectedIds.has(p.id)).length]));
}

/** The picked products, in catalogue order. */
export function selectedProducts(catalog: StarterCatalogGroup[], selectedIds: Set<string>): StarterProductOption[] {
  return catalog.flatMap((group) => group.products).filter((p) => selectedIds.has(p.id));
}

/** Ids of the starter products the vendor already has in their catalogue, matched by name. */
export function ownedProductIds(catalog: StarterCatalogGroup[], ownedNames: Iterable<string>): Set<string> {
  const owned = new Set(Array.from(ownedNames, nameKey));
  return new Set(catalog.flatMap((group) => group.products).filter((p) => owned.has(nameKey(p.name))).map((p) => p.id));
}

/** True once the vendor has picked as many products as their plan still allows (null = no limit known). */
export function atCapacity(selectedCount: number, remaining: number | null): boolean {
  return remaining !== null && selectedCount >= remaining;
}
