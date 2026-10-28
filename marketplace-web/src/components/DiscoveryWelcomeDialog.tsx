import { useEffect, useRef, useState, type FormEvent } from "react";
import { loadDiscoveryPreference, saveDiscoveryPreference, type DiscoverySelection } from "../lib/discoveryStorage";
import { listAllServiceCities, listServiceAreas, type ServiceCityDto } from "../api/marketplaceApi";
import type { Category } from "../types";

const POPULAR_CITY_COUNT = 8;

/** Best-effort - an empty list just means the Area step has nothing to offer beyond "All areas". */
async function fetchServiceAreaNames(cityId: string): Promise<string[]> {
  try {
    return (await listServiceAreas(cityId)).map((a) => a.name);
  } catch {
    return [];
  }
}

/**
 * Ported from js/discovery-welcome.js, now a two-step flow: city first, then
 * the original area/category screen. Shown only on a first visit (no stored
 * preference yet) for this kind ("shops"/"wholesalers"); any dismissal that
 * isn't the submit button - Skip, Escape, backdrop click - saves the neutral
 * "no city / all areas / all categories" preference, exactly like the original.
 *
 * Area/category options come from real service-area data and the category
 * master list (categories prop) - not from loaded listings, which are only
 * fetched lazily by MarketplacePage *after* this dialog resolves and would
 * otherwise always be empty at the moment this dialog needs them.
 */
