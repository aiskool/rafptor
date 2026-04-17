package com.rafptor.parser.model;

/**
 * Interface implemented by every typed Structured Field.
 *
 * <p>Not marked {@code sealed} because the permitted subtypes live in the
 * sibling {@code modca} package; sealing across packages without JPMS
 * modules is not allowed. Keep the concrete implementations records to
 * retain the typed AST feel.
 */
public interface AfpStructuredField {

    StructuredFieldId id();
}
