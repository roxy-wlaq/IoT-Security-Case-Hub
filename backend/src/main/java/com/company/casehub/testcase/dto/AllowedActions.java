package com.company.casehub.testcase.dto;

/**
 * The complete set of lifecycle actions the current user is allowed to perform
 * on the test case / version surfaced in the response. Computed entirely by the
 * Service layer; the Frontend MUST render its action bar from these flags and
 * never from raw role strings (Phase 7 implementation plan §6.4, API contract §3.1).
 *
 * @param editDraft          DRAFT ∧ revision_closed=false ∧ (ADMIN ∨ owner ∨ contributor)
 * @param createDraft        permission test_case:draft_create
 * @param submitReview       DRAFT ∧ revision_closed=false ∧ (ADMIN ∨ owner ∨ contributor)
 * @param publish            REVIEW ∧ revision_closed=false ∧ test_case:publish
 * @param returnReview       REVIEW ∧ revision_closed=false ∧ test_case:review (field avoids TS keyword)
 * @param reject             REVIEW ∧ revision_closed=false ∧ test_case:review
 * @param deprecate          PUBLISHED ∧ test_case:deprecate
 * @param createRevision     current PUBLISHED exists ∧ test_case:draft_create
 * @param manageContributors DRAFT ∧ revision_closed=false ∧ (ADMIN ∨ owner) ∧ test_case:draft_edit
 */
public record AllowedActions(boolean editDraft, boolean createDraft, boolean submitReview, boolean publish,
                             boolean returnReview, boolean reject, boolean deprecate, boolean createRevision,
                             boolean manageContributors) {
}
