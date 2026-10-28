import { CategoryIcon } from "./CategoryIcon";
import type { Category } from "../types";

export function CategoryList({
  categories,
  activeCategory,
  listingPlural,
  onSelect,
}: {
  categories: Category[];
  activeCategory: string;
  listingPlural: string;
  onSelect: (category: string) => void;
}) {
  const allCategories = [{ name: "All", icon: "▦" }, ...categories];
  return (
    <div id="category-list" className="category-list">
      {allCategories.map((category) => (
        <button
          key={category.name}
          className="category-button"
          data-category={category.name}
          aria-pressed={category.name === activeCategory}
          onClick={() => onSelect(category.name)}
        >
          <CategoryIcon name={category.name} fallback={category.icon} />
          {category.name === "All" ? "All " + listingPlural : category.name}
        </button>
      ))}
    </div>
  );
}
