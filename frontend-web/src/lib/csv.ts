/**
 * Minimal RFC4180-ish CSV parse/write, hand-rolled to avoid pulling in a
 * dependency for something this small. Handles quoted fields (with embedded
 * commas, newlines, and escaped "" quotes) and both CRLF and LF line endings.
 */
import { downloadBlob } from "./download";

export function parseCsv(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let field = "";
  let inQuotes = false;

  // Normalize CRLF/CR to LF so the state machine only has to handle one
  // line-ending case, then walk the string once, character by character.
  const normalized = text.replace(/\r\n/g, "\n").replace(/\r/g, "\n");

  for (let i = 0; i < normalized.length; i++) {
    const char = normalized[i];

    if (inQuotes) {
      if (char === '"') {
        if (normalized[i + 1] === '"') {
          field += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else {
        field += char;
      }
      continue;
    }

    if (char === '"') {
      inQuotes = true;
    } else if (char === ",") {
      row.push(field);
      field = "";
    } else if (char === "\n") {
      row.push(field);
      rows.push(row);
      row = [];
      field = "";
    } else {
      field += char;
    }
  }

  // Flush the last field/row if the file didn't end with a newline.
  if (field.length > 0 || row.length > 0) {
    row.push(field);
    rows.push(row);
  }

  // Drop fully blank trailing rows (a common artifact of a trailing newline).
  return rows.filter((r) => !(r.length === 1 && r[0].trim() === ""));
}

function csvEscape(value: string | number): string {
  const str = String(value);
  if (/[",\n]/.test(str)) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

export function toCsvString(rows: (string | number)[][]): string {
  return rows.map((row) => row.map(csvEscape).join(",")).join("\r\n");
}

export function downloadCsv(filename: string, rows: (string | number)[][]): void {
  const blob = new Blob([toCsvString(rows)], { type: "text/csv;charset=utf-8;" });
  downloadBlob(filename, blob);
}