export function DiscoveryWelcomeDialog({
  categories,
  kind,
  onApply,
  allowBrowseAll = true,
}: {
  categories: Category[];
  kind: string;
  onApply: (selection: DiscoverySelection) => void;
  /** False on the home page, where the offers feed needs a city: it drops the "Browse all businesses" way out. */
  allowBrowseAll?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const appliedRef = useRef(false);
  const [shouldShow, setShouldShow] = useState(false);
  const [step, setStep] = useState<"city" | "details">("city");
  const [cities, setCities] = useState<ServiceCityDto[]>([]);
  const [citiesLoading, setCitiesLoading] = useState(true);
  const [citiesFailed, setCitiesFailed] = useState(false);
  const [citySearch, setCitySearch] = useState("");
  const [showAllCities, setShowAllCities] = useState(false);
  const [cityId, setCityId] = useState("");
  const [cityName, setCityName] = useState("");
  const [areaNames, setAreaNames] = useState<string[]>([]);
  const [area, setArea] = useState("");
  const [category, setCategory] = useState("All");

  const validCategory = (value: string) => (value === "All" || categories.some((c) => c.name === value) ? value : "All");

  useEffect(() => {
    const stored = loadDiscoveryPreference(kind);
    if (stored?.seen === true) {
      (stored.cityId ? fetchServiceAreaNames(stored.cityId) : Promise.resolve<string[]>([])).then((names) => {
        const selectedArea = stored.area && names.includes(stored.area) ? stored.area : "";
        onApply({
          cityId: stored.cityId,
          cityName: stored.cityName,
          area: selectedArea,
          category: validCategory(stored.category),
        });
      });
      return;
    }
    setShouldShow(true);
    // Runs once on mount to resolve the stored preference (or show the dialog); intentionally not re-run on data changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [kind]);

  useEffect(() => {
    if (!shouldShow) return;
    listAllServiceCities()
      .then((result) => {
        setCities(result);
        setCitiesFailed(false);
      })
      .catch(() => setCitiesFailed(true))
      .finally(() => setCitiesLoading(false));
  }, [shouldShow]);

  useEffect(() => {
    if (shouldShow && ref.current && !ref.current.open) {
      ref.current.showModal();
    }
  }, [shouldShow]);

  function selectCity(city: ServiceCityDto) {
    setCityId(city.id);
    setCityName(city.name);
    setStep("details");
    fetchServiceAreaNames(city.id).then(setAreaNames);
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    // area is already constrained to areaNames by the <select>'s own options, nothing to re-validate.
    const selection: DiscoverySelection = {
      cityId: cityId || undefined,
      cityName: cityName || undefined,
      area,
      category: validCategory(category),
    };
    appliedRef.current = true;
    saveDiscoveryPreference(kind, selection);
    onApply(selection);
    ref.current?.close();
    document.getElementById("search")?.focus();
  }

  function handleClose() {
    if (!appliedRef.current) {
      const fallback: DiscoverySelection = { area: "", category: "All" };
      saveDiscoveryPreference(kind, fallback);
      onApply(fallback);
    }
    setShouldShow(false);
  }

  if (!shouldShow) return null;

  const normalizedSearch = citySearch.trim().toLowerCase();
  const filteredCities = normalizedSearch ? cities.filter((c) => c.name.toLowerCase().includes(normalizedSearch)) : cities;
  const citiesToShow = normalizedSearch || showAllCities ? filteredCities : filteredCities.slice(0, POPULAR_CITY_COUNT);

  return (
    <dialog
      id="discovery-welcome"
      ref={ref}
      aria-labelledby="welcome-title"
      aria-describedby="welcome-description"
      onClose={handleClose}
    >
      <button className="dialog-close" type="button" aria-label="Close location preferences" onClick={() => ref.current?.close()}>
        ×
      </button>
      <span className="eyebrow">WELCOME TO NEIGHBOURLY</span>
      <h2 id="welcome-title">Find your local favourites</h2>

      {step === "city" ? (
        <>
          <p id="welcome-description">Search for your city, or pick one below.</p>
          <div className="city-search">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
              <circle cx="11" cy="11" r="7" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              aria-label="Search for your city"
              placeholder="Search for your city"
              value={citySearch}
              autoFocus
              onChange={(e) => setCitySearch(e.target.value)}
            />
          </div>

          {citiesLoading && <p className="city-empty">Loading cities…</p>}
          {!citiesLoading && citiesFailed && <p className="city-empty">Couldn’t load cities - you can still browse all businesses below.</p>}
          {!citiesLoading && !citiesFailed && (
            <>
              {!normalizedSearch && <p className="city-grid-label">{showAllCities ? "All Cities" : "Popular Cities"}</p>}
              {citiesToShow.length === 0 ? (
                <p className="city-empty">No cities found.</p>
              ) : (
                <div className="city-grid">
                  {citiesToShow.map((city) => (
                    <button key={city.id} type="button" className="city-tile" onClick={() => selectCity(city)}>
                      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
                        <path d="M3 21h18" />
                        <path d="M5 21V8l5-4 5 4v13" />
                        <path d="M15 21V11l4 2v8" />
                        <path d="M9 9h.01M9 13h.01M9 17h.01" />
                      </svg>
                      <span>{city.name}</span>
                    </button>
                  ))}
                </div>
              )}
              {!normalizedSearch && !showAllCities && filteredCities.length > POPULAR_CITY_COUNT && (
                <button type="button" className="city-view-all" onClick={() => setShowAllCities(true)}>
                  View All Cities
                </button>
              )}
            </>
          )}

          {allowBrowseAll && (
            <button id="welcome-skip" type="button" onClick={() => ref.current?.close()}>
              Browse all businesses
            </button>
          )}
        </>
      ) : (
        <>
          <p id="welcome-description">Choose an area and what you’re looking for. We’ll show matching businesses and their products.</p>
          <form id="welcome-form" onSubmit={handleSubmit}>
            <label htmlFor="welcome-area">Area</label>
            <select id="welcome-area" autoFocus value={area} onChange={(e) => setArea(e.target.value)}>
              <option value="">All areas</option>
              {areaNames.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
            <label htmlFor="welcome-category">WHAT ARE YOU LOOKING FOR?</label>
            <select id="welcome-category" value={category} onChange={(e) => setCategory(e.target.value)}>
              <option value="All">All categories</option>
              {categories.map((c) => (
                <option key={c.name} value={c.name}>
                  {c.name}
                </option>
              ))}
            </select>
            <button className="primary-button" type="submit">
              Show matching businesses ↗
            </button>
            {allowBrowseAll && (
              <button id="welcome-skip" type="button" onClick={() => ref.current?.close()}>
                Browse all businesses
              </button>
            )}
          </form>
        </>
      )}
      <p className="welcome-note">You can change or reset these filters anytime.</p>
    </dialog>
  );
}
