package com.xerxes.msme.common;

/** rowIndex is 0-based, over the data rows only (the CSV header row isn't counted). */
public record BulkImportError(int rowIndex, String message) {}
