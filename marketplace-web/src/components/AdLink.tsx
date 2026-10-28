import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { contactURL } from "../lib/urlSafety";
import type { AdvertisementDto } from "../api/marketplaceApi";

type AdLinkTarget = { kind: "route" | "anchor" | "external"; href: string } | null;

/** Built-in default ads may point inside the app ("/", "#marketplace"); real ads stay external http(s) only. */
export function resolveAdLink(ad: AdvertisementDto): AdLinkTarget {
  const raw = ad.linkUrl ?? "";
  if (ad.isDefault && raw.startsWith("/") && !raw.startsWith("//")) return { kind: "route", href: raw };
  if (ad.isDefault && raw.startsWith("#")) return { kind: "anchor", href: raw };
  const external = raw ? contactURL(raw) : "";
  return external ? { kind: "external", href: external } : null;
}

export function AdLink({ ad, className, label, children }: { ad: AdvertisementDto; className: string; label: string; children: ReactNode }) {
  const target = resolveAdLink(ad);
  if (!target) return <div className={className} aria-label={label}>{children}</div>;
  if (target.kind === "route") return <Link className={className} to={target.href} aria-label={label}>{children}</Link>;
  if (target.kind === "anchor") return <a className={className} href={target.href} aria-label={label}>{children}</a>;
  return (
    <a className={className} href={target.href} target="_blank" rel="noopener noreferrer" aria-label={`${label} (opens in a new tab)`}>
      {children}
    </a>
  );
}
