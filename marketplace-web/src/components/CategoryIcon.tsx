import {
  Baby,
  CookingPot,
  Ellipsis,
  Footprints,
  Gem,
  LayoutGrid,
  Laptop,
  Shirt,
  ShoppingBag,
  ShoppingBasket,
  Smartphone,
  Sofa,
  Sparkles,
  Tv,
  UtensilsCrossed,
  Wrench,
  type LucideIcon,
} from "lucide-react";

/** Keyed by lowercased category name - the categories seeded in V6__marketplace_listings.sql, plus the "All" pill. */
const ICONS: Record<string, LucideIcon> = {
  all: LayoutGrid,
  "ladies wear": Shirt,
  "men's wear": ShoppingBag,
  "kids wear": Baby,
  footwear: Footprints,
  jewellery: Gem,
  "beauty & cosmetics": Sparkles,
  electronics: Tv,
  "mobile shops": Smartphone,
  grocery: ShoppingBasket,
  "home & kitchen": CookingPot,
  furniture: Sofa,
  restaurants: UtensilsCrossed,
  services: Wrench,
  "computer/laptop shop": Laptop,
  other: Ellipsis,
};

/** A category with no entry above keeps showing the glyph the API sent, exactly as before. */
export function CategoryIcon({ name, fallback }: { name: string; fallback: string }) {
  const Icon = ICONS[name.trim().toLowerCase()];
  if (!Icon) return <span aria-hidden="true">{fallback}</span>;
  return <Icon size={16} strokeWidth={1.75} aria-hidden="true" />;
}
