/**
 * First-visit city/area/category preference, kept only in this browser - ported from
 * discovery-welcome.js. cityId/cityName are optional so this type change alone doesn't
 * break the dialog before it's actually taught to collect a city (next step).
 */
export interface DiscoverySelection {
  cityId?: string;
  cityName?: string;
  area: string;
  category: string;
}

interface StoredDiscovery extends DiscoverySelection {
  seen: boolean;
}

// v2 - v1 only had area/category; bumped so every existing visitor is asked
// once for a city too, rather than silently skipping the new step.
function storageKey(kind: string): string {
  return `neighbourly.discovery.v2.${kind}`;
}

export function loadDiscoveryPreference(kind: string): StoredDiscovery | null {
  try {
    const raw = localStorage.getItem(storageKey(kind));
    return raw ? (JSON.parse(raw) as StoredDiscovery) : null;
  } catch {
    return null;
  }
}

export function saveDiscoveryPreference(kind: string, selection: DiscoverySelection): void {
  try {
    localStorage.setItem(storageKey(kind), JSON.stringify({ seen: true, ...selection }));
  } catch {
    // Storage blocked (private mode, quota, etc.) - browsing still works, popup may reappear.
  }
}

/** Forgets the stored choice for this kind, so the welcome dialog shows again on next mount. */
export function clearDiscoveryPreference(kind: string): void {
  try {
    localStorage.removeItem(storageKey(kind));
  } catch {
    // Storage blocked - nothing to clear anyway.
  }
}
