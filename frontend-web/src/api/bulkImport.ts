export interface BulkImportError {
  rowIndex: number;
  message: string;
}

export interface BulkImportResult {
  successCount: number;
  failureCount: number;
  errors: BulkImportError[];
}
