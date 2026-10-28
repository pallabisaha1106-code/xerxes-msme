import { useMemo, useState, type ReactNode } from "react";
import { Icon } from "./Icon";

export interface Column<T> {
  field: string;
  header: string;
  sortable?: boolean;
  body?: (row: T) => ReactNode;
  width?: string;
}

export interface TableProps<T> {
  data: T[];
  columns: Column<T>[];
  rowKey: (row: T) => string;
  loading?: boolean;
  emptyMessage?: string;
}

function getField(row: unknown, field: string): unknown {
  return (row as Record<string, unknown>)[field];
}

export function Table<T>({ data, columns, rowKey, loading = false, emptyMessage = "No records found." }: TableProps<T>) {
  const [sortField, setSortField] = useState<string | null>(null);
  const [sortOrder, setSortOrder] = useState<1 | -1>(1);

  const sorted = useMemo(() => {
    if (!sortField) return data;
    const copy = [...data];
    copy.sort((a, b) => {
      const av = getField(a, sortField);
      const bv = getField(b, sortField);
      if (av == null && bv == null) return 0;
      if (av == null) return -1 * sortOrder;
      if (bv == null) return 1 * sortOrder;
      if (av < bv) return -1 * sortOrder;
      if (av > bv) return 1 * sortOrder;
      return 0;
    });
    return copy;
  }, [data, sortField, sortOrder]);

  function toggleSort(field: string) {
    if (sortField !== field) {
      setSortField(field);
      setSortOrder(1);
    } else if (sortOrder === 1) {
      setSortOrder(-1);
    } else {
      setSortField(null);
    }
  }

  return (
    <div className="table-wrapper">
      <table className="table">
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.field} style={col.width ? { width: col.width } : undefined}>
                {col.sortable ? (
                  <button type="button" className="table-sort-header" onClick={() => toggleSort(col.field)}>
                    {col.header}
                    {sortField === col.field && (
                      <Icon
                        name="chevronDown"
                        size={14}
                        style={{ transform: sortOrder === -1 ? "rotate(180deg)" : undefined }}
                      />
                    )}
                  </button>
                ) : (
                  col.header
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading && (
            <tr>
              <td colSpan={columns.length} className="table-empty">
                Loading...
              </td>
            </tr>
          )}
          {!loading && sorted.length === 0 && (
            <tr>
              <td colSpan={columns.length} className="table-empty">
                {emptyMessage}
              </td>
            </tr>
          )}
          {!loading &&
            sorted.map((row) => (
              <tr key={rowKey(row)}>
                {columns.map((col) => (
                  <td key={col.field}>{col.body ? col.body(row) : String(getField(row, col.field) ?? "")}</td>
                ))}
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
}
