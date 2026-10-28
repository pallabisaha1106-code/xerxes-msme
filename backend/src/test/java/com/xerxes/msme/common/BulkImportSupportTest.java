package com.xerxes.msme.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.xerxes.msme.common.exception.DuplicateResourceException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.junit.jupiter.api.Test;

class BulkImportSupportTest {

    private final BulkImportSupport bulkImportSupport =
            new BulkImportSupport(Validation.buildDefaultValidatorFactory().getValidator());

    record NamedRequest(@NotBlank String name) {}

    @Test
    void allRowsSucceed_reportsFullSuccessAndNoErrors() {
        List<NamedRequest> requests = List.of(new NamedRequest("Alpha"), new NamedRequest("Beta"));
        List<String> created = new java.util.ArrayList<>();

        BulkImportResult result = bulkImportSupport.run(requests, r -> created.add(r.name()));

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(result.errors()).isEmpty();
        assertThat(created).containsExactly("Alpha", "Beta");
    }

    @Test
    void validationFailure_isRecordedAsErrorWithoutCallingCreator_andDoesNotStopLaterRows() {
        List<NamedRequest> requests = List.of(new NamedRequest(""), new NamedRequest("Beta"));
        List<String> created = new java.util.ArrayList<>();

        BulkImportResult result = bulkImportSupport.run(requests, r -> created.add(r.name()));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).rowIndex()).isEqualTo(0);
        assertThat(created).containsExactly("Beta");
    }

    @Test
    void serviceException_isRecordedAsErrorWithoutStoppingLaterRows() {
        List<NamedRequest> requests = List.of(new NamedRequest("duplicate"), new NamedRequest("ok"));

        BulkImportResult result = bulkImportSupport.run(requests, r -> {
            if (r.name().equals("duplicate")) {
                throw new DuplicateResourceException("already exists: " + r.name());
            }
        });

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.errors().get(0).rowIndex()).isEqualTo(0);
        assertThat(result.errors().get(0).message()).contains("already exists");
    }

    @Test
    void emptyBatch_reportsZeroCounts() {
        BulkImportResult result = bulkImportSupport.run(List.<NamedRequest>of(), r -> {});

        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(result.errors()).isEmpty();
    }
}
