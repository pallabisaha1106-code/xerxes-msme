import { type FormEvent } from "react";
import { ChevronDown, MapPin } from "lucide-react";

export function DiscoveryStrip({
  heading,
  subheading,
  searchAriaLabel,
  searchPlaceholder,
  searchButtonLabel,
  search,
  onSearchChange,
  onSubmit,
  cityName,
  onChangeCity,
  areas,
  pincodes,
  area,
  pincode,
  onAreaChange,
  onPincodeChange,
  onReset,
  resultCount,
}: {
  heading: string;
  subheading: string;
  searchAriaLabel: string;
  searchPlaceholder: string;
  searchButtonLabel: string;
  search: string;
  onSearchChange: (value: string) => void;
  onSubmit: () => void;
  /** The chosen city, or "" when none was chosen - the pill then invites the shopper to pick one. */
  cityName: string;
  onChangeCity: () => void;
  areas: string[];
  pincodes: string[];
  area: string;
  pincode: string;
  onAreaChange: (value: string) => void;
  onPincodeChange: (value: string) => void;
  onReset: () => void;
  resultCount: string;
}) {
  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit();
    document.getElementById("marketplace")?.scrollIntoView({
      behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
    });
  }

  return (
    <section className="discovery-strip" aria-labelledby="shops-heading">
      <div className="discovery-heading">
        <h1 id="shops-heading">{heading}</h1>
        <p>{subheading}</p>
      </div>
      <div className="discovery-controls">
        <form id="search-form" className="search-box" role="search" onSubmit={handleSubmit}>
          <span aria-hidden="true">⌕</span>
          <input
            id="search"
            type="search"
            aria-label={searchAriaLabel}
            placeholder={searchPlaceholder}
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
          />
          <button type="submit">
            {searchButtonLabel} <span aria-hidden="true">↗</span>
          </button>
        </form>
        <div className="filter-controls">
          <button
            type="button"
            className="city-chip"
            aria-label={cityName ? `Change city, currently ${cityName}` : "Choose your city"}
            onClick={onChangeCity}
          >
            <MapPin size={16} strokeWidth={2} aria-hidden="true" />
            <span className="city-chip-name">{cityName || "Choose your city"}</span>
            <span className="city-chip-change" aria-hidden="true">
              Change
            </span>
            <ChevronDown size={16} strokeWidth={2} aria-hidden="true" />
          </button>
          <label className="sr-only" htmlFor="area">
            Filter by area
          </label>
          <select id="area" value={area} onChange={(e) => onAreaChange(e.target.value)}>
            <option value="">All areas</option>
            {areas.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
          <label className="sr-only" htmlFor="pincode">
            Filter by pincode
          </label>
          <select id="pincode" value={pincode} onChange={(e) => onPincodeChange(e.target.value)}>
            <option value="">All pincodes</option>
            {pincodes.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
          <button id="reset" className="reset" onClick={onReset}>
            Reset
          </button>
        </div>
      </div>
      <p id="result-count" role="status" aria-live="polite">
        {resultCount}
      </p>
    </section>
  );
}
