package com.rafptor.converter.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationReport {

    private final boolean passed;
    private final List<String> errors;
    private final List<String> warnings;

    public ValidationReport(boolean passed, List<String> errors, List<String> warnings) {
        this.passed = passed;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
        this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean isPassed() { return passed; }
    public List<String> errors() { return errors; }
    public List<String> warnings() { return warnings; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public Builder error(String msg) { errors.add(msg); return this; }
        public Builder warning(String msg) { warnings.add(msg); return this; }

        public ValidationReport build() {
            return new ValidationReport(
                    errors.isEmpty(),
                    Collections.unmodifiableList(errors),
                    Collections.unmodifiableList(warnings));
        }
    }
}
