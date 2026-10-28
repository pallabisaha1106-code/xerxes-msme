import type { SVGProps } from "react";

/**
 * A small hand-authored line-icon set (24x24, stroke=currentColor) - just
 * the icons this app actually uses. Deliberately not a third-party icon
 * package, to keep the UI dependency-free per our own design system.
 */
const PATHS: Record<string, string> = {
  home: "M3 11.5 12 4l9 7.5 M5 10v9a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1v-9",
  tag: "M12.6 2H4a2 2 0 0 0-2 2v8.6a2 2 0 0 0 .6 1.4l9 9a2 2 0 0 0 2.8 0l7-7a2 2 0 0 0 0-2.8l-9-9A2 2 0 0 0 12.6 2ZM7 8a1 1 0 1 1 0-2 1 1 0 0 1 0 2Z",
  box: "M21 8 12 3 3 8l9 5 9-5ZM3 8v8l9 5 9-5V8 M12 13v8",
  sitemap: "M12 3v4M6 21v-4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v4M4 21h4M10 21h4M16 21h4M12 7a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z",
  truck: "M3 7h11v9H3zM14 10h4l3 3v3h-7z M6.5 19.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3ZM17.5 19.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z",
  fileImport: "M13 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9l-6-6ZM13 3v6h6 M4 12h6 M8 9l3 3-3 3",
  users: "M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM2 21v-1a6 6 0 0 1 6-6h2a6 6 0 0 1 6 6v1 M17 11a3.5 3.5 0 0 0 0-7 M22 21v-1a5.5 5.5 0 0 0-4-5.3",
  receipt: "M6 3h12v18l-3-2-3 2-3-2-3 2V3Z M9 8h6M9 12h6",
  activity: "M22 12h-4l-3 8-6-16-3 8H2",
  logOut: "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9",
  chevronDown: "m6 9 6 6 6-6",
  chevronRight: "m9 6 6 6-6 6",
  eye: "M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7Z M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z",
  eyeOff: "M17.9 17.9A10.4 10.4 0 0 1 12 20c-7 0-11-8-11-8a18.6 18.6 0 0 1 4.2-5.2M9.9 5.2A9.9 9.9 0 0 1 12 5c7 0 11 8 11 8a18.6 18.6 0 0 1-2.2 3.2 M14.1 14.1a3 3 0 1 1-4.2-4.2 M1 1l22 22",
  search: "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16ZM21 21l-4.3-4.3",
  plus: "M12 5v14M5 12h14",
  x: "M18 6 6 18M6 6l12 12",
  grid: "M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z",
  check: "m20 6-11 11-5-5",
  alertCircle: "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20ZM12 8v5 M12 16h.01",
  trash: "M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0-1 14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2L4 6h16Z",
  package: "M21 8 12 3 3 8l9 5 9-5ZM3 8v8l9 5 9-5V8",
  edit: "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4Z",
  download: "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 M7 10l5 5 5-5 M12 15V3",
  barcode: "M3 5v14 M7 5v14 M10 5v14 M13 5v14 M17 5v14 M21 5v14",
  printer: "M6 9V2h12v7 M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2 M6 14h12v8H6z",
  calendar:
    "M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
  inbox: "M22 12h-6l-2 3h-4l-2-3H2 M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11Z",
  creditCard: "M2 5h20a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H2a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z M1 10h22",
};

export type IconName = keyof typeof PATHS;

export function Icon({
  name,
  size = 16,
  strokeWidth = 2,
  ...rest
}: { name: IconName; size?: number } & SVGProps<SVGSVGElement>) {
  const path = PATHS[name];
  if (!path) return null;
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...rest}
    >
      <path d={path} />
    </svg>
  );
}
