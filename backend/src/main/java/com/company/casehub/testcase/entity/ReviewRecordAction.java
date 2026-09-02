package com.company.casehub.testcase.entity;

/**
 * Lifecycle actions recorded on {@link TestCaseReviewRecordEntity}.
 *
 * <p>Frozen by Phase 7 API contract §6 and the Phase 7 implementation plan §4.1.
 * The DB CHECK constraint (V008) admits exactly these five values. There is no
 * {@code REJECTED} version status: a rejected revision keeps
 * {@code status = REVIEW} and {@code revision_closed = true}, with a
 * {@code REJECT} review record as its latest action.
 */
public enum ReviewRecordAction {
    SUBMIT,
    PUBLISH,
    RETURN,
    REJECT,
    DEPRECATE
}
