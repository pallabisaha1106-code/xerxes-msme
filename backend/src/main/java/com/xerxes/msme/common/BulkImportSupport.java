package com.xerxes.msme.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Shared row-by-row bulk import runner: one bad row (a validation failure or
 * a business-rule exception like a duplicate SKU) doesn't abort the rest of
 * the batch - each row either succeeds or is recorded as an error, and the
 * batch always returns a full summary.
 *
 * <p>{@code creator} must be a call through the real Spring proxy (e.g. a
 * controller calling {@code someService.create(...)} on an injected bean),
 * not a same-class self-invocation, so each row still gets the
 * {@code @Transactional} boundary declared on the single-item create method -
 * otherwise a failure partway through the batch could roll back rows that
 * already reported success.
 */
@Component
public class BulkImportSupport {

    private final Validator validator;

    public BulkImportSupport(Validator validator) {
        this.validator = validator;
    }

    public <T> BulkImportResult run(List<T> requests, Consumer<T> creator) {
        List<BulkImportError> errors = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < requests.size(); i++) {
            T request = requests.get(i);
            Set<ConstraintViolation<T>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                String message =
                        violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("; "));
                errors.add(new BulkImportError(i, message));
                continue;
            }

            try {
                creator.accept(request);
                successCount++;
            } catch (RuntimeException ex) {
                errors.add(new BulkImportError(i, ex.getMessage()));
            }
        }

        return new BulkImportResult(successCount, errors.size(), errors);
    }
}
