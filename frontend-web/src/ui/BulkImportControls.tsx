import { useRef, useState } from "react";
import { downloadCsv, parseCsv } from "../lib/csv";
import type { BulkImportResult } from "../api/bulkImport";
import { extractErrorMessage } from "../api/client";
import { Button } from "./Button";
import { Dialog } from "./Dialog";
import { useToast } from "./Toast";

export interface BulkImportControlsProps {
  /** Filename for the downloaded sample, e.g. "vendors-sample.csv". */
  sampleFilename: string;
  /** Header row + one or more example data rows. */
  sampleRows: (string | number)[][];
  /**
   * Receives the parsed data rows (header row already stripped). Should
   * build the resource-specific request objects and call the resource's
   * bulk-create API.
   */
  onUpload: (dataRows: string[][]) => Promise<BulkImportResult>;
  /** Called after a completed import (even a partial one) so the caller can refresh its list. */
  onImportComplete: () => void;
}

export function BulkImportControls({
  sampleFilename,
  sampleRows,
  onUpload,
  onImportComplete,
}: BulkImportControlsProps) {
  const toast = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [lastResult, setLastResult] = useState<BulkImportResult | null>(null);
  const [errorsDialogVisible, setErrorsDialogVisible] = useState(false);

  function handleDownloadSample() {
    downloadCsv(sampleFilename, sampleRows);
  }

  async function handleFileSelected(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = ""; // allow re-selecting the same file next time
    if (!file) return;

    setUploading(true);
    try {
      const text = await file.text();
      const rows = parseCsv(text);
      const dataRows = rows.slice(1); // drop the header row

      if (dataRows.length === 0) {
        toast.show({ severity: "warning", summary: "Nothing to import", detail: "The file has no data rows." });
        return;
      }

      const result = await onUpload(dataRows);
      setLastResult(result);

      if (result.failureCount === 0) {
        toast.show({
          severity: "success",
          summary: "Import complete",
          detail: `${result.successCount} row${result.successCount === 1 ? "" : "s"} imported.`,
        });
      } else {
        toast.show({
          severity: result.successCount > 0 ? "warning" : "error",
          summary: "Import finished with errors",
          detail: `${result.successCount} succeeded, ${result.failureCount} failed. Click "View Errors" for details.`,
        });
      }

      onImportComplete();
    } catch (err) {
      toast.show({ severity: "error", summary: "Import failed", detail: extractErrorMessage(err) });
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="bulk-import-controls">
      <Button size="sm" variant="outlined" severity="secondary" icon="package" onClick={handleDownloadSample}>
        Download Sample CSV
      </Button>
      <Button
        size="sm"
        variant="outlined"
        severity="secondary"
        icon="fileImport"
        loading={uploading}
        onClick={() => fileInputRef.current?.click()}
      >
        Upload CSV
      </Button>
      {lastResult && lastResult.errors.length > 0 && (
        <Button size="sm" variant="text" severity="danger" onClick={() => setErrorsDialogVisible(true)}>
          View Errors ({lastResult.errors.length})
        </Button>
      )}
      <input
        ref={fileInputRef}
        type="file"
        accept=".csv,text/csv"
        style={{ display: "none" }}
        onChange={handleFileSelected}
      />

      <Dialog
        visible={errorsDialogVisible}
        onHide={() => setErrorsDialogVisible(false)}
        title="Import Errors"
        footer={
          <Button variant="text" severity="secondary" onClick={() => setErrorsDialogVisible(false)}>
            Close
          </Button>
        }
      >
        <ul className="bulk-import-error-list">
          {lastResult?.errors.map((error) => (
            // +2: the CSV header line, plus 1-based row numbering, so this
            // matches the line the user would actually count in their editor.
            <li key={error.rowIndex}>
              <strong>Row {error.rowIndex + 2}:</strong> {error.message}
            </li>
          ))}
        </ul>
      </Dialog>
    </div>
  );
}
