package com.xerxes.msme.common;

import java.util.List;

public record BulkImportResult(int successCount, int failureCount, List<BulkImportError> errors) {}
