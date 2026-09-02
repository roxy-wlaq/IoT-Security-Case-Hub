package com.company.casehub.testcase.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.casehub.auth.security.UserPrincipal;
import com.company.casehub.common.MethodSecurityTestConfig;
import com.company.casehub.testcase.dto.CreateDraftRequest;
import com.company.casehub.testcase.dto.TestCaseDetailResponse;
import com.company.casehub.testcase.dto.UpdateDraftRequest;
import com.company.casehub.testcase.service.TestCaseAccessPolicy;
import com.company.casehub.testcase.service.TestCaseDraftService;
import com.company.casehub.testcase.service.TestCaseQueryService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TestCaseController.class)
@Import(MethodSecurityTestConfig.class)
class TestCaseControllerRbacTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TestCaseDraftService draftService;
    @MockBean private TestCaseQueryService queryService;
    @MockBean(name = "testCaseAccessPolicy") private TestCaseAccessPolicy accessPolicy;

    private static final UUID MASTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void testerCannotCreateDraft() throws Exception {
        mockMvc.perform(post("/api/v1/test-cases").with(user("tester")).with(csrf())
                        .contentType("application/json")
                        .content("{\"caseCode\":\"BLE-001\",\"categoryId\":\"00000000-0000-0000-0000-000000000001\",\"caseName\":\"Pairing\",\"selectionMode\":\"SINGLE\"}"))
                .andExpect(status().isForbidden());
        verify(draftService, never()).createDraft(any(CreateDraftRequest.class), any());
    }

    @Test
    void testerContributorCanEditDraft() throws Exception {
        // HIGH-02: a TESTER contributor with no global test_case:draft_edit authority may edit the
        // assigned draft because the resource-level gate (canEditDraftById) resolves to true.
        when(accessPolicy.canEditDraftById(any(), any())).thenReturn(true);
        when(draftService.updateDraft(any(UUID.class), any(UpdateDraftRequest.class), any()))
                .thenReturn(detailResponse());

        mockMvc.perform(put("/api/v1/test-cases/{masterId}/draft", MASTER_ID)
                        .with(user(testerContributor())).with(csrf())
                        .contentType("application/json")
                        .content("{\"caseName\":\"Updated Name\",\"selectionMode\":\"SINGLE\",\"steps\":[{\"title\":\"S1\",\"content\":\"step content\"}]}"))
                .andExpect(status().isOk());
        verify(draftService).updateDraft(any(UUID.class), any(UpdateDraftRequest.class), any());
    }

    /** A TESTER contributor: no global draft_edit, but added to a Draft. */
    private static UserPrincipal testerContributor() {
        return new UserPrincipal(UUID.randomUUID(), "tester", "hash", "Tester", true, false,
                Set.of("TESTER"), Set.of("test_case:read", "test_case:submit_review"));
    }

    private static TestCaseDetailResponse detailResponse() {
        return new TestCaseDetailResponse(MASTER_ID, "CASE-001", null, null, null, true, null, null,
                List.of(), null, null, null, List.of(), null);
    }
}
