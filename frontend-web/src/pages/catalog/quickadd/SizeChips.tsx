import { useState } from "react";
import { SIZE_PRESETS } from "./sizeVariants";

interface SizeChipsProps {
  sizes: string[];
  onChange: (sizes: string[]) => void;
}

/** Tap to pick the sizes a garment comes in (XS to XXL), or add any other size with "Other". Picked sizes keep the order they were picked in. */
export function SizeChips({ sizes, onChange }: SizeChipsProps) {
  const [otherOpen, setOtherOpen] = useState(false);
  const [other, setOther] = useState("");
  const custom = sizes.filter((size) => !(SIZE_PRESETS as readonly string[]).includes(size));

  const toggle = (size: string) => onChange(sizes.includes(size) ? sizes.filter((s) => s !== size) : [...sizes, size]);

  const addOther = () => {
    const value = other.trim().slice(0, 20);
    if (value && !sizes.some((size) => size.toLowerCase() === value.toLowerCase())) onChange([...sizes, value]);
    setOther("");
    setOtherOpen(false);
  };

  return (
    <div className="qa-chips" role="group" aria-label="Sizes">
      {[...SIZE_PRESETS, ...custom].map((size) => (
        <button
          key={size}
          type="button"
          className={["qa-chip", sizes.includes(size) ? "qa-chip-on" : ""].filter(Boolean).join(" ")}
          aria-pressed={sizes.includes(size)}
          onClick={() => toggle(size)}
        >
          {size}
        </button>
      ))}
      {otherOpen ? (
        <input
          className="qa-chip-input"
          autoFocus
          value={other}
          maxLength={20}
          placeholder="Size"
          aria-label="Other size"
          onChange={(event) => setOther(event.target.value)}
          onBlur={addOther}
          onKeyDown={(event) => {
            if (event.key === "Enter") addOther();
            if (event.key === "Escape") {
              setOther("");
              setOtherOpen(false);
            }
          }}
        />
      ) : (
        <button type="button" className="qa-chip" onClick={() => setOtherOpen(true)}>
          + Other
        </button>
      )}
    </div>
  );
}
