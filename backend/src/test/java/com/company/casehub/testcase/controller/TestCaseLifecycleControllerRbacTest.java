package com.company.casehub.testcase.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.MethodSecurityTestConfig;
import com.company.casehub.testcase.dto.AddContributorRequest;
import com.company.casehub.testcase.dto.CreateRevisionRequest;
import com.company.casehub.testcase.dto.LifecycleActionRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.service.TestCaseAccessPolicy;
import com.company.casehub.testcase.service.TestCaseLifecycleService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice-level RBAC test for the Phase 7 lifecycle controller. Verifies the
 * permission-code gate enforced by {@code @PreAuthorize} on each endpoint.
 * The resource-level gate (ownership / contributor / status / revision_closed)
 * is covered by {@code TestCaseLifecycleServiceTest} and {@code TestCaseLifecycleIT};
 * this test only checks that a principal lacking the permission code receives 403
 * and the service is never invoked, while a principal holding the code reaches the
 * service.
 */
@WebMvcTest(controllers = TestCaseLifecycleController.class)
@Import(MethodSecurityTestConfig.class)
class TestCaseLifecycleControllerRbacTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private TestCaseLifecycleService lifecycleService;
    @MockBean(name = "testCaseAccessPolicy") private TestCaseAccessPolicy accessPolicy;

    private static final UUID MASTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VERSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CONTRIBUTOR_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    // -------------------------------------------------------------------------
    // Negative: tester (no lifecycle authorities) is blocked at every endpoint
    // -------------------------------------------------------------------------

    @Test
    void testerCannotSubmitReview() throws Exception {
        // A plain TESTER (no submit_review authority, no contributor membership) hits the
        // permission-code gate (false) and the resource-level gate canEditDraftById (false).
        mockMvc.perform(post("/api/v1/test-cases/{masterId}/draft/submit-review", MASTER_ID)
                        .with(user(tester())).with(csrf())
                        .contentType("application/json")
                        .content("{\"comment\":\"ok\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lifecycleService);
    }

    @Test
    void testerContributorCanSubmitReview() throws Exception {
        // HIGH-02: a TESTER contributor with no global test_case:submit_review authority may
        // still submit the assigned draft because the resource-level gate (canEditDraftById)
        // resolves to true for them.
        when(accessPolicy.canEditDraftById(any(), any())).thenReturn(true);
        doReturn(detailResponse()).when(lifecycleService)
                .submitReview(eq(MASTER_ID), any(), any());

        mockMvc.perform(post("/api/v1/test-cases/{masterId}/draft/submit-review", MASTER_ID)
                        .with(user(testerContributor())).with(csrf())
                        .contentType("application/json")
                        .content("{\"comment\":\"tester submit\"}"))
                .andExpect(status().isOk());
        verify(lifecycleService).submitReview(eq(MASTER_ID), any(), any());
    }

    @Test
    void testerCannotPublish() throws Exception {
        mockMvc.perform(post("/api/v1/test-cases/{masterId}/versions/{versionId}/publish", MASTER_ID, VERSION_ID)
                        .with(user("tester")).with(csrf())
                        .contentType("application/json")
                        .content("{\"comment\":\"approved\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lifecycleService);
    }

    @Test
    void testerCannotReturn() throws Exception {
        mockMvc.perform(post("/api/v1/test-cases/{masterId}/versions/{versionId}/return", MASTER_ID, VERSION_ID)
                        .with(user("tester")).with(csrf())
                        .contentType("application/json")
                        .content("{\"comment\":\"fix\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lifecycleService);
    }

    @Test
    void testerCannotReject() throws Exception {
        mockMvc.perform(post("/api/v1/test-cases/{masterId}/versions/{versionId}/reject", MASTER_ID, VERSION_ID)
                        .with(user("tester")).with(csrf())
                        .contentType("application/json")
                        .content("{\"comment\":\"bad\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lifecycleService);
    }

    @Test
    void testerCannotDeprecate() throws Exception {
        mockMvc.perform(post("/api/v1/test-cases/{masterId}/versions/{versionId}/deprecate", MASTER_ID, VERSION_ID)
                        .with(user("tester")).with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lifecycleService);
    }

    @Test
    void testerCannotCreateRevision() throws Exception {
        mockMvc.perform(post("/api/v1/test-cases/{masterId}/revisions", MASTER_ID)
                        .with(user("tester")).with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lifecycleService);
    }

    @Test
    void testerCannotAddContributor() throws Exception {
        mockMvc.perform(post("/api/v1/test-cases/{masterId}/draft/contributors", MASTER_ID)
                        .with(user("tester")).with(csrf())
                        .contentType("application/json")
                        .content("{\"userId\":\"" + CONTRIBUTOR_USER_ID + "\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lifecycleService);
    }

    @Test
    void testerCannotRemoveContributor() throws Exception {
        mockMvc.perform(delete("/api/v1/test-cases/{masterId}/draft/contributors/{userId}", MASTER_ID, CONTRIBUTOR_USER_ID)
                        .with(user("tester")).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lifecycleService);
    }

    // -------------------------------------------------------------------------
    // Positive: principal holding the required permission codes reaches the service
    // -------------------------------------------------------------------------

    @Test
    void coordinatorCanSubmitReview() throws Exception {
        doReturn(detailResponse()).when(lifecycleService)
                .submitReview(eq(MASTER_ID), any(), any());

        mockMvc.perform(post("/api/v1/test-cases/{masterId}/draft/submit-review", MASTER_ID)
                        .with(user(coordinator())).with(csrf())
                        .contentType("application/json")
                        .content("{\"comment\":\"ready\"}"))
                .andExpect(status().isOk());
        verify(lifecycleService).submitReview(eq(MASTER_ID), any(), any());
    }

    @Test
    void adminCanPublish() throws Exception {
        doReturn(detailResponse()).when(lifecycleService)
                .publish(eq(MASTER_ID), eq(VERSION_ID), any(), any());

        mockMvc.perform(post("/api/v1/test-cases/{masterId}/versions/{versionId}/publish", MASTER_ID, VERSION_ID)
                        .with(user(admin())).with(csrf())
                        .contentType("application/json")
                        .content("{\"comment\":\"approved\"}"))
                .andExpect(status().isOk());
        verify(lifecycleService).publish(eq(MASTER_ID), eq(VERSION_ID), any(), any());
    }

    @Test
    void adminCanReject() throws Exception {
        doReturn(detailResponse()).when(lifecycleService)
                .reject(eq(MASTER_ID), eq(VERSION_ID), any(), any());

        mockMvc.perform(post("/api/v1/test-cases/{masterId}/versions/{versionId}/reject", MASTER_ID, VERSION_ID)
                        .with(user(admin())).with(csrf())
                        .contentType("application/json")
                        .content("{\"comment\":\"non-compliant\"}"))
                .andExpect(status().isOk());
        verify(lifecycleService).reject(eq(MASTER_ID), eq(VERSION_ID), any(), any());
    }

    @Test
    void adminCanDeprecate() throws Exception {
        doReturn(detailResponse()).when(lifecycleService)
                .deprecate(eq(MASTER_ID), eq(VERSION_ID), any(), any());

        mockMvc.perform(post("/api/v1/test-cases/{masterId}/versions/{versionId}/deprecate", MASTER_ID, VERSION_ID)
                        .with(user(admin())).with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
        verify(lifecycleService).deprecate(eq(MASTER_ID), eq(VERSION_ID), any(), any());
    }

    @Test
    void coordinatorCanCreateRevision() throws Exception {
        doReturn(detailResponse()).when(lifecycleService)
                .createRevision(eq(MASTER_ID), any(CreateRevisionRequest.class), any());

        mockMvc.perform(post("/api/v1/test-cases/{masterId}/revisions", MASTER_ID)
                        .with(user(coordinator())).with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isCreated());
        verify(lifecycleService).createRevision(eq(MASTER_ID), any(CreateRevisionRequest.class), any());
    }

    @Test
    void anyAuthenticatedUserCanReadReviewRecords() throws Exception {
        doReturn(List.of()).when(lifecycleService).reviewRecords(eq(MASTER_ID), eq(VERSION_ID), any());

        mockMvc.perform(get("/api/v1/test-cases/{masterId}/versions/{versionId}/review-records", MASTER_ID, VERSION_ID)
                        .with(user("tester")))
                .andExpect(status().isOk());
        verify(lifecycleService).reviewRecords(eq(MASTER_ID), eq(VERSION_ID), any());
    }

    @Test
    void anyAuthenticatedUserCanListContributors() throws Exception {
        doReturn(List.of()).when(lifecycleService).listContributors(eq(MASTER_ID), any());

        mockMvc.perform(get("/api/v1/test-cases/{masterId}/draft/contributors", MASTER_ID)
                        .with(user("tester")))
                .andExpect(status().isOk());
        verify(lifecycleService).listContributors(eq(MASTER_ID), any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static UserPrincipal coordinator() {
        return new UserPrincipal(UUID.randomUUID(), "coord", "hash", "Coordinator", true, false,
                Set.of("TEST_COORDINATOR"), Set.of("test_case:submit_review", "test_case:draft_create", "test_case:read"));
    }

    /** A plain TESTER with only read — no lifecycle authorities, no contributor membership. */
    private static UserPrincipal tester() {
        return new UserPrincipal(UUID.randomUUID(), "tester", "hash", "Tester", true, false,
                Set.of("TESTER"), Set.of("test_case:read"));
    }

    /** A TESTER contributor: no global draft_edit/submit_review, but added to a Draft. */
    private static UserPrincipal testerContributor() {
        return new UserPrincipal(UUID.randomUUID(), "tester", "hash", "Tester", true, false,
                Set.of("TESTER"), Set.of("test_case:read", "test_case:submit_review"));
    }

    private static UserPrincipal admin() {
        return new UserPrincipal(UUID.randomUUID(), "admin", "hash", "Admin", true, false, Set.of("ADMIN"),
                Set.of("test_case:review", "test_case:publish", "test_case:deprecate", "test_case:draft_create",
                        "test_case:draft_edit", "test_case:submit_review", "test_case:read"));
    }

    /** Returns a minimal non-null detail response so the MockMvc serialization does not NPE. */
    private static TestCaseDetailResponse detailResponse() {
        return new TestCaseDetailResponse(MASTER_ID, "CASE-001", null, null, null, true, null, null,
                List.of(), null, null, null, List.of(), null);
    }

    @SuppressWarnings("unused")
    private static AddContributorRequest sampleContributorRequest() {
        return new AddContributorRequest(CONTRIBUTOR_USER_ID);
    }

    @SuppressWarnings("unused")
    private static LifecycleActionRequest sampleActionRequest() {
        return new LifecycleActionRequest("comment");
    }
}
